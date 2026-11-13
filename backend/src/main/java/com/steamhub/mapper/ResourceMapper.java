package com.steamhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.steamhub.entity.PO.Resource;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 资源表数据访问接口。
 */
public interface ResourceMapper extends BaseMapper<Resource> {

    /**
     * 点击计数原子自增，直接由数据库完成累加，避免「先查后写」在并发下丢失计数。
     *
     * @param id 资源 ID，非空
     * @return 受影响行数，0 表示该资源不存在
     */
    @Update("UPDATE resource SET click_count = click_count + 1 WHERE id = #{id}")
    int increaseClick(@Param("id") Long id);
}
