package com.steamhub.service;

import com.steamhub.common.BizException;
import com.steamhub.entity.PO.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 用户会话业务接口：读写 httpOnly Cookie、解析当前登录用户、提供「必须登录」的校验入口。
 */
public interface UserAuthService {

    /**
     * 下发登录 Cookie。
     *
     * @param response 当前响应，非空
     * @param token    用户令牌，非空
     */
    void writeCookie(HttpServletResponse response, String token);

    /**
     * 清除登录 Cookie。
     *
     * @param response 当前响应，非空
     */
    void clearCookie(HttpServletResponse response);

    /**
     * 解析当前登录用户，未登录或令牌失效时返回 null。
     *
     * @param request 当前请求，非空
     * @return 用户实体；未登录时返回 null
     */
    AppUser currentUser(HttpServletRequest request);

    /**
     * 取当前登录用户，未登录时抛出 401。
     *
     * @param request 当前请求，非空
     * @return 用户实体，非空
     * @throws BizException 未登录或登录态失效时（错误码 401）
     */
    AppUser requireUser(HttpServletRequest request);
}
