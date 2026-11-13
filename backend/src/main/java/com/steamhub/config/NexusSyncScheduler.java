package com.steamhub.config;

import com.steamhub.service.NexusSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nexus 数据定时同步任务。
 * 通过 nexus.api.enabled 控制启停；未配置 API Key 时仍会执行，但内部走演示数据模式。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nexus.api", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NexusSyncScheduler {

    /** Nexus 同步服务 */
    private final NexusSyncService nexusSyncService;

    /**
     * 按配置的 cron 触发同步，默认每天凌晨 4 点。
     * 具体 cron 由 nexus.api.cron 提供，写在配置文件而非注解里，便于按环境调整。
     */
    @Scheduled(cron = "${nexus.api.cron:0 0 4 * * ?}")
    public void syncDaily() {
        log.info("定时任务开始：Nexus 数据同步");
        boolean started = nexusSyncService.triggerAsync();
        if (!started) {
            log.info("定时任务跳过：已有同步任务在执行");
        }
    }
}
