package com.steamhub.config;

import com.steamhub.entity.PO.AdminUser;
import com.steamhub.mapper.AdminUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化管理员账号：表为空时插入一个默认账号，避免首次部署无法进入后台。
 * 生产环境务必通过配置覆盖默认密码，或登录后立即修改。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    /** 管理员数据访问接口 */
    private final AdminUserMapper adminUserMapper;

    /** 密码编码器 */
    private final PasswordEncoder passwordEncoder;

    /** 默认管理员用户名，可由配置 app.admin.username 覆盖 */
    @Value("${app.admin.username:admin}")
    private String defaultUsername;

    /** 默认管理员明文密码，可由配置 app.admin.password 覆盖 */
    @Value("${app.admin.password:admin123}")
    private String defaultPassword;

    /**
     * 应用启动后检查管理员表，为空则创建默认账号。
     *
     * @param args 启动参数，未使用
     */
    @Override
    public void run(String... args) {
        Long count = adminUserMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        AdminUser admin = new AdminUser();
        admin.setUsername(defaultUsername);
        admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
        adminUserMapper.insert(admin);
        log.warn("管理员表为空，已创建默认账号 [{}]，请登录后立即修改密码", defaultUsername);
    }
}
