package com.steamhub.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * Steam OpenID 登录业务接口：生成带 state 的登录地址、校验回调断言、落库用户并下发会话 Cookie。
 */
public interface SteamLoginService {

    /**
     * 生成跳转 Steam 的登录地址，同时把随机 state 写入临时 Cookie。
     *
     * @param response 当前响应，用于写 state Cookie，非空
     * @return Steam 登录地址
     */
    String createLoginUrl(HttpServletResponse response);

    /**
     * 处理 OpenID 回调：校验 state 与断言，落库用户并下发会话。
     *
     * @param params   回调参数（含 state 与 openid.*），非空
     * @param request  当前请求，用于读取 state Cookie，非空
     * @param response 当前响应，用于写会话 Cookie，非空
     * @return 前端跳转地址；失败时带 login=fail 参数，由前端提示
     */
    String handleCallback(Map<String, String> params, HttpServletRequest request, HttpServletResponse response);
}
