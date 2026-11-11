package com.steamhub.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 帧率反馈实体，存放登录用户回填的实际帧率，用于校准基准数据，对应表 fps_feedback。
 */
@Data
@TableName("fps_feedback")
public class FpsFeedback {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交反馈的用户 ID，非空 */
    private Long userId;

    /** 所属游戏 ID，非空 */
    private Long gameId;

    /** GPU 档位编码，非空 */
    private String gpuTier;

    /** 分辨率：1080P / 2K / 4K，非空 */
    private String resolution;

    /** 提交时的画质设置快照（JSON 字符串），可空 */
    private String settingsJson;

    /** 用户回填的实际帧率 */
    private BigDecimal actualFps;

    /** 提交时间，插入时为空由数据库默认值填充 */
    private LocalDateTime createdAt;
}
