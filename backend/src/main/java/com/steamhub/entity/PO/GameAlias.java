package com.steamhub.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 游戏别名实体，用于「老头环」这类中文俗称的检索命中，对应表 game_alias。
 */
@Data
@TableName("game_alias")
public class GameAlias {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属游戏 ID，非空 */
    private Long gameId;

    /** 别名，非空 */
    private String alias;
}
