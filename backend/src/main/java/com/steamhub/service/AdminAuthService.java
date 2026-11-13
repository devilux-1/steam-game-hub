package com.steamhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.steamhub.common.BizException;
import com.steamhub.common.ErrorCode;
import com.steamhub.entity.DTO.AdminLoginDTO;
import com.steamhub.entity.PO.AdminUser;
import com.steamhub.entity.VO.LoginVO;
import com.steamhub.mapper.AdminUserMapper;
import com.steamhub.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理员登录业务层。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    /** 登录失败统一提示，不区分「用户不存在」与「密码错误」，避免泄露账号是否存在 */
    private static final String LOGIN_FAILED_MESSAGE = "用户名或密码错误";

    /** 管理员数据访问接口 */
    private final AdminUserMapper adminUserMapper;

    /** 密码编码器，用于校验 BCrypt 哈希 */
    private final PasswordEncoder passwordEncoder;

    /** JWT 工具 */
    private final JwtUtil jwtUtil;

    /**
     * 校验账号密码并签发令牌。
     *
     * @param dto 登录入参，非空；用户名与密码均已通过非空校验
     * @return 登录结果，含令牌与用户名
     * @throws BizException 账号不存在或密码错误时（错误码 1000）
     */
    public LoginVO login(AdminLoginDTO dto) {
        String username = dto.getUsername().trim();
        AdminUser user = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            log.warn("管理员登录失败: username={}", username);
            throw new BizException(ErrorCode.BIZ_FAIL, LOGIN_FAILED_MESSAGE);
        }

        // 登录成功才更新最近登录时间，失败分支不写库
        AdminUser update = new AdminUser();
        update.setId(user.getId());
        update.setLastLoginAt(LocalDateTime.now());
        adminUserMapper.updateById(update);

        log.info("管理员登录成功: username={}", username);
        return new LoginVO(jwtUtil.generateToken(username), username);
    }
}
