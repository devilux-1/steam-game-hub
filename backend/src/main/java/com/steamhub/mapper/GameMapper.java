package com.steamhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.steamhub.entity.PO.Game;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 游戏表数据访问接口，继承 BaseMapper 获得通用 CRUD。
 */
public interface GameMapper extends BaseMapper<Game> {

    /**
     * 按热度（上架资源数）分页查询游戏，热度相同按 id 倒序，保证分页结果稳定。
     * 关键字同时匹配中文名与英文名；别名命中的游戏由调用方先查 game_alias 得到 id 列表后经 ids 传入。
     *
     * @param page       分页参数，页码从 1 开始，非空
     * @param keyword    搜索关键字，可空；为空或空白串时不加关键字条件
     * @param ids        别名命中或其它来源命中的游戏 ID 集合，可空；为空时只按名称匹配
     * @param onlyOnline 是否只统计上架资源（status=1）；前台传 true，后台传 false
     * @return 分页结果，空页时 records 为空列表
     */
    @Select("<script>"
            + "SELECT g.* FROM game g "
            + "LEFT JOIN (SELECT game_id, COUNT(*) AS cnt FROM resource "
            + "<if test='onlyOnline'>WHERE status = 1</if>"
            + " GROUP BY game_id) r ON r.game_id = g.id "
            + "<where>"
            + "<if test='keyword != null and keyword != \"\"'>"
            + "<choose>"
            + "<when test='ids != null and ids.size() > 0'>"
            + "AND (g.name_cn LIKE CONCAT('%', #{keyword}, '%') "
            + "OR g.name_en LIKE CONCAT('%', #{keyword}, '%') "
            + "OR g.id IN <foreach collection='ids' item='gid' open='(' separator=',' close=')'>#{gid}</foreach>)"
            + "</when>"
            + "<otherwise>"
            + "AND (g.name_cn LIKE CONCAT('%', #{keyword}, '%') "
            + "OR g.name_en LIKE CONCAT('%', #{keyword}, '%'))"
            + "</otherwise>"
            + "</choose>"
            + "</if>"
            + "</where>"
            + " ORDER BY COALESCE(r.cnt, 0) DESC, g.id DESC"
            + "</script>")
    IPage<Game> selectPageByHeat(Page<Game> page,
                                 @Param("keyword") String keyword,
                                 @Param("ids") List<Long> ids,
                                 @Param("onlyOnline") boolean onlyOnline);
}
