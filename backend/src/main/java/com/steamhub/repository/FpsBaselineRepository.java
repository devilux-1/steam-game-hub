package com.steamhub.repository;

import com.steamhub.entity.FpsBaseline;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 基准帧率表数据访问接口。
 */
public interface FpsBaselineRepository extends JpaRepository<FpsBaseline, Long> {
}
