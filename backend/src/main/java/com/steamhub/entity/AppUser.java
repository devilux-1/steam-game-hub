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
 * 用户实体，只有 Steam 账号，无密码。
 */
@Data
@Entity
@Table(name = "app_user")
public class AppUser {

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** SteamID64，唯一且非空 */
    @Column(name = "steam_id", length = 20, nullable = false, unique = true)
    private String steamId;

    /** Steam 昵称，可空 */
    @Column(name = "persona_name", length = 100)
    private String personaName;

    /** 头像地址，可空 */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** 首次登录时间，由数据库默认值填充 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 最近登录时间，可空 */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
