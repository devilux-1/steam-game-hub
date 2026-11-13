package com.steamhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.steamhub.entity.PO.GameAlias;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 游戏别名表数据访问接口。
 */
public interface GameAliasMapper extends BaseMapper<GameAlias> {

    /**
     * 按别名模糊匹配游戏 ID，供搜索时把「老头环」这类俗称并入结果。
     *
     * @param keyword 搜索关键字，非空；为空时由调用方保证不调用本方法
     * @return 命中的游戏 ID 列表，去重；无命中返回空列表
     */
    @Select("SELECT DISTINCT game_id FROM game_alias WHERE alias LIKE CONCAT('%', #{keyword}, '%')")
    List<Long> selectGameIdsByAliasLike(@Param("keyword") String keyword);

    /**
     * 查询某个游戏的全部别名，按录入顺序返回。
     *
     * @param gameId 游戏 ID，非空
     * @return 别名列表；无别名时返回空列表
     */
    @Select("SELECT alias FROM game_alias WHERE game_id = #{gameId} ORDER BY id")
    List<String> selectAliasesByGameId(@Param("gameId") Long gameId);
}
