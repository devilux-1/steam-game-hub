package com.steamhub.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 基准帧率实体，记录某游戏在某 GPU 档位某分辨率下中画质的实测帧率，对应表 fps_baseline。
 */
@Data
@TableName("fps_baseline")
public class FpsBaseline {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属游戏 ID，非空 */
    private Long gameId;

    /** GPU 档位编码，如 RTX4060，与 gameId、resolution 组成唯一键 */
    private String gpuTier;

    /** 分辨率：1080P / 2K / 4K，默认 1080P */
    private String resolution;

    /** 基准预设，固定 MEDIUM */
    private String refPreset;

    /** 该组合在基准预设下的实测帧率 */
    private BigDecimal fps;

    /** 参与统计的样本数，默认 0 */
    private Integer sampleCount;
}
