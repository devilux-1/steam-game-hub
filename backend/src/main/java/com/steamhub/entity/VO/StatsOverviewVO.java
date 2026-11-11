package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 站内统计概览视图对象，供首页展示游戏数与资源数。
 */
@Data
@Schema(description = "站内统计概览")
public class StatsOverviewVO {

    /** 收录的游戏总数 */
    @Schema(description = "收录的游戏总数", example = "0")
    private Long gameCount;

    /** 收录的资源条目总数 */
    @Schema(description = "收录的资源条目总数", example = "0")
    private Long resourceCount;
}
