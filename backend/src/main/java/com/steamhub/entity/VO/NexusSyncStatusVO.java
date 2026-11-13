package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Nexus 同步状态视图对象，供后台展示「是否在跑、上次结果如何」。
 */
@Data
@Schema(description = "Nexus 同步状态")
public class NexusSyncStatusVO {

    /** 是否有同步任务正在执行 */
    @Schema(description = "是否有同步任务正在执行")
    private boolean running;

    /** 是否处于演示数据模式（未配置 API Key 或显式开启 mock） */
    @Schema(description = "是否处于演示数据模式")
    private boolean mockMode;

    /** 本次/上次同步开始时间，从未同步过为 null */
    @Schema(description = "同步开始时间")
    private LocalDateTime lastStartAt;

    /** 本次/上次同步结束时间，仍在执行为 null */
    @Schema(description = "同步结束时间")
    private LocalDateTime lastEndAt;

    /** 参与同步的游戏总数（仅统计配置了 nexus_domain 的游戏） */
    @Schema(description = "参与同步的游戏数")
    private int totalGames;

    /** 成功拉取的游戏数 */
    @Schema(description = "成功拉取的游戏数")
    private int successGames;

    /** 拉取失败的游戏数 */
    @Schema(description = "拉取失败的游戏数")
    private int failedGames;

    /** 写入或更新的缓存条数 */
    @Schema(description = "写入或更新的缓存条数")
    private int modsSaved;

    /** 最近一次失败摘要，成功时为 null */
    @Schema(description = "最近一次失败摘要")
    private String message;
}
