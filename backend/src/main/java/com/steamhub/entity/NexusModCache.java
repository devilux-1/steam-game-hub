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
 * Nexus Mod 元数据缓存实体，由同步任务写入，页面不直连 Nexus。
 */
@Data
@Entity
@Table(name = "nexus_mod_cache")
public class NexusModCache {

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 所属游戏 ID，非空 */
    @Column(name = "game_id", nullable = false)
    private Long gameId;

    /** Nexus 侧 mod id，与 gameId 组成唯一键 */
    @Column(name = "mod_id", nullable = false)
    private Integer modId;

    /** Mod 名称，非空 */
    @Column(name = "name", length = 300, nullable = false)
    private String name;

    /** Mod 作者，可空 */
    @Column(name = "author", length = 100)
    private String author;

    /** 点赞数，用于热度排序，默认 0 */
    @Column(name = "endorsements", nullable = false)
    private Integer endorsements;

    /** 下载量，默认 0 */
    @Column(name = "downloads", nullable = false)
    private Long downloads;

    /** 简介，可空 */
    @Column(name = "summary", length = 1000)
    private String summary;

    /** Mod 页面地址，可空 */
    @Column(name = "mod_url", length = 500)
    private String modUrl;

    /** 本条缓存最近一次同步时间，非空 */
    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;
}
