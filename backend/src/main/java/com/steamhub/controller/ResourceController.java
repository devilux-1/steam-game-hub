package com.steamhub.controller;

import com.steamhub.common.Result;
import com.steamhub.entity.VO.ResourceVO;
import com.steamhub.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 资源公开接口：按游戏查询资源与记录点击。
 * 路径挂在 /games 下以保持「一个游戏一页」的语义，完整路径前缀 /api。
 */
@RestController
@Tag(name = "资源")
@RequiredArgsConstructor
public class ResourceController {

    /** 资源业务层 */
    private final ResourceService resourceService;

    /**
     * 查询某个游戏下的上架资源。
     *
     * @param gameId 游戏 ID，非空
     * @param type   资源类型，可空；为空时返回全部类型
     * @return 资源列表，无数据时为空列表
     */
    @GetMapping("/games/{gameId}/resources")
    @Operation(summary = "按游戏查询资源")
    public Result<List<ResourceVO>> listByGame(@PathVariable Long gameId,
                                               @RequestParam(required = false) String type) {
        return Result.success(resourceService.listByGame(gameId, type));
    }

    /**
     * 记录一次资源点击，供前端跳转前调用。
     *
     * @param id 资源 ID，非空
     * @return 无业务数据；资源不存在时返回 404
     */
    @PostMapping("/resources/{id}/click")
    @Operation(summary = "资源点击计数")
    public Result<Void> click(@PathVariable Long id) {
        resourceService.increaseClick(id);
        return Result.success();
    }
}
