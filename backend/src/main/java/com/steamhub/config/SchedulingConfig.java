package com.steamhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务开关：Nexus 每日同步依赖 @Scheduled 生效。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
