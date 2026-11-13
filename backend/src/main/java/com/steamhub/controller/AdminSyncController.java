package com.steamhub.controller;

import com.steamhub.common.Result;
import com.steamhub.entity.VO.NexusSyncStatusVO;
import com.steamhub.service.NexusSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台 Nexus 同步接口，完整路径前缀 /api/admin/sync/nexus。
 * 需在请求头携带 Authorization: Bearer {token}。
 */
@RestController
@RequestMapping("/admin/sync")
@Tag(name = "后台-Nexus 同步")
@RequiredArgsConstructor
public class AdminSyncController {

    /** Nexus 同步服务 */
    private final NexusSyncService nexusSyncService;

    /**
     * 手动触发一次 Nexus 全量同步。
     * 同步在后台线程执行，接口立即返回，进度通过状态接口轮询。
     *
     * @return started=true 表示已开始；false 表示已有任务在跑
     */
    @PostMapping("/nexus")
    @Operation(summary = "触发 Nexus 同步")
    public Result<Map<String, Boolean>> trigger() {
        boolean started = nexusSyncService.triggerAsync();
        return Result.success(Map.of("started", started));
    }

    /**
     * 查询 Nexus 同步状态。
     *
     * @return 同步状态，含运行标记、上次结果统计与演示模式标记
     */
    @GetMapping("/nexus/status")
    @Operation(summary = "Nexus 同步状态")
    public Result<NexusSyncStatusVO> status() {
        return Result.success(nexusSyncService.getStatus());
    }
}
