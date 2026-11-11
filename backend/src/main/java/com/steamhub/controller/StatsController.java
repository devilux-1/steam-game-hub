package com.steamhub.controller;

import com.steamhub.common.ApiResponse;
import com.steamhub.repository.GameRepository;
import com.steamhub.repository.ResourceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 站内统计接口，供首页展示游戏数与资源数（完整路径 /api/stats/overview）。
 */
@RestController
@RequestMapping("/stats")
@Tag(name = "统计")
public class StatsController {

    /** 游戏数统计字段名 */
    private static final String KEY_GAME_COUNT = "gameCount";

    /** 资源数统计字段名 */
    private static final String KEY_RESOURCE_COUNT = "resourceCount";

    /** 游戏仓储，用于统计游戏总数 */
    private final GameRepository gameRepository;

    /** 资源仓储，用于统计资源总数 */
    private final ResourceRepository resourceRepository;

    /**
     * 构造统计接口，注入所需仓储。
     *
     * @param gameRepository     游戏仓储，非空
     * @param resourceRepository 资源仓储，非空
     */
    public StatsController(GameRepository gameRepository, ResourceRepository resourceRepository) {
        this.gameRepository = gameRepository;
        this.resourceRepository = resourceRepository;
    }

    /**
     * 统计站内游戏数与资源总数。
     *
     * @return 统一响应体，data 含 gameCount（游戏数）与 resourceCount（资源数）
     */
    @GetMapping("/overview")
    @Operation(summary = "站内统计概览")
    public ApiResponse<Map<String, Long>> overview() {
        // 键顺序固定为游戏数、资源数，便于前端直接读取
        Map<String, Long> data = new LinkedHashMap<>();
        data.put(KEY_GAME_COUNT, gameRepository.count());
        data.put(KEY_RESOURCE_COUNT, resourceRepository.count());
        return ApiResponse.ok(data);
    }
}
