package com.steamhub.controller;

import com.steamhub.common.Result;
import com.steamhub.entity.DTO.AdminLoginDTO;
import com.steamhub.entity.VO.LoginVO;
import com.steamhub.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台登录接口，完整路径 /api/admin/auth/login。
 * 该路径已在拦截器中被排除，无需携带令牌。
 */
@RestController
@RequestMapping("/admin/auth")
@Tag(name = "后台登录")
@RequiredArgsConstructor
public class AdminAuthController {

    /** 管理员登录业务层 */
    private final AdminAuthService adminAuthService;

    /**
     * 管理员登录，成功后返回 JWT。
     *
     * @param dto 登录入参，非空；用户名与密码均有非空校验
     * @return 登录结果，含 token 与 username；失败时返回 400
     */
    @PostMapping("/login")
    @Operation(summary = "管理员登录")
    public Result<LoginVO> login(@RequestBody @Valid AdminLoginDTO dto) {
        return Result.success(adminAuthService.login(dto));
    }
}
