package com.steamhub.controller;

import com.steamhub.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口，用于确认服务已启动、前后端联调通路可用（完整路径 /api/health）。
 */
@RestController
@RequestMapping("/health")
@Tag(name = "健康检查")
public class HealthController {

    /** 服务健康状态标识 */
    private static final String STATUS_UP = "UP";

    /** 服务名，返回给调用方确认请求打到了正确的应用 */
    private static final String SERVICE_NAME = "steam-game-hub";

    /**
     * 返回服务健康状态。
     *
     * @return 统一响应体，data 含 status、service、timestamp 三项
     */
    @GetMapping
    @Operation(summary = "健康检查")
    public ApiResponse<Map<String, Object>> health() {
        // 用 LinkedHashMap 保证返回字段顺序固定，便于前端与联调脚本比对
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", STATUS_UP);
        data.put("service", SERVICE_NAME);
        data.put("timestamp", LocalDateTime.now().toString());
        return ApiResponse.ok(data);
    }
}
