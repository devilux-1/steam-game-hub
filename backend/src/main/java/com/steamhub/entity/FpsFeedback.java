package com.steamhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 帧率反馈实体，存放登录用户回填的实际帧率，用于校准基准数据。
 */
@Data
@Entity
@Table(name = "fps_feedback")
public class FpsFeedback {

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 提交反馈的用户 ID，非空 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 所属游戏 ID，非空 */
    @Column(name = "game_id", nullable = false)
    private Long gameId;

    /** GPU 档位编码，非空 */
    @Column(name = "gpu_tier", length = 20, nullable = false)
    private String gpuTier;

    /** 分辨率：1080P / 2K / 4K，非空 */
    @Column(name = "resolution", length = 10, nullable = false)
    private String resolution;

    /** 提交时的画质设置快照（JSON 字符串），可空 */
    @Column(name = "settings_json", length = 2000)
    private String settingsJson;

    /** 用户回填的实际帧率 */
    @Column(name = "actual_fps", precision = 6, scale = 1, nullable = false)
    private BigDecimal actualFps;

    /** 提交时间，由数据库默认值填充 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
