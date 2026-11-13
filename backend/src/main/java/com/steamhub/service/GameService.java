package com.steamhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.steamhub.common.BizException;
import com.steamhub.common.ErrorCode;
import com.steamhub.common.PageResult;
import com.steamhub.converter.GameConverter;
import com.steamhub.entity.DTO.GameQueryDTO;
import com.steamhub.entity.DTO.GameSaveDTO;
import com.steamhub.entity.PO.Game;
import com.steamhub.entity.PO.GameAlias;
import com.steamhub.entity.PO.Resource;
import com.steamhub.entity.VO.GameDetailVO;
import com.steamhub.entity.VO.GameOptionVO;
import com.steamhub.entity.VO.GameVO;
import com.steamhub.mapper.GameAliasMapper;
import com.steamhub.mapper.GameMapper;
import com.steamhub.mapper.ResourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 游戏业务层：负责前台搜索/详情与后台增删改，PO、VO 的组装集中在此。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    /** 别名列表允许的最大条数，防止单次提交写入过多数据 */
    private static final int MAX_ALIAS_COUNT = 20;

    /** 资源类型对应的资源表状态值：上架 */
    private static final int RESOURCE_ONLINE = 1;

    /** 游戏数据访问接口 */
    private final GameMapper gameMapper;

    /** 游戏别名数据访问接口 */
    private final GameAliasMapper gameAliasMapper;

    /** 资源数据访问接口，用于统计资源数与级联删除 */
    private final ResourceMapper resourceMapper;

    /**
     * 前台分页搜索游戏，支持中文名、英文名与别名匹配，结果按上架资源数倒序。
     *
     * @param query 查询条件，非空；keyword 可空，page 从 1 开始，size 上限由 DTO 约束
     * @return 分页结果，空页时 list 为空列表
     */
    public PageResult<GameVO> search(GameQueryDTO query) {
        return queryByHeat(query, true);
    }

    /**
     * 后台分页搜索游戏，统计口径包含已下架资源。
     *
     * @param query 查询条件，非空；keyword 可空
     * @return 分页结果
     */
    public PageResult<GameVO> adminSearch(GameQueryDTO query) {
        return queryByHeat(query, false);
    }

    /**
     * 查询游戏详情，含别名列表。
     *
     * @param id 游戏 ID，非空
     * @return 游戏详情；游戏不存在时抛出业务异常
     * @throws BizException 游戏不存在（错误码 404）
     */
    public GameDetailVO detail(Long id) {
        Game po = gameMapper.selectById(id);
        if (po == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "游戏不存在");
        }
        long resourceCount = countResources(List.of(id), true).getOrDefault(id, 0L);
        List<String> aliases = gameAliasMapper.selectAliasesByGameId(id);
        return GameConverter.toDetailVO(po, resourceCount, aliases);
    }

    /**
     * 查询全部游戏的下拉选项，供后台资源管理选择所属游戏。
     *
     * @return 选项列表，按 id 倒序；无数据时返回空列表
     */
    public List<GameOptionVO> listOptions() {
        List<Game> games = gameMapper.selectList(new LambdaQueryWrapper<Game>().orderByDesc(Game::getId));
        List<GameOptionVO> options = new ArrayList<>(games.size());
        for (Game po : games) {
            GameOptionVO vo = new GameOptionVO();
            vo.setId(po.getId());
            vo.setNameCn(po.getNameCn());
            vo.setNameEn(po.getNameEn());
            options.add(vo);
        }
        return options;
    }

    /**
     * 后台新增游戏，并在同一事务内写入别名。
     *
     * @param dto 游戏入参，非空；id 字段忽略
     * @return 新增游戏的 ID
     * @throws BizException Steam AppID 已被其它游戏占用时（错误码 409）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(GameSaveDTO dto) {
        validateSteamAppid(dto.getSteamAppid(), null);
        Game po = new Game();
        copyEditableFields(dto, po);
        gameMapper.insert(po);
        replaceAliases(po.getId(), dto.getAliases());
        log.info("新增游戏成功: id={}, nameEn={}", po.getId(), po.getNameEn());
        return po.getId();
    }

    /**
     * 后台编辑游戏，path 中的 ID 优先于入参 ID，别名按全量覆盖处理。
     *
     * @param id  游戏 ID，非空
     * @param dto 游戏入参，非空
     * @throws BizException 游戏不存在（404）或 Steam AppID 冲突（409）
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GameSaveDTO dto) {
        Game exists = gameMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "游戏不存在");
        }
        validateSteamAppid(dto.getSteamAppid(), id);
        Game po = new Game();
        po.setId(id);
        copyEditableFields(dto, po);
        gameMapper.updateById(po);
        replaceAliases(id, dto.getAliases());
        log.info("编辑游戏成功: id={}", id);
    }

    /**
     * 后台删除游戏，同时清理其别名与资源，避免留下孤儿数据。
     *
     * @param id 游戏 ID，非空
     * @throws BizException 游戏不存在时（错误码 404）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Game exists = gameMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "游戏不存在");
        }
        gameAliasMapper.delete(new LambdaQueryWrapper<GameAlias>().eq(GameAlias::getGameId, id));
        resourceMapper.delete(new LambdaQueryWrapper<Resource>().eq(Resource::getGameId, id));
        gameMapper.deleteById(id);
        log.info("删除游戏成功: id={}", id);
    }

    /**
     * 按热度分页查询的公共实现。
     *
     * @param query      查询条件，非空
     * @param onlyOnline 是否只统计上架资源
     * @return 分页结果
     */
    private PageResult<GameVO> queryByHeat(GameQueryDTO query, boolean onlyOnline) {
        String keyword = normalize(query.getKeyword());
        // 别名命中的游戏先统一查出来，避免在主查询里做 join 或循环查询
        List<Long> aliasGameIds = keyword == null ? List.of() : gameAliasMapper.selectGameIdsByAliasLike(keyword);

        Page<Game> page = new Page<>(query.getPage(), query.getSize());
        IPage<Game> pageResult = gameMapper.selectPageByHeat(page, keyword, aliasGameIds, onlyOnline);

        List<Game> records = pageResult.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(query.getPage(), query.getSize());
        }
        List<Long> gameIds = records.stream().map(Game::getId).toList();
        Map<Long, Long> countMap = countResources(gameIds, onlyOnline);

        List<GameVO> list = new ArrayList<>(records.size());
        for (Game po : records) {
            list.add(GameConverter.toVO(po, countMap.get(po.getId())));
        }
        return PageResult.of(list, pageResult.getTotal(), query.getPage(), query.getSize());
    }

    /**
     * 批量统计指定游戏的资源数，一次查询完成，避免逐条 count 造成 N+1。
     *
     * @param gameIds    游戏 ID 列表，非空
     * @param onlyOnline 是否只统计 status=1 的上架资源
     * @return 游戏 ID 到资源数的映射；无资源的游戏不在 map 中
     */
    private Map<Long, Long> countResources(List<Long> gameIds, boolean onlyOnline) {
        if (gameIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<Resource> wrapper = new QueryWrapper<>();
        wrapper.select("game_id", "COUNT(*) AS cnt");
        if (onlyOnline) {
            wrapper.eq("status", RESOURCE_ONLINE);
        }
        wrapper.in("game_id", gameIds).groupBy("game_id");

        List<Map<String, Object>> rows = resourceMapper.selectMaps(wrapper);
        Map<Long, Long> countMap = new HashMap<>(rows.size());
        for (Map<String, Object> row : rows) {
            Object gameId = row.get("game_id");
            Object cnt = row.get("cnt");
            if (gameId instanceof Number idValue && cnt instanceof Number countValue) {
                countMap.put(idValue.longValue(), countValue.longValue());
            }
        }
        return countMap;
    }

    /**
     * 全量替换某游戏的别名：先删旧再插新，保证别名列表与提交内容一致。
     *
     * @param gameId  游戏 ID，非空
     * @param aliases 别名列表，可空；为空时仅执行删除
     * @throws BizException 别名条数超过上限时（错误码 400）
     */
    private void replaceAliases(Long gameId, List<String> aliases) {
        gameAliasMapper.delete(new LambdaQueryWrapper<GameAlias>().eq(GameAlias::getGameId, gameId));
        if (aliases == null || aliases.isEmpty()) {
            return;
        }
        // 去空、去重，避免同一别名重复写入影响搜索结果
        Set<String> distinct = new LinkedHashSet<>();
        for (String alias : aliases) {
            String trimmed = normalize(alias);
            if (trimmed != null) {
                distinct.add(trimmed);
            }
        }
        if (distinct.size() > MAX_ALIAS_COUNT) {
            throw new BizException(ErrorCode.PARAM_INVALID, "别名不能超过 " + MAX_ALIAS_COUNT + " 个");
        }
        for (String alias : distinct) {
            GameAlias po = new GameAlias();
            po.setGameId(gameId);
            po.setAlias(alias);
            gameAliasMapper.insert(po);
        }
    }

    /**
     * 校验 Steam AppID 未被其它游戏占用；为 null 时跳过校验。
     *
     * @param steamAppid 待校验的 Steam AppID，可空
     * @param excludeId  编辑场景下需要排除自身的游戏 ID，新增时为 null
     * @throws BizException AppID 已被占用时（错误码 409）
     */
    private void validateSteamAppid(Integer steamAppid, Long excludeId) {
        if (steamAppid == null) {
            return;
        }
        Long count = gameMapper.selectCount(new LambdaQueryWrapper<Game>()
                .eq(Game::getSteamAppid, steamAppid)
                .ne(excludeId != null, Game::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.DATA_CONFLICT, "该 Steam AppID 已被其它游戏占用");
        }
    }

    /**
     * 把入参中的可编辑字段拷贝到 PO。
     *
     * @param dto 游戏入参，非空
     * @param po  目标 PO，非空
     */
    private void copyEditableFields(GameSaveDTO dto, Game po) {
        po.setNameCn(normalize(dto.getNameCn()));
        po.setNameEn(normalize(dto.getNameEn()));
        po.setSteamAppid(dto.getSteamAppid());
        po.setNexusDomain(normalize(dto.getNexusDomain()));
        po.setCoverUrl(normalize(dto.getCoverUrl()));
        po.setSummary(normalize(dto.getSummary()));
        po.setReleaseDate(dto.getReleaseDate());
    }

    /**
     * 去除首尾空白，空白串统一转为 null，避免出现空字符串字段。
     *
     * @param value 原始字符串，可空
     * @return 去空白后的字符串；入参为 null 或空白时返回 null
     */
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
