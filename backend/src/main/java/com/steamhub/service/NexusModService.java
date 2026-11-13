package com.steamhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.steamhub.common.BizException;
import com.steamhub.common.ErrorCode;
import com.steamhub.entity.PO.Game;
import com.steamhub.entity.PO.NexusModCache;
import com.steamhub.entity.VO.NexusModVO;
import com.steamhub.mapper.GameMapper;
import com.steamhub.mapper.NexusModCacheMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Nexus 精选 Mod 查询服务：只读本地缓存，不直接请求 Nexus，保证页面响应速度。
 */
@Service
@RequiredArgsConstructor
public class NexusModService {

    /** 单次返回的最大条数，避免前端一次拉取过多 */
    private static final int MAX_LIMIT = 50;

    /** 游戏数据访问接口，用于校验游戏存在 */
    private final GameMapper gameMapper;

    /** Nexus 缓存数据访问接口 */
    private final NexusModCacheMapper nexusModCacheMapper;

    /**
     * 查询某个游戏的 Nexus 精选 Mod，按点赞数倒序。
     *
     * @param gameId 游戏 ID，非空
     * @param limit  返回条数上限，取值范围 1-50，超出按 50 处理
     * @return Mod 列表，无缓存数据时返回空列表
     * @throws BizException 游戏不存在时（错误码 404）
     */
    public List<NexusModVO> listByGame(Long gameId, int limit) {
        Game game = gameMapper.selectById(gameId);
        if (game == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "游戏不存在");
        }
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<NexusModCache> rows = nexusModCacheMapper.selectList(new LambdaQueryWrapper<NexusModCache>()
                .eq(NexusModCache::getGameId, gameId)
                .orderByDesc(NexusModCache::getEndorsements)
                .orderByDesc(NexusModCache::getDownloads)
                .last("LIMIT " + safeLimit));
        return rows.stream().map(this::toVO).toList();
    }

    /**
     * 把缓存 PO 转换为展示用 VO。
     *
     * @param po 缓存 PO，非空
     * @return 展示 VO
     */
    private NexusModVO toVO(NexusModCache po) {
        NexusModVO vo = new NexusModVO();
        vo.setModId(po.getModId());
        vo.setName(po.getName());
        vo.setNameCn(po.getNameCn());
        vo.setAuthor(po.getAuthor());
        vo.setEndorsements(po.getEndorsements() == null ? 0 : po.getEndorsements());
        vo.setDownloads(po.getDownloads() == null ? 0L : po.getDownloads());
        vo.setSummary(po.getSummary());
        vo.setSummaryCn(po.getSummaryCn());
        vo.setModUrl(po.getModUrl());
        vo.setSyncedAt(po.getSyncedAt());
        return vo;
    }
}
