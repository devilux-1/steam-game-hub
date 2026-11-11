package com.steamhub.repository;

import com.steamhub.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 管理员表数据访问接口。
 */
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
}
