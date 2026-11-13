package com.steamhub.controller;

import com.steamhub.common.PageResult;
import com.steamhub.common.Result;
import com.steamhub.entity.DTO.ResourceSaveDTO;
import com.steamhub.entity.VO.ResourceVO;
import com.steamhub.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台资源管理接口，完整路径前缀 /api/admin/resources。
 * 需在请求头携带 Authorization: Bearer {token}。
 */
@RestController
@RequestMapping("/admin/resources")
@Tag(name = "后台-资源管理")
@RequiredArgsConstructor
@Validated
public class AdminResourceController {

    /** 资源业务层 */
    private final ResourceService resourceService;

    /**
     * 后台分页查询资源，包含已下架数据。
     *
     * @param gameId 游戏 ID，可空；为空时查询全部
     * @param page   页码，从 1 开始，默认 1
     * @param size   每页条数，1-100，默认 20
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "后台资源列表")
    public Result<PageResult<ResourceVO>> list(@RequestParam(required = false) Long gameId,
                                               @RequestParam(defaultValue = "1") @Min(1) int page,
                                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return Result.success(resourceService.adminList(gameId, page, size));
    }

    /**
     * 新增资源。
     *
     * @param dto 资源入参，非空
     * @return 新增资源的 ID
     */
    @PostMapping
    @Operation(summary = "新增资源")
    public Result<Long> create(@RequestBody @Valid ResourceSaveDTO dto) {
        return Result.success(resourceService.create(dto));
    }

    /**
     * 编辑资源，path 中的 ID 优先。
     *
     * @param id  资源 ID，非空
     * @param dto 资源入参，非空
     * @return 无业务数据
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑资源")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid ResourceSaveDTO dto) {
        resourceService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除资源。
     *
     * @param id 资源 ID，非空
     * @return 无业务数据
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除资源")
    public Result<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return Result.success();
    }
}
