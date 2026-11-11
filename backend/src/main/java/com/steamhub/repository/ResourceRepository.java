package com.steamhub.repository;

import com.steamhub.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 资源表数据访问接口。
 */
public interface ResourceRepository extends JpaRepository<Resource, Long> {
}
