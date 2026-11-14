package com.steamhub.service;

import com.steamhub.common.BizException;
import com.steamhub.entity.VO.NexusModVO;

import java.util.List;

/**
 * Nexus 精选 Mod 查询业务接口：只读本地缓存，不直接请求 Nexus。
 */
public interface NexusModService {

    /**
     * 查询某个游戏的 Nexus 精选 Mod，按点赞数倒序。
     *
     * @param gameId 游戏 ID，非空
     * @param limit  返回条数上限，取值范围 1-50，超出按 50 处理
     * @return Mod 列表，无缓存数据时返回空列表
     * @throws BizException 游戏不存在时（错误码 404）
     */
    List<NexusModVO> listByGame(Long gameId, int limit);
}
