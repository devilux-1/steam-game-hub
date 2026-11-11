package com.steamhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 跨域配置，允许本地前端开发服务器 http://localhost:5173 访问后端接口。
 */
@Configuration
public class CorsConfig {

    /** 允许跨域的前端来源（Vite 默认端口） */
    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    /** 全部路径都应用同一套跨域规则 */
    private static final String ALL_PATH_PATTERN = "/**";

    /** 预检结果缓存秒数，减少重复 OPTIONS 请求 */
    private static final long PREFLIGHT_MAX_AGE_SECONDS = 3600L;

    /**
     * 注册全局跨域规则，Spring Security 的 cors() 与 Spring MVC 都会使用该配置源。
     *
     * @return 匹配全部路径的跨域配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(FRONTEND_ORIGIN));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // 登录态后续通过 httpOnly Cookie 传递，必须允许携带凭证
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(PREFLIGHT_MAX_AGE_SECONDS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ALL_PATH_PATTERN, configuration);
        return source;
    }
}
