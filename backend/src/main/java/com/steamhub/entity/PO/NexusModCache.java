package com.steamhub.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Nexus Mod 元数据缓存实体，由同步任务写入，页面不直连 Nexus，对应表 nexus_mod_cache。
 */
@Data
@TableName("nexus_mod_cache")
public class NexusModCache {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属游戏 ID，非空 */
    private Long gameId;

    /** Nexus 侧 mod id，与 gameId 组成唯一键 */
    private Integer modId;

    /** Mod 名称，非空 */
    private String name;

    /** Mod 作者，可空 */
    private String author;

    /** 点赞数，用于热度排序，默认 0 */
    private Integer endorsements;

    /** 下载量，默认 0 */
    private Long downloads;

    /** 简介，可空 */
    private String summary;

    /** Mod 页面地址，可空 */
    private String modUrl;

    /** 本条缓存最近一次同步时间，非空 */
    private LocalDateTime syncedAt;
}
