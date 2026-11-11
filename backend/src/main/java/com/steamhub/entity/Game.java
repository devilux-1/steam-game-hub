package com.steamhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 游戏主表实体，一个游戏聚合 Nexus Mod、汉化补丁、修改器三类资源，对应表 game。
 */
@Data
@TableName("game")
public class Game {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 中文名，可空 */
    private String nameCn;

    /** 英文名，非空 */
    private String nameEn;

    /** Steam AppID，可空，用于游戏库匹配与商店信息拉取 */
    private Integer steamAppid;

    /** Nexus 的 URL 标识，可空，如 eldenring */
    private String nexusDomain;

    /** 封面图地址 */
    private String coverUrl;

    /** 简介 */
    private String summary;

    /** 发售日期，可空 */
    private LocalDate releaseDate;

    /** 创建时间，插入时为空由数据库默认值填充，查询后由数据库值回填 */
    private LocalDateTime createdAt;

    /** 更新时间，由数据库 ON UPDATE CURRENT_TIMESTAMP 维护 */
    private LocalDateTime updatedAt;
}
