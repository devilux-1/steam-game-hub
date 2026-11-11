package com.steamhub.repository;

import com.steamhub.entity.GameAlias;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 游戏别名表数据访问接口。
 */
public interface GameAliasRepository extends JpaRepository<GameAlias, Long> {
}
