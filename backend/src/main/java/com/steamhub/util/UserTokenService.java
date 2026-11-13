package com.steamhub.util;

import com.steamhub.config.SteamProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 用户令牌工具：签发与解析普通用户（Steam 登录）的 JWT。
 * 与管理员令牌共用签名密钥，靠 scope=user 区分，避免两种身份互相混用。
 */
@Component
public class UserTokenService {

    /** 令牌作用域值，解析时用于拒绝管理员令牌 */
    private static final String SCOPE_USER = "user";

    /** scope 声明名 */
    private static final String CLAIM_SCOPE = "scope";

    /** SteamID 声明名 */
    private static final String CLAIM_STEAM_ID = "steamId";

    /** HMAC-SHA 密钥 */
    private final SecretKey secretKey;

    /** 令牌有效期（毫秒），取自会话 Cookie 的有效期 */
    private final long expirationMs;

    /**
     * 构造用户令牌工具。
     *
     * @param secret  签名密钥字符串，非空且不少于 32 字节
     * @param steamProperties Steam 配置，提供会话有效期
     */
    public UserTokenService(@Value("${jwt.secret}") String secret, SteamProperties steamProperties) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = Math.max(steamProperties.getCookieMaxAgeSeconds(), 60L) * 1000L;
    }

    /**
     * 签发用户令牌。
     *
     * @param userId  用户主键，非空
     * @param steamId SteamID64，非空
     * @return 紧凑格式的 JWS 字符串
     */
    public String issue(Long userId, String steamId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_STEAM_ID, steamId)
                .claim(CLAIM_SCOPE, SCOPE_USER)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析用户令牌并取出用户主键。
     *
     * @param token 紧凑格式的 JWS 字符串，非空
     * @return 用户主键；令牌非法、过期或不是用户令牌时返回 null
     */
    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!SCOPE_USER.equals(claims.get(CLAIM_SCOPE, String.class))) {
                return null;
            }
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            // 过期、篡改、格式错误一律按未登录处理，由调用方走游客分支
            return null;
        }
    }
}
