package com.steamhub.entity.DTO;

import lombok.Data;

/**
 * Steam 玩家资料，来自 ISteamUser/GetPlayerSummaries。
 */
@Data
public class SteamPlayerDTO {

    /** SteamID64 */
    private String steamId;

    /** 昵称，可能为空 */
    private String personaName;

    /** 大尺寸头像地址，可能为空 */
    private String avatarUrl;
}
