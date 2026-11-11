package com.steamhub.repository;

import com.steamhub.entity.FpsFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 帧率反馈表数据访问接口。
 */
public interface FpsFeedbackRepository extends JpaRepository<FpsFeedback, Long> {
}
