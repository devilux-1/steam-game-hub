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
 * 基准帧率实体，记录某游戏在某 GPU 档位某分辨率下中画质的实测帧率。
 */
@Data
@Entity
@Table(name = "fps_baseline")
public class FpsBaseline {

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 所属游戏 ID，非空 */
    @Column(name = "game_id", nullable = false)
    private Long gameId;

    /** GPU 档位编码，如 RTX4060，与 gameId、resolution 组成唯一键 */
    @Column(name = "gpu_tier", length = 20, nullable = false)
    private String gpuTier;

    /** 分辨率：1080P / 2K / 4K，默认 1080P */
    @Column(name = "resolution", length = 10, nullable = false)
    private String resolution;

    /** 基准预设，固定 MEDIUM */
    @Column(name = "ref_preset", length = 20, nullable = false)
    private String refPreset;

    /** 该组合在基准预设下的实测帧率 */
    @Column(name = "fps", precision = 6, scale = 1, nullable = false)
    private BigDecimal fps;

    /** 参与统计的样本数，默认 0 */
    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount;
}
