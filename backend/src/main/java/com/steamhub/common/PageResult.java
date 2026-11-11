package com.steamhub.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果，作为 {@link Result} 的 data 返回给前端。
 *
 * @param <T> 列表元素类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageResult<T> {

    /** 当前页数据，空页为空列表而非 null */
    @Schema(description = "当前页数据")
    private List<T> list;

    /** 满足条件的总记录数 */
    @Schema(description = "总记录数", example = "0")
    private long total;

    /** 当前页码，从 1 开始 */
    @Schema(description = "当前页码，从 1 开始", example = "1")
    private int page;

    /** 每页条数 */
    @Schema(description = "每页条数", example = "20")
    private int size;

    /**
     * 根据当前页数据与总数构造分页结果。
     *
     * @param list  当前页数据，空页传空列表
     * @param total 满足条件的总记录数，非负
     * @param page  当前页码，从 1 开始
     * @param size  每页条数，不小于 1
     * @param <T>   列表元素类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        return new PageResult<>(list, total, page, size);
    }

    /**
     * 构造空分页结果，用于查询无命中等场景。
     *
     * @param page 当前页码，从 1 开始
     * @param size 每页条数，不小于 1
     * @param <T>  列表元素类型
     * @return total=0 且 list 为空列表的分页结果
     */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), 0L, page, size);
    }
}
