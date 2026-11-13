package com.steamhub.config;

import com.steamhub.common.BizException;
import com.steamhub.common.ErrorCode;
import com.steamhub.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 后台接口鉴权拦截器：校验 Authorization 头中的 Bearer 令牌，通过后把管理员用户名放入请求属性。
 * 仅注册在 /admin/** 路径上，登录接口单独排除。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    /** 请求属性名，控制器可通过该键取到当前管理员用户名 */
    public static final String ATTR_ADMIN_USERNAME = "adminUsername";

    /** 令牌请求头名 */
    private static final String AUTH_HEADER = "Authorization";

    /** 令牌前缀，固定为 Bearer 加一个空格 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** JWT 工具 */
    private final JwtUtil jwtUtil;

    /**
     * 请求进入控制器前校验令牌。
     *
     * @param request  当前请求，非空
     * @param response 当前响应，非空
     * @param handler  即将执行的处理器
     * @return 校验通过返回 true；校验失败抛出业务异常由全局异常处理器转成 401
     * @throws BizException 未携带令牌或令牌无效时（错误码 401）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 静态资源等非控制器请求不做鉴权
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录后台");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录后台");
        }
        request.setAttribute(ATTR_ADMIN_USERNAME, jwtUtil.parseUsername(token));
        return true;
    }
}
