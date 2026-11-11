package com.steamhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员实体，账号密码与普通用户完全隔离。
 */
@Data
@Entity
@Table(name = "admin_user")
public class AdminUser {

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 登录用户名，唯一且非空 */
    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    /** BCrypt 加密后的密码，非空 */
    @Column(name = "password_hash", length = 100, nullable = false)
    private String passwordHash;

    /** 最近登录时间，可空 */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
