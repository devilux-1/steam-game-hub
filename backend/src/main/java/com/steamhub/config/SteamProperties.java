package com.steamhub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Steam 接口配置：OpenID 登录、Web API（玩家信息与游戏库），以及可选的本地 HTTP 代理。
 */
@Data
@Component
@ConfigurationProperties(prefix = "steam")
public class SteamProperties {

    /** Steam Web API Key，用于读取玩家信息与拥有游戏；为空时同步接口直接提示未配置 */
    private String apiKey = "";

    /** OpenID 登录地址；国内直连会被拦截，需要经代理访问 */
    private String openidUrl = "https://steamcommunity.com/openid/login";

    /** Steam Web API 基础地址 */
    private String apiBaseUrl = "https://api.steampowered.com";

    /** Steam 商店详情接口地址，用于给未收录的库内游戏补简介与截图 */
    private String storeApiUrl = "https://store.steampowered.com/api/appdetails";

    /** 登录回调地址，必须是浏览器可访问的地址，Steam 校验通过后回跳到这里 */
    private String callbackUrl = "http://localhost:8080/api/auth/steam/callback";

    /** 登录完成后的前端跳转地址 */
    private String frontendUrl = "http://localhost:5173";

    /** 用户会话 Cookie 名 */
    private String cookieName = "sgh_user";

    /** 会话有效期（秒），同时作为用户 JWT 的有效期 */
    private long cookieMaxAgeSeconds = 604800L;

    /** 同一用户两次同步的最小间隔（秒） */
    private long syncCooldownSeconds = 60L;

    /** HTTP 代理主机，为空表示直连；本机访问 OpenID 需要填 127.0.0.1 */
    private String proxyHost = "";

    /** HTTP 代理端口，proxyHost 非空且端口大于 0 时生效 */
    private int proxyPort = 0;

    /** 单次请求超时秒数 */
    private int timeoutSeconds = 20;
}
