package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 当前登录用户视图对象，未登录时接口返回 data=null。
 */
@Data
@Schema(description = "当前登录用户")
public class MeVO {

    /** 用户主键 */
    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    /** SteamID64 */
    @Schema(description = "SteamID64", example = "76561197960287930")
    private String steamId;

    /** Steam 昵称 */
    @Schema(description = "Steam 昵称")
    private String personaName;

    /** 头像地址 */
    @Schema(description = "头像地址")
    private String avatarUrl;
}
