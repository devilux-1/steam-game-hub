package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 游戏下拉选项视图对象，供后台资源管理选择所属游戏。
 */
@Data
@Schema(description = "游戏下拉选项")
public class GameOptionVO {

    /** 游戏 ID */
    @Schema(description = "游戏 ID", example = "1")
    private Long id;

    /** 中文名，可空 */
    @Schema(description = "中文名")
    private String nameCn;

    /** 英文名，非空 */
    @Schema(description = "英文名")
    private String nameEn;
}
