package com.steamhub.controller;

import com.steamhub.common.PageResult;
import com.steamhub.common.Result;
import com.steamhub.entity.DTO.GameQueryDTO;
import com.steamhub.entity.DTO.GameSaveDTO;
import com.steamhub.entity.VO.GameOptionVO;
import com.steamhub.entity.VO.GameVO;
import com.steamhub.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台游戏管理接口，完整路径前缀 /api/admin/games。
 * 除登录外的后台接口均需在请求头携带 Authorization: Bearer {token}。
 */
@RestController
@RequestMapping("/admin/games")
@Tag(name = "后台-游戏管理")
@RequiredArgsConstructor
public class AdminGameController {

    /** 游戏业务层 */
    private final GameService gameService;

    /**
     * 后台分页查询游戏，统计口径包含已下架资源。
     *
     * @param query 查询条件，非空；keyword 可空
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "后台游戏列表")
    public Result<PageResult<GameVO>> list(@Valid GameQueryDTO query) {
        return Result.success(gameService.adminSearch(query));
    }

    /**
     * 查询游戏下拉选项，供资源管理选择所属游戏。
     *
     * @return 选项列表，按 id 倒序
     */
    @GetMapping("/options")
    @Operation(summary = "游戏下拉选项")
    public Result<List<GameOptionVO>> options() {
        return Result.success(gameService.listOptions());
    }

    /**
     * 新增游戏，别名一并写入。
     *
     * @param dto 游戏入参，非空；English 名必填
     * @return 新增游戏的 ID
     */
    @PostMapping
    @Operation(summary = "新增游戏")
    public Result<Long> create(@RequestBody @Valid GameSaveDTO dto) {
        return Result.success(gameService.create(dto));
    }

    /**
     * 编辑游戏，path 中的 ID 优先。
     *
     * @param id  游戏 ID，非空
     * @param dto 游戏入参，非空
     * @return 无业务数据
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑游戏")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid GameSaveDTO dto) {
        gameService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除游戏，同时清理其别名与资源。
     *
     * @param id 游戏 ID，非空
     * @return 无业务数据
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除游戏")
    public Result<Void> delete(@PathVariable Long id) {
        gameService.delete(id);
        return Result.success();
    }
}
