package com.steamhub.entity.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员登录入参。
 */
@Data
@Schema(description = "管理员登录入参")
public class AdminLoginDTO {

    /** 登录用户名，非空 */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过 50 字符")
    @Schema(description = "用户名")
    private String username;

    /** 登录密码，非空；长度上限与 BCrypt 支持范围一致 */
    @NotBlank(message = "密码不能为空")
    @Size(max = 72, message = "密码不能超过 72 字符")
    @Schema(description = "密码")
    private String password;
}
