package com.steamhub.entity.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台新增或编辑资源的入参。
 * id 为空表示新增，非空表示编辑；本站只存外链，不接收文件。
 */
@Data
@Schema(description = "资源新增或编辑入参")
public class ResourceSaveDTO {

    /** 资源 ID，编辑时必填，新增时为空 */
    @Schema(description = "资源 ID，编辑时必填，新增时为空")
    private Long id;

    /** 所属游戏 ID，非空 */
    @NotNull(message = "所属游戏不能为空")
    @Schema(description = "所属游戏 ID")
    private Long gameId;

    /** 资源类型：NEXUS_MOD / HANZI_PATCH / TRAINER，非空 */
    @NotBlank(message = "资源类型不能为空")
    @Size(max = 20, message = "资源类型不能超过 20 字符")
    @Schema(description = "资源类型：NEXUS_MOD / HANZI_PATCH / TRAINER")
    private String type;

    /** 资源名称，非空 */
    @NotBlank(message = "资源名称不能为空")
    @Size(max = 300, message = "资源名称不能超过 300 字符")
    @Schema(description = "资源名称")
    private String title;

    /** 版本号，可空 */
    @Size(max = 50, message = "版本号不能超过 50 字符")
    @Schema(description = "版本号")
    private String version;

    /** 适配的游戏版本，可空 */
    @Size(max = 100, message = "适配版本不能超过 100 字符")
    @Schema(description = "适配的游戏版本")
    private String compatibleVersion;

    /** 作者或汉化组，可空 */
    @Size(max = 100, message = "作者不能超过 100 字符")
    @Schema(description = "作者或汉化组")
    private String author;

    /** 来源站标识，可空 */
    @Size(max = 50, message = "来源站不能超过 50 字符")
    @Schema(description = "来源站标识")
    private String sourceSite;

    /** 跳转地址，非空 */
    @NotBlank(message = "跳转地址不能为空")
    @Size(max = 1000, message = "跳转地址不能超过 1000 字符")
    @Schema(description = "跳转地址")
    private String sourceUrl;

    /** 一句话说明，可空 */
    @Size(max = 500, message = "说明不能超过 500 字符")
    @Schema(description = "一句话说明")
    private String description;

    /** 安装要点，可空 */
    @Size(max = 1000, message = "安装要点不能超过 1000 字符")
    @Schema(description = "安装要点")
    private String installNote;

    /** 排序权重，越大越靠前，可空，默认 0 */
    @Schema(description = "排序权重，越大越靠前", example = "0")
    private Integer sortWeight;

    /** 上下架状态：1 上架 / 0 下架，可空，默认 1 */
    @Schema(description = "上下架状态：1 上架 / 0 下架", example = "1")
    private Integer status;
}
