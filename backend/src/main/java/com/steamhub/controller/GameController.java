package com.steamhub.controller;

import com.steamhub.common.PageResult;
import com.steamhub.common.Result;
import com.steamhub.entity.DTO.GameQueryDTO;
import com.steamhub.entity.VO.GameDetailVO;
import com.steamhub.entity.VO.GameVO;
import com.steamhub.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 游戏公开接口：搜索列表与详情，完整路径前缀 /api/games。
 */
@RestController
@RequestMapping("/games")
@Tag(name = "游戏")
@RequiredArgsConstructor
public class GameController {

    /** 游戏业务层 */
    private final GameService gameService;

    /**
     * 分页搜索游戏，支持中文名、英文名与别名。
     *
     * @param query 查询条件，非空；keyword 可空表示按热度返回全部，page 从 1 开始
     * @return 分页结果，data 为 PageResult，内含 GameVO 列表
     */
    @GetMapping
    @Operation(summary = "搜索游戏")
    public Result<PageResult<GameVO>> list(@Valid GameQueryDTO query) {
        return Result.success(gameService.search(query));
    }

    /**
     * 查询游戏详情，含别名与资源数。
     *
     * @param id 游戏 ID，非空
     * @return 游戏详情；不存在时由全局异常处理器返回 404
     */
    @GetMapping("/{id}")
    @Operation(summary = "游戏详情")
    public Result<GameDetailVO> detail(@PathVariable Long id) {
        return Result.success(gameService.detail(id));
    }
}
