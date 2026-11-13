package com.steamhub.entity.DTO;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Steam 游戏库里的一条游戏，来自 IPlayerService/GetOwnedGames。
 */
@Data
public class SteamOwnedGameDTO {

    /** Steam AppID */
    private Integer appid;

    /** 游戏名称，可能为空 */
    private String name;

    /** 总时长（分钟） */
    private Integer playtimeForever;

    /** 最近两周时长（分钟），可能为空 */
    private Integer playtime2weeks;

    /** 最后游玩时间，从未游玩时为空 */
    private LocalDateTime lastPlayedAt;
}
