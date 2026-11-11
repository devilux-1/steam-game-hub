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
 * 资源表实体，Nexus Mod、汉化补丁、风灵月影修改器统一存储，用 type 区分。
 */
@Data
@Entity
@Table(name = "resource")
public class Resource {

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 所属游戏 ID，非空 */
    @Column(name = "game_id", nullable = false)
    private Long gameId;

    /** 资源类型：NEXUS_MOD / HANZI_PATCH / TRAINER，非空 */
    @Column(name = "type", length = 20, nullable = false)
    private String type;

    /** 资源名称，非空 */
    @Column(name = "title", length = 300, nullable = false)
    private String title;

    /** 版本号，可空 */
    @Column(name = "version", length = 50)
    private String version;

    /** 适配的游戏版本，可空 */
    @Column(name = "compatible_version", length = 100)
    private String compatibleVersion;

    /** 作者或汉化组，可空 */
    @Column(name = "author", length = 100)
    private String author;

    /** 来源站标识，可空 */
    @Column(name = "source_site", length = 50)
    private String sourceSite;

    /** 跳转地址，非空，本站不托管文件 */
    @Column(name = "source_url", length = 1000, nullable = false)
    private String sourceUrl;

    /** 一句话说明，可空 */
    @Column(name = "description", length = 500)
    private String description;

    /** 安装要点，可空 */
    @Column(name = "install_note", length = 1000)
    private String installNote;

    /** 点击计数，默认 0 */
    @Column(name = "click_count", nullable = false)
    private Long clickCount;

    /** 排序权重，越大越靠前，默认 0 */
    @Column(name = "sort_weight", nullable = false)
    private Integer sortWeight;

    /** 上下架状态：1 上架 / 0 下架，默认 1 */
    @Column(name = "status", nullable = false)
    private Integer status;

    /** 创建时间，由数据库默认值填充 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间，由数据库 ON UPDATE CURRENT_TIMESTAMP 维护 */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
