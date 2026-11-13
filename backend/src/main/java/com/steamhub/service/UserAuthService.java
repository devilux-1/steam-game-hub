package com.steamhub.service;

import com.steamhub.common.BizException;
import com.steamhub.common.ErrorCode;
import com.steamhub.config.SteamProperties;
import com.steamhub.entity.PO.AppUser;
import com.steamhub.mapper.AppUserMapper;
import com.steamhub.util.UserTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 用户会话服务：读写 httpOnly Cookie、解析当前登录用户、提供「必须登录」的校验入口。
 */
@Service
@RequiredArgsConstructor
public class UserAuthService {

    /** Steam 配置，提供 Cookie 名与有效期 */
    private final SteamProperties properties;

    /** 用户令牌工具 */
    private final UserTokenService userTokenService;

    /** 用户表访问接口 */
    private final AppUserMapper appUserMapper;

    /**
     * 下发登录 Cookie。
     *
     * @param response 当前响应，非空
     * @param token    用户令牌，非空
     */
    public void writeCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(properties.getCookieName(), token)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofSeconds(properties.getCookieMaxAgeSeconds()))
                // 前后端同站（同主机不同端口），Lax 即可满足 XHR 携带，且能防住跨站 POST
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 清除登录 Cookie。
     *
     * @param response 当前响应，非空
     */
    public void clearCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(properties.getCookieName(), "")
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 解析当前登录用户，未登录或令牌失效时返回 null。
     *
     * @param request 当前请求，非空
     * @return 用户实体；未登录时返回 null
     */
    public AppUser currentUser(HttpServletRequest request) {
        String token = readToken(request);
        if (token == null) {
            return null;
        }
        Long userId = userTokenService.parseUserId(token);
        return userId == null ? null : appUserMapper.selectById(userId);
    }

    /**
     * 取当前登录用户，未登录时抛出 401。
     *
     * @param request 当前请求，非空
     * @return 用户实体，非空
     * @throws BizException 未登录或登录态失效时（错误码 401）
     */
    public AppUser requireUser(HttpServletRequest request) {
        AppUser user = currentUser(request);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录 Steam");
        }
        return user;
    }

    /**
     * 从 Cookie 中读取用户令牌。
     *
     * @param request 当前请求，非空
     * @return 令牌字符串；没有 Cookie 时返回 null
     */
    private String readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (properties.getCookieName().equals(cookie.getName())) {
                String value = cookie.getValue();
                return value == null || value.isBlank() ? null : value;
            }
        }
        return null;
    }
}
