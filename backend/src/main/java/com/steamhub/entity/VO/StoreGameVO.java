package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Steam 商店信息视图对象：给未收录进站内条目的库内游戏补充简介与截图。
 * 数据来自 Steam Store 的 appdetails 接口，取不到时整对象为 null。
 */
@Data
@Schema(description = "Steam 商店信息")
public class StoreGameVO {

    /** 商店名称 */
    @Schema(description = "商店名称", example = "Stardew Valley")
    private String name;

    /** 简短简介（简体中文） */
    @Schema(description = "简短简介")
    private String shortDescription;

    /** 头图地址 */
    @Schema(description = "头图地址")
    private String headerImage;

    /** 开发商，多个用顿号连接 */
    @Schema(description = "开发商")
    private String developers;

    /** 类型标签，如「独立」「角色扮演」 */
    @Schema(description = "类型标签")
    private List<String> genres = new ArrayList<>();

    /** 截图地址列表，最多 8 张 */
    @Schema(description = "截图地址列表")
    private List<String> screenshots = new ArrayList<>();

    /** 发售日期文案，Steam 已格式化，如「2016 年 2 月 26 日」 */
    @Schema(description = "发售日期")
    private String releaseDate;

    /** 对应的 Steam 商店地址 */
    @Schema(description = "Steam 商店地址")
    private String storeUrl;
}
