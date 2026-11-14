package com.steamhub.service;

import com.steamhub.common.BizException;
import com.steamhub.entity.DTO.AdminLoginDTO;
import com.steamhub.entity.VO.LoginVO;

/**
 * 管理员登录业务接口。
 */
public interface AdminAuthService {

    /**
     * 校验账号密码并签发令牌。
     *
     * @param dto 登录入参，非空；用户名与密码均已通过非空校验
     * @return 登录结果，含令牌与用户名
     * @throws BizException 账号不存在或密码错误时（错误码 1000）
     */
    LoginVO login(AdminLoginDTO dto);
}
