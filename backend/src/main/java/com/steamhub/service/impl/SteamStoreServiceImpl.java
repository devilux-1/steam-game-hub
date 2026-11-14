package com.steamhub.service.impl;

import com.steamhub.client.SteamStoreClient;
import com.steamhub.entity.VO.StoreGameVO;
import com.steamhub.service.SteamStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Steam 商店信息缓存服务：进程内 TTL 缓存，避免每次打开详情页都请求外网。
 * 单机部署下够用；后续换 Redis 只需替换本类实现，调用方不变。
 */
@Service
@RequiredArgsConstructor
public class SteamStoreServiceImpl implements SteamStoreService {

    /** 成功结果的缓存时长 */
    private static final Duration SUCCESS_TTL = Duration.ofHours(24);

    /** 失败结果的缓存时长，防止外网异常时被反复重试拖慢页面 */
    private static final Duration FAILURE_TTL = Duration.ofMinutes(10);

    /** 商店接口客户端 */
    private final SteamStoreClient steamStoreClient;

    /** appid 到缓存条目的映射 */
    private final Map<Integer, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 读取商店信息，命中缓存时直接返回。
     *
     * @param appid Steam AppID，可空
     * @return 商店信息；拿不到时返回 null（失败结果也会短暂缓存）
     */
    @Override
    public StoreGameVO get(Integer appid) {
        if (appid == null || appid <= 0) {
            return null;
        }
        Instant now = Instant.now();
        CacheEntry cached = cache.get(appid);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.data();
        }
        StoreGameVO data = steamStoreClient.fetch(appid);
        // 成功缓存 24 小时，失败只缓存 10 分钟，等网络恢复后能较快重试
        Duration ttl = data == null ? FAILURE_TTL : SUCCESS_TTL;
        cache.put(appid, new CacheEntry(data, now.plus(ttl)));
        return data;
    }

    /**
     * 缓存条目。
     *
     * @param data     商店信息，可空（null 表示上次拉取失败）
     * @param expiresAt 过期时间，非空
     */
    private record CacheEntry(StoreGameVO data, Instant expiresAt) {
    }
}
