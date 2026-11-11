package com.steamhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 游戏别名实体，用于「老头环」这类中文俗称的检索命中。
 */
@Data
@Entity
@Table(name = "game_alias")
public class GameAlias {

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 所属游戏 ID，非空 */
    @Column(name = "game_id", nullable = false)
    private Long gameId;

    /** 别名，非空 */
    @Column(name = "alias", length = 200, nullable = false)
    private String alias;
}
