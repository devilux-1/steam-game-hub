package com.steamhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.steamhub.client.SteamClient;
import com.steamhub.common.BizException;
import com.steamhub.common.ErrorCode;
import com.steamhub.config.SteamProperties;
import com.steamhub.entity.DTO.SteamOwnedGameDTO;
import com.steamhub.entity.PO.AppUser;
import com.steamhub.entity.PO.Game;
import com.steamhub.entity.PO.UserLibrary;
import com.steamhub.entity.VO.LibraryDetailVO;
import com.steamhub.entity.VO.LibraryItemVO;
import com.steamhub.entity.VO.LibrarySummaryVO;
import com.steamhub.mapper.GameMapper;
import com.steamhub.mapper.UserLibraryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Steam 游戏库服务：同步 Steam 拥有游戏到本地表、匹配站内条目，并提供统计与列表查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SteamLibraryService {

    /** 列表排序：按时长倒序（默认） */
    private static final String SORT_PLAYTIME = "playtime";

    /** 列表排序：按名称升序 */
    private static final String SORT_NAME = "name";

    /** 筛选条件：只看从未游玩 */
    private static final String FILTER_UNPLAYED = "unplayed";

    /** Steam 头图地址前缀，未收录进站内条目的库内游戏用它补封面 */
    private static final String STEAM_HEADER_BASE = "https://cdn.cloudflare.steamstatic.com/steam/apps/";

    /** Steam 配置 */
    private final SteamProperties properties;

    /** Steam 接口客户端 */
    private final SteamClient steamClient;

    /** 用户游戏库数据访问接口 */
    private final UserLibraryMapper userLibraryMapper;

    /** 游戏表数据访问接口，用于按 AppID 匹配站内条目 */
    private final GameMapper gameMapper;

    /** Steam 商店信息缓存服务，用于补全未收录游戏的简介与截图 */
    private final SteamStoreService steamStoreService;

    /** 各用户最近一次成功同步时间，仅用于进程内限流，重启后重置 */
    private final Map<Long, Instant> lastSyncAt = new ConcurrentHashMap<>();

    /**
     * 同步当前用户的 Steam 游戏库：先清空旧数据再整体写入，保证与 Steam 侧一致。
     *
     * @param user 当前登录用户，非空
     * @return 同步后的库统计
     * @throws BizException 未配置 API Key、同步过于频繁或 Steam 未返回数据时
     */
    @Transactional
    public LibrarySummaryVO sync(AppUser user) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BizException(ErrorCode.BIZ_FAIL, "服务端未配置 Steam API Key，暂时无法同步游戏库");
        }
        checkCooldown(user.getId());
        lastSyncAt.put(user.getId(), Instant.now());
        List<SteamOwnedGameDTO> games;
        try {
            games = steamClient.fetchOwnedGames(user.getSteamId());
        } catch (RuntimeException e) {
            // 拉取失败不占用冷却时间，允许用户立刻重试
            lastSyncAt.remove(user.getId());
            throw e;
        }
        if (games.isEmpty()) {
            lastSyncAt.remove(user.getId());
            throw new BizException(ErrorCode.BIZ_FAIL,
                    "Steam 未返回游戏数据，请把「隐私设置 → 游戏详情」设为公开后再试");
        }

        Map<Integer, Game> matched = matchGames(games);
        LocalDateTime now = LocalDateTime.now();
        userLibraryMapper.delete(new LambdaQueryWrapper<UserLibrary>()
                .eq(UserLibrary::getUserId, user.getId()));
        for (SteamOwnedGameDTO dto : games) {
            if (dto.getAppid() == null) {
                continue;
            }
            UserLibrary row = new UserLibrary();
            row.setUserId(user.getId());
            row.setAppid(dto.getAppid());
            row.setTitle(dto.getName() == null || dto.getName().isBlank()
                    ? "App " + dto.getAppid() : dto.getName());
            row.setPlaytimeForever(dto.getPlaytimeForever() == null ? 0 : dto.getPlaytimeForever());
            row.setPlaytime2weeks(dto.getPlaytime2weeks() == null ? 0 : dto.getPlaytime2weeks());
            row.setLastPlayedAt(dto.getLastPlayedAt());
            Game game = matched.get(dto.getAppid());
            row.setGameId(game == null ? null : game.getId());
            row.setSyncedAt(now);
            userLibraryMapper.insert(row);
        }
        log.info("Steam 库同步完成 userId={} 游戏 {} 条，匹配站内 {} 条", user.getId(), games.size(), matched.size());
        return summary(user.getId());
    }

    /**
     * 清空当前用户的游戏库数据。
     *
     * @param userId 用户主键，非空
     */
    public void clear(Long userId) {
        userLibraryMapper.delete(new LambdaQueryWrapper<UserLibrary>()
                .eq(UserLibrary::getUserId, userId));
        lastSyncAt.remove(userId);
    }

    /**
     * 查询当前用户的库统计。
     *
     * @param userId 用户主键，非空
     * @return 统计视图；无数据时各项为 0，同步时间为 null
     */
    public LibrarySummaryVO summary(Long userId) {
        List<UserLibrary> rows = userLibraryMapper.selectList(new LambdaQueryWrapper<UserLibrary>()
                .eq(UserLibrary::getUserId, userId));
        LibrarySummaryVO vo = new LibrarySummaryVO();
        vo.setTotalGames(rows.size());
        vo.setTotalMinutes(rows.stream()
                .map(UserLibrary::getPlaytimeForever)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum());
        vo.setTwoWeeksMinutes(rows.stream()
                .map(UserLibrary::getPlaytime2weeks)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum());
        vo.setMatchedGames((int) rows.stream().filter(row -> row.getGameId() != null).count());
        vo.setLastSyncedAt(rows.stream()
                .map(UserLibrary::getSyncedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        return vo;
    }

    /**
     * 查询当前用户的游戏库列表。
     *
     * @param userId 用户主键，非空
     * @param sort   排序方式：playtime（默认，按时长倒序）或 name（按名称升序）
     * @param filter 筛选条件：unplayed 表示只看从未游玩，其它值不筛选
     * @return 列表；无数据时返回空列表
     */
    public List<LibraryItemVO> list(Long userId, String sort, String filter) {
        LambdaQueryWrapper<UserLibrary> wrapper = new LambdaQueryWrapper<UserLibrary>()
                .eq(UserLibrary::getUserId, userId);
        if (FILTER_UNPLAYED.equalsIgnoreCase(filter)) {
            wrapper.eq(UserLibrary::getPlaytimeForever, 0);
        }
        if (SORT_NAME.equalsIgnoreCase(sort)) {
            wrapper.orderByAsc(UserLibrary::getTitle);
        } else {
            wrapper.orderByDesc(UserLibrary::getPlaytimeForever).orderByAsc(UserLibrary::getTitle);
        }
        List<UserLibrary> rows = userLibraryMapper.selectList(wrapper);
        Map<Long, Game> gameMap = loadGames(rows);
        return rows.stream().map(row -> toVO(row, gameMap)).toList();
    }

    /**
     * 查询库内某款游戏的详情：库记录 + 站内条目匹配 + Steam 商店信息。
     *
     * @param userId 用户主键，非空
     * @param appid  Steam AppID，非空
     * @return 详情对象；Steam 商店信息拉不到时 store 字段为 null
     * @throws BizException 该 AppID 不在当前用户库中时（错误码 404）
     */
    public LibraryDetailVO detail(Long userId, Integer appid) {
        UserLibrary row = userLibraryMapper.selectOne(new LambdaQueryWrapper<UserLibrary>()
                .eq(UserLibrary::getUserId, userId)
                .eq(UserLibrary::getAppid, appid)
                .last("limit 1"));
        if (row == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "你的游戏库里没有这款游戏，请先同步 Steam 库");
        }
        Game game = row.getGameId() == null ? null : gameMapper.selectById(row.getGameId());
        LibraryDetailVO vo = new LibraryDetailVO();
        vo.setAppid(row.getAppid());
        vo.setTitle(row.getTitle());
        vo.setPlaytimeForever(row.getPlaytimeForever() == null ? 0 : row.getPlaytimeForever());
        vo.setPlaytime2weeks(row.getPlaytime2weeks() == null ? 0 : row.getPlaytime2weeks());
        vo.setLastPlayedAt(row.getLastPlayedAt());
        vo.setCoverUrl(resolveCover(row.getAppid(), game));
        if (game != null) {
            vo.setGameId(game.getId());
            vo.setGameNameCn(game.getNameCn());
        }
        vo.setStore(steamStoreService.get(appid));
        return vo;
    }

    /**
     * 检查同步冷却，未到间隔直接拒绝。
     *
     * @param userId 用户主键，非空
     * @throws BizException 距离上次成功同步不足配置间隔时（错误码 1000）
     */
    private void checkCooldown(Long userId) {
        long cooldown = Math.max(properties.getSyncCooldownSeconds(), 0L);
        if (cooldown == 0) {
            return;
        }
        Instant last = lastSyncAt.get(userId);
        if (last == null) {
            return;
        }
        long elapsed = Duration.between(last, Instant.now()).getSeconds();
        if (elapsed < cooldown) {
            throw new BizException(ErrorCode.BIZ_FAIL, "同步过于频繁，请 " + (cooldown - elapsed) + " 秒后再试");
        }
    }

    /**
     * 按 Steam AppID 批量匹配站内游戏。
     *
     * @param games Steam 库数据，非空
     * @return AppID 到站内游戏的映射；没有匹配时返回空 Map
     */
    private Map<Integer, Game> matchGames(List<SteamOwnedGameDTO> games) {
        List<Integer> appids = games.stream()
                .map(SteamOwnedGameDTO::getAppid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (appids.isEmpty()) {
            return Map.of();
        }
        List<Game> matched = gameMapper.selectList(new LambdaQueryWrapper<Game>()
                .in(Game::getSteamAppid, appids));
        Map<Integer, Game> map = new HashMap<>(matched.size());
        for (Game game : matched) {
            map.put(game.getSteamAppid(), game);
        }
        return map;
    }

    /**
     * 批量读取库记录对应的站内游戏。
     *
     * @param rows 库记录列表，非空
     * @return 站内游戏 ID 到游戏的映射；没有匹配时返回空 Map
     */
    private Map<Long, Game> loadGames(List<UserLibrary> rows) {
        Set<Long> ids = rows.stream()
                .map(UserLibrary::getGameId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return gameMapper.selectList(new LambdaQueryWrapper<Game>().in(Game::getId, ids)).stream()
                .collect(Collectors.toMap(Game::getId, game -> game, (a, b) -> a));
    }

    /**
     * 把库记录与站内游戏合并为展示对象。
     *
     * @param row     库记录，非空
     * @param gameMap 站内游戏映射，可空
     * @return 展示对象
     */
    private LibraryItemVO toVO(UserLibrary row, Map<Long, Game> gameMap) {
        LibraryItemVO vo = new LibraryItemVO();
        vo.setAppid(row.getAppid());
        vo.setTitle(row.getTitle());
        vo.setPlaytimeForever(row.getPlaytimeForever() == null ? 0 : row.getPlaytimeForever());
        vo.setPlaytime2weeks(row.getPlaytime2weeks() == null ? 0 : row.getPlaytime2weeks());
        vo.setLastPlayedAt(row.getLastPlayedAt());
        vo.setGameId(row.getGameId());
        Game game = row.getGameId() == null ? null : gameMap.get(row.getGameId());
        if (game != null) {
            vo.setGameNameCn(game.getNameCn());
        }
        // 未收录进站内条目的游戏同样要有封面，统一回落到 Steam 头图
        vo.setCoverUrl(resolveCover(row.getAppid(), game));
        return vo;
    }

    /**
     * 解析库内游戏的封面地址：站内封面优先，没有则回落到 Steam 头图。
     *
     * @param appid Steam AppID，可空
     * @param game  匹配到的站内游戏，可空
     * @return 封面地址；appid 非法且无站内封面时返回 null
     */
    private String resolveCover(Integer appid, Game game) {
        if (game != null && game.getCoverUrl() != null && !game.getCoverUrl().isBlank()) {
            return game.getCoverUrl();
        }
        return appid == null || appid <= 0 ? null : STEAM_HEADER_BASE + appid + "/header.jpg";
    }
}
