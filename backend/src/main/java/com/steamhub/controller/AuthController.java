package com.steamhub.controller;

import com.steamhub.common.Result;
import com.steamhub.entity.PO.AppUser;
import com.steamhub.entity.VO.MeVO;
import com.steamhub.service.SteamLoginService;
import com.steamhub.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/**
 * 用户登录接口：Steam OpenID 登录、回调、退出与当前用户查询。
 * 路径按设计文档 9.2 平铺在 /api 下，不使用类级前缀。
 */
@Slf4j
@RestController
@Tag(name = "用户登录")
@RequiredArgsConstructor
public class AuthController {

    /** 登录业务层 */
    private final SteamLoginService steamLoginService;

    /** 用户会话服务 */
    private final UserAuthService userAuthService;

    /**
     * 跳转 Steam 登录页。
     *
     * @param response 当前响应，用于写入防 CSRF 的 state Cookie
     * @throws IOException 写响应失败时
     */
    @GetMapping("/auth/steam/login")
    @Operation(summary = "跳转 Steam 登录")
    public void login(HttpServletResponse response) throws IOException {
        response.sendRedirect(steamLoginService.createLoginUrl(response));
    }

    /**
     * Steam 登录回调：校验断言、落库用户、下发会话 Cookie，然后跳回前端。
     *
     * @param params   回调参数，含 state 与全部 openid.* 字段
     * @param request  当前请求，用于读取 state Cookie
     * @param response 当前响应，用于写入会话 Cookie
     * @throws IOException 写响应失败时
     */
    @GetMapping("/auth/steam/callback")
    @Operation(summary = "Steam 登录回调")
    public void callback(@RequestParam Map<String, String> params,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.sendRedirect(steamLoginService.handleCallback(params, request, response));
    }

    /**
     * 退出登录：清除会话 Cookie。
     *
     * @param response 当前响应，用于清除 Cookie
     * @return 成功结果
     */
    @PostMapping("/auth/logout")
    @Operation(summary = "退出登录")
    public Result<Void> logout(HttpServletResponse response) {
        userAuthService.clearCookie(response);
        return Result.success();
    }

    /**
     * 查询当前登录用户。
     *
     * @param request 当前请求，用于解析会话 Cookie
     * @return 已登录时返回用户信息，未登录时 data 为 null
     */
    @GetMapping("/me")
    @Operation(summary = "当前登录用户")
    public Result<MeVO> me(HttpServletRequest request) {
        AppUser user = userAuthService.currentUser(request);
        if (user == null) {
            return Result.success(null);
        }
        MeVO vo = new MeVO();
        vo.setUserId(user.getId());
        vo.setSteamId(user.getSteamId());
        vo.setPersonaName(user.getPersonaName());
        vo.setAvatarUrl(user.getAvatarUrl());
        return Result.success(vo);
    }
}
