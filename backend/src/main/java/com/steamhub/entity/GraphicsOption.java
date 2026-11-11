package com.steamhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 画质选项实体，记录某个游戏可调选项的性能代价与视觉收益权重，对应表 graphics_option。
 */
@Data
@TableName("graphics_option")
public class GraphicsOption {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属游戏 ID，非空 */
    private Long gameId;

    /** 选项名称，如「阴影质量」，与 gameId 组成唯一键 */
    private String optionName;

    /** 可选档位，逗号分隔，如「低,中,高,超高」 */
    private String levels;

    /** 每提升一级的帧率代价权重，1.00 为基准 */
    private BigDecimal costWeight;

    /** 视觉收益权重，1.00 为基准 */
    private BigDecimal visualValue;

    /** 默认推荐档位下标，默认 0 */
    private Integer defaultLevelIndex;
}
