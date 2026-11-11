package com.steamhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户游戏库实体，存放 Steam 同步返回的单条游戏记录，对应表 user_library。
 */
@Data
@TableName("user_library")
public class UserLibrary {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID，非空 */
    private Long userId;

    /** Steam AppID，与 userId 组成唯一键 */
    private Integer appid;

    /** Steam 上的游戏名称，非空 */
    private String title;

    /** 总时长（分钟），默认 0 */
    private Integer playtimeForever;

    /** 最近两周时长（分钟），默认 0；列名含数字，驼峰转换无法命中，必须显式指定 */
    @TableField("playtime_2weeks")
    private Integer playtime2weeks;

    /** 最后游玩时间，可空 */
    private LocalDateTime lastPlayedAt;

    /** 匹配到的站内游戏 ID，可空，未匹配时为 null */
    private Long gameId;

    /** 本条记录的同步时间，非空 */
    private LocalDateTime syncedAt;
}
