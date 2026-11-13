package com.steamhub.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 游戏详情视图对象，在游戏基础信息之上补充别名列表，供游戏条目页展示与搜索提示。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "游戏详情")
public class GameDetailVO extends GameVO {

    /** 别名列表，如「老头环」；无别名时为空列表而非 null */
    @Schema(description = "别名列表")
    private List<String> aliases;
}
