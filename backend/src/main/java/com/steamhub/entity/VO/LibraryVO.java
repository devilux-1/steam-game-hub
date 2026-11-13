package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 我的游戏库页面数据：统计信息 + 列表，一次请求返回。
 */
@Data
@Schema(description = "我的游戏库")
public class LibraryVO {

    /** 顶部统计 */
    @Schema(description = "统计信息")
    private LibrarySummaryVO summary;

    /** 游戏列表 */
    @Schema(description = "游戏列表")
    private List<LibraryItemVO> items;
}
