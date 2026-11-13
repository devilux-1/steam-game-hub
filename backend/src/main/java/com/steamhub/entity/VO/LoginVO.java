package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员登录成功后的返回对象，前端保存 token 并在后续请求的 Authorization 头中携带。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员登录结果")
public class LoginVO {

    /** 访问令牌，格式见 jjwt 的 HS256 JWS */
    @Schema(description = "访问令牌")
    private String token;

    /** 登录用户名，用于后台界面展示当前操作人 */
    @Schema(description = "登录用户名")
    private String username;
}
