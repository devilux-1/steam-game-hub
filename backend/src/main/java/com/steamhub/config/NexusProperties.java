package com.steamhub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Nexus Mods 接口配置。
 * 密钥需在 https://www.nexusmods.com/users/myaccount?tab=api 申请，未配置时同步任务自动跳过。
 */
@Data
@Component
@ConfigurationProperties(prefix = "nexus.api")
public class NexusProperties {

    /** API Key，为空表示未配置，同步任务不执行 */
    private String key = "";

    /** 接口基础地址，一般不需要修改 */
    private String baseUrl = "https://api.nexusmods.com";

    /** 应用名，Nexus 要求调用方声明，用于其侧统计 */
    private String appName = "steam-game-hub";

    /** 应用版本号，随应用版本更新 */
    private String appVersion = "1.0.0";

    /** 是否启用定时同步 */
    private boolean enabled = true;

    /**
     * 演示数据模式：为 true 时不请求 Nexus，直接返回内置示例数据。
     * 用于未申请 Key 时验证同步链路与前端展示，生产环境必须为 false。
     */
    private boolean mock = false;

    /** 单次请求超时秒数 */
    private int timeoutSeconds = 15;

    /** 每个游戏最多缓存的 Mod 条数，控制接口配额消耗 */
    private int maxModsPerGame = 20;

    /** 相邻两次游戏请求之间的间隔毫秒数，避免触发 Nexus 限流 */
    private long requestIntervalMs = 700L;

    /** 定时同步的 cron 表达式，默认每天凌晨 4 点 */
    private String cron = "0 0 4 * * ?";

    /**
     * 判断是否处于演示数据模式：显式开启 mock，或未配置 Key 时，均走内置数据。
     *
     * @return true 表示不发起真实网络请求
     */
    public boolean isDemoMode() {
        return mock || key == null || key.isBlank();
    }
}
