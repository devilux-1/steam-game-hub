package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库内游戏详情视图对象：库记录 + 站内条目匹配结果 + Steam 商店信息。
 */
@Data
@Schema(description = "库内游戏详情")
public class LibraryDetailVO {

    /** Steam AppID */
    @Schema(description = "Steam AppID", example = "413150")
    private Integer appid;

    /** Steam 上的游戏名称 */
    @Schema(description = "游戏名称")
    private String title;

    /** 总时长（分钟） */
    @Schema(description = "总时长（分钟）", example = "3600")
    private Integer playtimeForever;

    /** 最近两周时长（分钟） */
    @Schema(description = "近两周时长（分钟）", example = "0")
    private Integer playtime2weeks;

    /** 最后游玩时间，从未游玩时为 null */
    @Schema(description = "最后游玩时间")
    private LocalDateTime lastPlayedAt;

    /** 匹配到的站内游戏 ID，未匹配时为 null */
    @Schema(description = "站内游戏 ID")
    private Long gameId;

    /** 站内游戏中文名，未匹配时为 null */
    @Schema(description = "站内中文名")
    private String gameNameCn;

    /** 封面地址：优先站内封面，其次 Steam 头图 */
    @Schema(description = "封面地址")
    private String coverUrl;

    /** Steam 商店信息，拉取失败时为 null */
    @Schema(description = "Steam 商店信息")
    private StoreGameVO store;
}
