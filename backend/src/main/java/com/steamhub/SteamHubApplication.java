package com.steamhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Steam 单机游戏资源站后端启动类。
 */
@SpringBootApplication
public class SteamHubApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数，本地启动可追加 --spring.profiles.active=local 以启用 application-local.yml
     */
    public static void main(String[] args) {
        SpringApplication.run(SteamHubApplication.class, args);
    }
}
