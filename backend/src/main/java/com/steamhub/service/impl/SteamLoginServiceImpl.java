package com.steamhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.steamhub.client.SteamClient;
import com.steamhub.config.SteamProperties;
import com.steamhub.entity.DTO.SteamPlayerDTO;
import com.steamhub.entity.PO.AppUser;
import com.steamhub.mapper.AppUserMapper;
import com.steamhub.service.SteamLoginService;
import com.steamhub.service.UserAuthService;
import com.steamhub.util.UserTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Steam OpenID 登录服务：生成带 state 的登录地址、校验回调断言、落库用户并下发会话 Cookie。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SteamLoginServiceImpl implements SteamLoginService {

    /** 回调参数中的 claimed_id 键名 */
    private static final String PARAM_CLAIMED_ID = "openid.claimed_id";

    /** 回调参数中的 mode 键名 */
    private static final String PARAM_MODE = "openid.mode";

    /** 授权成功时 Steam 回传的 mode 值 */
    private static final String MODE_ID_RES = "id_res";

    /** 防 CSRF 的临时 Cookie 名 */
    private static final String STATE_COOKIE = "sgh_oid_state";

    /** state 有效期，只在跳转与回调之间短暂有效 */
    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    /** Steam 配置 */
    private final SteamProperties properties;

    /** Steam 接口客户端 */
    private final SteamClient steamClient;

    /** 用户表访问接口 */
    private final AppUserMapper appUserMapper;

    /** 用户令牌工具 */
    private final UserTokenService userTokenService;

    /** 用户会话服务 */
    private final UserAuthService userAuthService;

    /**
     * 生成跳转 Steam 的登录地址，同时把随机 state 写入临时 Cookie。
     *
     * @param response 当前响应，用于写 state Cookie，非空
     * @return Steam 登录地址
     */
    @Override
    public String createLoginUrl(HttpServletResponse response) {
        String state = UUID.randomUUID().toString().replace("-", "");
        ResponseCookie cookie = ResponseCookie.from(STATE_COOKIE, state)
                .httpOnly(true)
                .path("/")
                .maxAge(STATE_TTL)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return steamClient.buildLoginUrl(state);
    }

    /**
     * 处理 OpenID 回调：校验 state 与断言，落库用户并下发会话。
     *
     * @param params   回调参数（含 state 与 openid.*），非空
     * @param request  当前请求，用于读取 state Cookie，非空
     * @param response 当前响应，用于写会话 Cookie，非空
     * @return 前端跳转地址；失败时带 login=fail 参数，由前端提示
     */
    @Override
    public String handleCallback(Map<String, String> params, HttpServletRequest request, HttpServletResponse response) {
        if (!MODE_ID_RES.equals(params.get(PARAM_MODE))) {
            log.warn("OpenID 回调 mode 非 id_res：{}", params.get(PARAM_MODE));
            return failUrl();
        }
        String state = params.get("state");
        String expectState = readCookie(request, STATE_COOKIE);
        if (state == null || expectState == null || !expectState.equals(state)) {
            log.warn("OpenID 回调 state 校验失败");
            return failUrl();
        }
        if (!steamClient.verifyAssertion(params)) {
            log.warn("OpenID 断言校验未通过");
            return failUrl();
        }
        String steamId = steamClient.extractSteamId(params.get(PARAM_CLAIMED_ID));
        if (steamId == null) {
            log.warn("OpenID 回调缺少 SteamID64：{}", params.get(PARAM_CLAIMED_ID));
            return failUrl();
        }

        AppUser user = upsertUser(steamId);
        userAuthService.writeCookie(response, userTokenService.issue(user.getId(), steamId));
        clearStateCookie(response);
        return properties.getFrontendUrl() + "/me/library?login=ok";
    }

    /**
     * 按 SteamID64 落库用户：新用户插入，老用户更新昵称头像与登录时间。
     *
     * @param steamId SteamID64，非空
     * @return 落库后的用户（含主键）
     */
    private AppUser upsertUser(String steamId) {
        // 拉不到资料时不影响登录，昵称头像留空，页面上只显示 SteamID
        SteamPlayerDTO player = steamClient.fetchPlayerSummary(steamId);
        LocalDateTime now = LocalDateTime.now();
        AppUser existing = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getSteamId, steamId));
        if (existing == null) {
            AppUser user = new AppUser();
            user.setSteamId(steamId);
            user.setPersonaName(player == null ? null : player.getPersonaName());
            user.setAvatarUrl(player == null ? null : player.getAvatarUrl());
            user.setLastLoginAt(now);
            appUserMapper.insert(user);
            log.info("新用户首次登录 steamId={} userId={}", steamId, user.getId());
            return user;
        }
        existing.setPersonaName(player == null ? existing.getPersonaName() : player.getPersonaName());
        existing.setAvatarUrl(player == null ? existing.getAvatarUrl() : player.getAvatarUrl());
        existing.setLastLoginAt(now);
        appUserMapper.updateById(existing);
        return existing;
    }

    /**
     * 读取指定名称的 Cookie。
     *
     * @param request 当前请求，非空
     * @param name    Cookie 名，非空
     * @return Cookie 值；不存在时返回 null
     */
    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 清除一次性 state Cookie。
     *
     * @param response 当前响应，非空
     */
    private void clearStateCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(STATE_COOKIE, "")
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 登录失败时的前端跳转地址。
     *
     * @return 带 login=fail 的地址
     */
    private String failUrl() {
        return properties.getFrontendUrl() + "/me/library?login=fail";
    }
}
