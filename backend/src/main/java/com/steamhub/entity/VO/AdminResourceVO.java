package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台资源视图对象：在展示字段之外补充游戏 ID、上下架状态与排序权重，
 * 使后台编辑弹窗可以完整回填，避免提交时把未展示字段重置为默认值。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台资源信息")
public class AdminResourceVO extends ResourceVO {

    /** 所属游戏 ID，后台编辑时回填游戏下拉 */
    @Schema(description = "所属游戏 ID", example = "1")
    private Long gameId;

    /** 排序权重，越大越靠前 */
    @Schema(description = "排序权重", example = "0")
    private Integer sortWeight;

    /** 上下架状态：1 上架 / 0 下架 */
    @Schema(description = "上下架状态：1 上架 / 0 下架", example = "1")
    private Integer status;
}
