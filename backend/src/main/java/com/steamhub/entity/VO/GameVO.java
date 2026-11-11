package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 游戏视图对象，用于首页热门列表与游戏条目页的基础信息展示。
 * 只暴露展示需要的字段，不包含创建时间、更新时间等内部字段。
 */
@Data
@Schema(description = "游戏展示信息")
public class GameVO {

    /** 游戏 ID */
    @Schema(description = "游戏 ID", example = "1")
    private Long id;

    /** 中文名，可空 */
    @Schema(description = "中文名")
    private String nameCn;

    /** 英文名，非空 */
    @Schema(description = "英文名")
    private String nameEn;

    /** Steam AppID，可空，前端用于跳转 Steam 商店页 */
    @Schema(description = "Steam AppID")
    private Integer steamAppid;

    /** 封面图地址 */
    @Schema(description = "封面图地址")
    private String coverUrl;

    /** 简介 */
    @Schema(description = "简介")
    private String summary;

    /** 发售日期，可空 */
    @Schema(description = "发售日期")
    private LocalDate releaseDate;

    /** 该游戏已收录的资源数，用于列表按热度排序与卡片展示 */
    @Schema(description = "已收录资源数", example = "0")
    private Long resourceCount;
}
