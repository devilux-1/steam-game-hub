package com.steamhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 画质选项实体，记录某个游戏可调选项的性能代价与视觉收益权重。
 */
@Data
@Entity
@Table(name = "graphics_option")
public class GraphicsOption {

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 所属游戏 ID，非空 */
    @Column(name = "game_id", nullable = false)
    private Long gameId;

    /** 选项名称，如「阴影质量」，与 gameId 组成唯一键 */
    @Column(name = "option_name", length = 100, nullable = false)
    private String optionName;

    /** 可选档位，逗号分隔，如「低,中,高,超高」 */
    @Column(name = "levels", length = 200, nullable = false)
    private String levels;

    /** 每提升一级的帧率代价权重，1.00 为基准 */
    @Column(name = "cost_weight", precision = 4, scale = 2, nullable = false)
    private BigDecimal costWeight;

    /** 视觉收益权重，1.00 为基准 */
    @Column(name = "visual_value", precision = 4, scale = 2, nullable = false)
    private BigDecimal visualValue;

    /** 默认推荐档位下标，默认 0 */
    @Column(name = "default_level_index", nullable = false)
    private Integer defaultLevelIndex;
}
