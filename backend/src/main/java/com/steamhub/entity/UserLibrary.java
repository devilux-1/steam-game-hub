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
 * 用户游戏库实体，存放 Steam 同步返回的单条游戏记录。
 */
@Data
@Entity
@Table(name = "user_library")
public class UserLibrary {

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 所属用户 ID，非空 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Steam AppID，与 userId 组成唯一键 */
    @Column(name = "appid", nullable = false)
    private Integer appid;

    /** Steam 上的游戏名称，非空 */
    @Column(name = "title", length = 300, nullable = false)
    private String title;

    /** 总时长（分钟），默认 0 */
    @Column(name = "playtime_forever", nullable = false)
    private Integer playtimeForever;

    /** 最近两周时长（分钟），默认 0；列名含下划线数字，必须显式指定 */
    @Column(name = "playtime_2weeks", nullable = false)
    private Integer playtime2weeks;

    /** 最后游玩时间，可空 */
    @Column(name = "last_played_at")
    private LocalDateTime lastPlayedAt;

    /** 匹配到的站内游戏 ID，可空，未匹配时为 null */
    @Column(name = "game_id")
    private Long gameId;

    /** 本条记录的同步时间，非空 */
    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;
}
