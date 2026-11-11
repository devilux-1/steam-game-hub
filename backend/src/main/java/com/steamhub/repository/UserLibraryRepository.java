package com.steamhub.repository;

import com.steamhub.entity.UserLibrary;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户游戏库表数据访问接口。
 */
public interface UserLibraryRepository extends JpaRepository<UserLibrary, Long> {
}
