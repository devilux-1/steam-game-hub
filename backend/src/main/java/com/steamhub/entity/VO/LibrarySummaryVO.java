package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的游戏库统计视图对象，展示在列表顶部。
 */
@Data
@Schema(description = "我的游戏库统计")
public class LibrarySummaryVO {

    /** 库中游戏总数 */
    @Schema(description = "游戏总数", example = "128")
    private Integer totalGames;

    /** 总游玩时长（分钟） */
    @Schema(description = "总时长（分钟）", example = "12000")
    private Integer totalMinutes;

    /** 最近两周游玩时长（分钟） */
    @Schema(description = "近两周时长（分钟）", example = "320")
    private Integer twoWeeksMinutes;

    /** 已匹配到站内条目的游戏数 */
    @Schema(description = "已匹配站内条目数", example = "6")
    private Integer matchedGames;

    /** 最近一次同步时间，未同步时为 null */
    @Schema(description = "最近同步时间")
    private LocalDateTime lastSyncedAt;
}
