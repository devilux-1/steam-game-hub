package com.steamhub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地接口文档配置，文档入口为 /api/swagger-ui.html。
 */
@Configuration
public class OpenApiConfig {

    /** 文档标题 */
    private static final String API_TITLE = "Steam 单机游戏资源站 API";

    /** 文档版本，与工程版本号保持一致 */
    private static final String API_VERSION = "0.0.1";

    /** JWT 鉴权方案名称，后续 controller 用 @SecurityRequirement 引用 */
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * 构建 OpenAPI 文档描述。
     *
     * @return OpenAPI 根对象，含基础信息与 JWT 鉴权方案
     */
    @Bean
    public OpenAPI steamHubOpenApi() {
        Info info = new Info()
                .title(API_TITLE)
                .version(API_VERSION)
                .description("统一响应体为 {code, message, data}，code=0 表示成功。");
        // 阶段 3 登录后下发的 JWT，阶段 0 尚未启用，先登记方案便于后续接口标注
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
        return new OpenAPI()
                .info(info)
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme));
    }
}
