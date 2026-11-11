package com.steamhub.entity.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 游戏搜索查询参数，支持关键字与分页。
 */
@Data
@Schema(description = "游戏搜索查询参数")
public class GameQueryDTO {

    /** 搜索关键字，可空；为空时按热度返回全部 */
    @Schema(description = "搜索关键字，匹配中文名、英文名或别名")
    private String keyword;

    /** 页码，从 1 开始，默认 1 */
    @Min(value = 1, message = "页码不能小于 1")
    @Schema(description = "页码，从 1 开始", example = "1")
    private Integer page = 1;

    /** 每页条数，默认 20，上限 100 防止一次拉取过多数据 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能大于 100")
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;
}
