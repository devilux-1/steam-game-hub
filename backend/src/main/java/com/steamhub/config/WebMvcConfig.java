package com.steamhub.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置：注册后台鉴权拦截器。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    /** 后台接口路径前缀，相对于 server.servlet.context-path=/api */
    private static final String ADMIN_PATH_PATTERN = "/admin/**";

    /** 登录接口无需鉴权，否则登录请求本身会被拦截 */
    private static final String LOGIN_PATH = "/admin/auth/login";

    /** 后台鉴权拦截器 */
    private final JwtInterceptor jwtInterceptor;

    /**
     * 注册拦截器：拦截全部后台接口，放行登录接口。
     *
     * @param registry 拦截器注册表，由框架注入
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns(ADMIN_PATH_PATTERN)
                .excludePathPatterns(LOGIN_PATH);
    }
}
