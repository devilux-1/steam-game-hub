package com.steamhub.controller;

import com.steamhub.common.Result;
import com.steamhub.entity.VO.NexusModVO;
import com.steamhub.service.NexusModService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Nexus 精选 Mod 公开接口，完整路径 /api/games/{id}/nexus-mods。
 * 数据来自本地缓存，由定时任务或后台手动同步刷新。
 */
@RestController
@Tag(name = "Nexus 精选")
@RequiredArgsConstructor
@Validated
public class NexusModController {

    /** Nexus 精选查询服务 */
    private final NexusModService nexusModService;

    /**
     * 查询某游戏的 Nexus 精选 Mod。
     *
     * @param gameId 游戏 ID，非空
     * @param limit  返回条数上限，1-50，默认 20
     * @return Mod 列表，无缓存时为空数组
     */
    @GetMapping("/games/{gameId}/nexus-mods")
    @Operation(summary = "查询游戏的热门 Mod")
    public Result<List<NexusModVO>> listByGame(@PathVariable Long gameId,
                                               @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return Result.success(nexusModService.listByGame(gameId, limit));
    }
}
