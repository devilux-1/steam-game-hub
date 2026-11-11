package com.steamhub.repository;

import com.steamhub.entity.NexusModCache;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Nexus Mod 缓存表数据访问接口。
 */
public interface NexusModCacheRepository extends JpaRepository<NexusModCache, Long> {
}
