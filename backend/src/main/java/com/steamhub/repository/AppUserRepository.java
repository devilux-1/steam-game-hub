package com.steamhub.repository;

import com.steamhub.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户表数据访问接口。
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
}
