package com.steamhub.entity.DTO;

import lombok.Data;

/**
 * Nexus Mod 数据的中间对象，由客户端解析接口响应后产出，再由同步服务落库。
 */
@Data
public class NexusModDTO {

    /** Nexus 侧 mod id，与游戏 domain 共同确定一条 Mod */
    private Integer modId;

    /** Mod 名称 */
    private String name;

    /** 作者，可空 */
    private String author;

    /** 点赞数，用于热度排序 */
    private Integer endorsements;

    /** 下载量 */
    private Long downloads;

    /** 简介，可空 */
    private String summary;

    /** Mod 详情页地址 */
    private String modUrl;
}
