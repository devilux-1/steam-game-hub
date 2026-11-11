package com.steamhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源表实体，Nexus Mod、汉化补丁、风灵月影修改器统一存储，用 type 区分，对应表 resource。
 */
@Data
@TableName("resource")
public class Resource {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属游戏 ID，非空 */
    private Long gameId;

    /** 资源类型：NEXUS_MOD / HANZI_PATCH / TRAINER，非空 */
    private String type;

    /** 资源名称，非空 */
    private String title;

    /** 版本号，可空 */
    private String version;

    /** 适配的游戏版本，可空 */
    private String compatibleVersion;

    /** 作者或汉化组，可空 */
    private String author;

    /** 来源站标识，可空 */
    private String sourceSite;

    /** 跳转地址，非空，本站不托管文件 */
    private String sourceUrl;

    /** 一句话说明，可空 */
    private String description;

    /** 安装要点，可空 */
    private String installNote;

    /** 点击计数，默认 0 */
    private Long clickCount;

    /** 排序权重，越大越靠前，默认 0 */
    private Integer sortWeight;

    /** 上下架状态：1 上架 / 0 下架，默认 1 */
    private Integer status;

    /** 创建时间，插入时为空由数据库默认值填充 */
    private LocalDateTime createdAt;

    /** 更新时间，由数据库 ON UPDATE CURRENT_TIMESTAMP 维护 */
    private LocalDateTime updatedAt;
}
