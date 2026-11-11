package com.steamhub.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，只有 Steam 账号，无密码，对应表 app_user。
 */
@Data
@TableName("app_user")
public class AppUser {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** SteamID64，唯一且非空 */
    private String steamId;

    /** Steam 昵称，可空 */
    private String personaName;

    /** 头像地址，可空 */
    private String avatarUrl;

    /** 首次登录时间，插入时为空由数据库默认值填充 */
    private LocalDateTime createdAt;

    /** 最近登录时间，可空 */
    private LocalDateTime lastLoginAt;
}
