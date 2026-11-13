package com.steamhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置：阶段 0 放行全部接口、关闭 CSRF、使用无状态会话，阶段 3 接入 Steam 登录后再按路径收紧。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 构建安全过滤链。
     *
     * @param http 过滤链构造器，由框架注入
     * @return 安全过滤链
     * @throws Exception 构造过滤链过程中框架抛出的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 登录态后续走 JWT Cookie，服务端不保存会话，因此无需 CSRF token
                .csrf(AbstractHttpConfigurer::disable)
                // 启用全局跨域规则（由 CorsConfig 中的 CorsConfigurationSource 提供）
                .cors(Customizer.withDefaults())
                // 前后端分离，全部接口按无状态处理
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 不使用表单登录与 HTTP Basic，登录改由 Steam OpenID 流程完成
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 阶段 0 仅打通联调通路，所有请求放行
                .authorizeHttpRequests(registry -> registry.anyRequest().permitAll());
        return http.build();
    }

    /**
     * 密码编码器：管理员密码使用 BCrypt 存储与校验。
     *
     * @return BCrypt 编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
