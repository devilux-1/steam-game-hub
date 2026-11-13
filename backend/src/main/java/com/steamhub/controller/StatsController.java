package com.steamhub.controller;

import com.steamhub.common.Result;
import com.steamhub.entity.VO.StatsOverviewVO;
import com.steamhub.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内统计接口，供首页展示游戏数与资源数（完整路径 /api/stats/overview）。
 * Controller 只接收请求并返回统一结果体，统计逻辑在 {@link StatsService}。
 */
@RestController
@RequestMapping("/stats")
@Tag(name = "统计")
@RequiredArgsConstructor
public class StatsController {

    /** 站内统计业务层 */
    private final StatsService statsService;

    /**
     * 统计站内游戏数与资源总数。
     *
     * @return 统一结果体，data 为统计概览视图对象，含游戏数与资源数
     */
    @GetMapping("/overview")
    @Operation(summary = "站内统计概览")
    public Result<StatsOverviewVO> overview() {
        return Result.success(statsService.getOverview());
    }
}
