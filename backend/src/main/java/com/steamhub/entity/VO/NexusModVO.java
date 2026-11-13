package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Nexus 精选 Mod 视图对象，供游戏条目页展示同步下来的 Mod 列表。
 */
@Data
@Schema(description = "Nexus 精选 Mod")
public class NexusModVO {

    /** Nexus 侧 mod id */
    @Schema(description = "Nexus mod id", example = "510")
    private Integer modId;

    /** Mod 名称 */
    @Schema(description = "Mod 名称")
    private String name;

    /** 中文标题，为空时前端回退展示 name */
    @Schema(description = "中文标题，为空时前端回退显示 name")
    private String nameCn;

    /** 作者 */
    @Schema(description = "作者")
    private String author;

    /** 点赞数 */
    @Schema(description = "点赞数", example = "12000")
    private Integer endorsements;

    /** 下载量 */
    @Schema(description = "下载量", example = "500000")
    private Long downloads;

    /** 简介 */
    @Schema(description = "简介")
    private String summary;

    /** 中文简介，为空时前端回退展示 summary */
    @Schema(description = "中文简介，为空时前端回退显示 summary")
    private String summaryCn;

    /** Mod 详情页地址，前端新窗口打开 */
    @Schema(description = "Mod 详情页地址")
    private String modUrl;

    /** 本条数据的同步时间 */
    @Schema(description = "同步时间")
    private LocalDateTime syncedAt;
}
