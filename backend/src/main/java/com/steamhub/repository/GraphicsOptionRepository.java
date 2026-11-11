package com.steamhub.repository;

import com.steamhub.entity.GraphicsOption;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 画质选项表数据访问接口。
 */
public interface GraphicsOptionRepository extends JpaRepository<GraphicsOption, Long> {
}
