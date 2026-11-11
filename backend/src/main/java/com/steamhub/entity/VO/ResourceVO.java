package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源视图对象，用于游戏条目页三类资源卡片的展示。
 * 不暴露上下架状态、排序权重等后台字段。
 */
@Data
@Schema(description = "资源展示信息")
public class ResourceVO {

    /** 资源 ID */
    @Schema(description = "资源 ID", example = "1")
    private Long id;

    /** 资源类型：NEXUS_MOD / HANZI_PATCH / TRAINER */
    @Schema(description = "资源类型：NEXUS_MOD / HANZI_PATCH / TRAINER")
    private String type;

    /** 资源名称 */
    @Schema(description = "资源名称")
    private String title;

    /** 版本号，可空 */
    @Schema(description = "版本号")
    private String version;

    /** 适配的游戏版本，可空 */
    @Schema(description = "适配的游戏版本")
    private String compatibleVersion;

    /** 作者或汉化组，可空 */
    @Schema(description = "作者或汉化组")
    private String author;

    /** 来源站标识，如 Nexus、3DM */
    @Schema(description = "来源站标识")
    private String sourceSite;

    /** 跳转地址，点击后新窗口打开来源站点 */
    @Schema(description = "跳转地址")
    private String sourceUrl;

    /** 一句话说明 */
    @Schema(description = "一句话说明")
    private String description;

    /** 安装要点，可空 */
    @Schema(description = "安装要点")
    private String installNote;

    /** 点击计数 */
    @Schema(description = "点击计数", example = "0")
    private Long clickCount;

    /** 最近更新时间，用于展示「最近更新」 */
    @Schema(description = "最近更新时间")
    private LocalDateTime updatedAt;
}
