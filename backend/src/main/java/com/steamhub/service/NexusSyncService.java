package com.steamhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.steamhub.client.NexusApiException;
import com.steamhub.client.NexusClient;
import com.steamhub.config.NexusProperties;
import com.steamhub.entity.DTO.NexusModDTO;
import com.steamhub.entity.PO.Game;
import com.steamhub.entity.PO.NexusModCache;
import com.steamhub.entity.VO.NexusSyncStatusVO;
import com.steamhub.mapper.GameMapper;
import com.steamhub.mapper.NexusModCacheMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Nexus 数据同步服务：按游戏拉取热门 Mod 并写入本地缓存，供条目页直接读取。
 * 同步为单线程异步执行，避免与定时任务并发；同一时刻只允许一个任务在跑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NexusSyncService {

    /** 游戏数据访问接口，用于取出配置了 nexus_domain 的游戏 */
    private final GameMapper gameMapper;

    /** Nexus 缓存数据访问接口 */
    private final NexusModCacheMapper nexusModCacheMapper;

    /** Nexus 配置 */
    private final NexusProperties properties;

    /** Nexus 接口客户端 */
    private final NexusClient nexusClient;

    /** 是否已有任务在执行，用作并发开关 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 最近一次同步的结果快照，服务重启后重置 */
    private volatile NexusSyncStatusVO lastStatus = new NexusSyncStatusVO();

    /** 单线程执行器：串行执行同步，避免多任务同时打满接口配额 */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "nexus-sync");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 异步触发一次全量同步。
     *
     * @return true 表示已开始执行；false 表示已有任务在跑，本次未触发
     */
    public boolean triggerAsync() {
        if (!running.compareAndSet(false, true)) {
            log.info("Nexus 同步已在进行中，忽略本次触发");
            return false;
        }
        executor.submit(() -> {
            try {
                doSync();
            } catch (Exception e) {
                // 兜底：任何未预料异常都要释放开关，否则后续永远无法再触发
                log.error("Nexus 同步异常终止", e);
                lastStatus.setRunning(false);
                lastStatus.setLastEndAt(LocalDateTime.now());
                lastStatus.setMessage("同步异常终止：" + e.getMessage());
            } finally {
                running.set(false);
            }
        });
        return true;
    }

    /**
     * 查询当前同步状态快照。
     *
     * @return 状态视图对象，从末次同步结果复制而来；从未同步时各计数为 0
     */
    public NexusSyncStatusVO getStatus() {
        NexusSyncStatusVO status = new NexusSyncStatusVO();
        NexusSyncStatusVO snapshot = lastStatus;
        status.setRunning(running.get());
        status.setMockMode(properties.isDemoMode());
        status.setLastStartAt(snapshot.getLastStartAt());
        status.setLastEndAt(snapshot.getLastEndAt());
        status.setTotalGames(snapshot.getTotalGames());
        status.setSuccessGames(snapshot.getSuccessGames());
        status.setFailedGames(snapshot.getFailedGames());
        status.setModsSaved(snapshot.getModsSaved());
        status.setMessage(snapshot.getMessage());
        return status;
    }

    /**
     * 执行一次全量同步：遍历配置了 nexus_domain 的游戏，逐个拉取热门 Mod 并落库。
     * 单个游戏失败不影响其它游戏，失败原因汇总到状态对象的 message 中。
     */
    private void doSync() {
        NexusSyncStatusVO status = new NexusSyncStatusVO();
        status.setRunning(true);
        status.setMockMode(properties.isDemoMode());
        status.setLastStartAt(LocalDateTime.now());
        lastStatus = status;

        List<Game> games = gameMapper.selectList(new LambdaQueryWrapper<Game>()
                .isNotNull(Game::getNexusDomain)
                .ne(Game::getNexusDomain, "")
                .orderByAsc(Game::getId));
        status.setTotalGames(games.size());

        if (games.isEmpty()) {
            status.setLastEndAt(LocalDateTime.now());
            status.setMessage("没有配置 Nexus 标识的游戏，跳过同步");
            log.info("Nexus 同步跳过：没有配置 nexus_domain 的游戏");
            return;
        }

        List<String> errors = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int saved = 0;
        for (Game game : games) {
            try {
                saved += syncOneGame(game.getNexusDomain(), game.getId());
                success++;
            } catch (NexusApiException e) {
                failed++;
                errors.add(e.getMessage());
                log.warn("Nexus 同步失败 gameId={} domain={} 原因={}", game.getId(), game.getNexusDomain(), e.getMessage());
            } catch (Exception e) {
                failed++;
                errors.add("domain=" + game.getNexusDomain() + " 未知错误：" + e.getMessage());
                log.error("Nexus 同步出现未预期异常 gameId={}", game.getId(), e);
            }
            sleepBetweenRequests();
        }

        status.setSuccessGames(success);
        status.setFailedGames(failed);
        status.setModsSaved(saved);
        status.setLastEndAt(LocalDateTime.now());
        // 只保留前三条错误，避免状态接口返回过长文本
        status.setMessage(errors.isEmpty() ? null : String.join("；", errors.subList(0, Math.min(3, errors.size()))));
        log.info("Nexus 同步结束：游戏 {} 个，成功 {}，失败 {}，写入 {} 条", games.size(), success, failed, saved);
    }

    /**
     * 同步单个游戏的 Mod 缓存。
     *
     * @param domain 游戏的 Nexus 标识，非空
     * @param gameId 站内游戏 ID，非空
     * @return 写入或更新的条数
     * @throws NexusApiException 拉取失败时
     */
    private int syncOneGame(String domain, Long gameId) {
        List<NexusModDTO> mods = nexusClient.fetchTrendingMods(domain, properties.getMaxModsPerGame());
        if (mods.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        Set<Integer> fetchedIds = new HashSet<>(mods.size());
        int saved = 0;
        for (NexusModDTO dto : mods) {
            fetchedIds.add(dto.getModId());
            NexusModCache existing = nexusModCacheMapper.selectOne(new LambdaQueryWrapper<NexusModCache>()
                    .eq(NexusModCache::getGameId, gameId)
                    .eq(NexusModCache::getModId, dto.getModId()));
            if (existing == null) {
                nexusModCacheMapper.insert(toPO(gameId, dto, now));
            } else {
                NexusModCache update = toPO(gameId, dto, now);
                update.setId(existing.getId());
                nexusModCacheMapper.updateById(update);
            }
            saved++;
        }
        // 清理本次未出现的旧记录，保证页面展示与 Nexus 当前热门一致
        nexusModCacheMapper.delete(new LambdaQueryWrapper<NexusModCache>()
                .eq(NexusModCache::getGameId, gameId)
                .notIn(NexusModCache::getModId, fetchedIds));
        return saved;
    }

    /**
     * 把接口返回的 Mod 数据转换为缓存表 PO。
     *
     * @param gameId 站内游戏 ID，非空
     * @param dto    Nexus Mod 数据，非空
     * @param now    本次同步时间，非空
     * @return 待落库的 PO
     */
    private NexusModCache toPO(Long gameId, NexusModDTO dto, LocalDateTime now) {
        NexusModCache po = new NexusModCache();
        po.setGameId(gameId);
        po.setModId(dto.getModId());
        po.setName(dto.getName() == null ? ("Mod " + dto.getModId()) : dto.getName());
        po.setAuthor(dto.getAuthor());
        po.setEndorsements(dto.getEndorsements() == null ? 0 : dto.getEndorsements());
        po.setDownloads(dto.getDownloads() == null ? 0L : dto.getDownloads());
        po.setSummary(dto.getSummary());
        po.setModUrl(dto.getModUrl());
        po.setSyncedAt(now);
        return po;
    }

    /**
     * 按配置间隔休眠，降低触发 Nexus 限流的概率。
     */
    private void sleepBetweenRequests() {
        long interval = properties.getRequestIntervalMs();
        if (interval <= 0) {
            return;
        }
        try {
            Thread.sleep(interval);
        } catch (InterruptedException e) {
            // 被中断时恢复标记，让任务尽快结束
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 应用关闭时释放同步线程池。
     */
    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
