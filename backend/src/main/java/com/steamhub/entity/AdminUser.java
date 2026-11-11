package com.steamhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员实体，账号密码与普通用户完全隔离，对应表 admin_user。
 */
@Data
@TableName("admin_user")
public class AdminUser {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名，唯一且非空 */
    private String username;

    /** BCrypt 加密后的密码，非空 */
    private String passwordHash;

    /** 最近登录时间，可空 */
    private LocalDateTime lastLoginAt;
}
