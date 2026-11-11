package com.steamhub.repository;

import com.steamhub.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 游戏表数据访问接口。
 */
public interface GameRepository extends JpaRepository<Game, Long> {
}
