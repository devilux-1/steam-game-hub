package com.steamhub.entity.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 后台新增或编辑游戏的入参。
 * id 为空表示新增，非空表示编辑；别名一并提交，后台按全量覆盖处理。
 */
@Data
@Schema(description = "游戏新增或编辑入参")
public class GameSaveDTO {

    /** 游戏 ID，编辑时必填，新增时为空 */
    @Schema(description = "游戏 ID，编辑时必填，新增时为空")
    private Long id;

    /** 中文名，可空 */
    @Size(max = 200, message = "中文名不能超过 200 字符")
    @Schema(description = "中文名")
    private String nameCn;

    /** 英文名，非空 */
    @NotBlank(message = "英文名不能为空")
    @Size(max = 200, message = "英文名不能超过 200 字符")
    @Schema(description = "英文名")
    private String nameEn;

    /** Steam AppID，可空 */
    @Schema(description = "Steam AppID")
    private Integer steamAppid;

    /** Nexus 的 URL 标识，可空，如 eldenring */
    @Size(max = 100, message = "Nexus 标识不能超过 100 字符")
    @Schema(description = "Nexus 的 URL 标识")
    private String nexusDomain;

    /** 封面图地址，可空 */
    @Size(max = 500, message = "封面地址不能超过 500 字符")
    @Schema(description = "封面图地址")
    private String coverUrl;

    /** 简介，可空 */
    @Size(max = 1000, message = "简介不能超过 1000 字符")
    @Schema(description = "简介")
    private String summary;

    /** 发售日期，可空 */
    @Schema(description = "发售日期")
    private LocalDate releaseDate;

    /** 别名列表，可空；提交后全量替换该游戏的别名 */
    @Schema(description = "别名列表，如「老头环」")
    private List<String> aliases;
}
