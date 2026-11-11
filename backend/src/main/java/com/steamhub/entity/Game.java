package com.steamhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 游戏主表实体，一个游戏聚合 Nexus Mod、汉化补丁、修改器三类资源。
 */
@Data
@Entity
@Table(name = "game")
public class Game {

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 中文名，可空 */
    @Column(name = "name_cn", length = 200)
    private String nameCn;

    /** 英文名，非空 */
    @Column(name = "name_en", length = 200, nullable = false)
    private String nameEn;

    /** Steam AppID，可空，用于游戏库匹配与商店信息拉取 */
    @Column(name = "steam_appid")
    private Integer steamAppid;

    /** Nexus 的 URL 标识，可空，如 eldenring */
    @Column(name = "nexus_domain", length = 100)
    private String nexusDomain;

    /** 封面图地址 */
    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    /** 简介 */
    @Column(name = "summary", length = 1000)
    private String summary;

    /** 发售日期，可空 */
    @Column(name = "release_date")
    private LocalDate releaseDate;

    /** 创建时间，由数据库默认值填充，插入后不由应用回写 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间，由数据库 ON UPDATE CURRENT_TIMESTAMP 维护 */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
