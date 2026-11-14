package com.steamhub.service.impl;

import com.steamhub.entity.VO.StatsOverviewVO;
import com.steamhub.mapper.GameMapper;
import com.steamhub.mapper.ResourceMapper;
import com.steamhub.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 站内统计业务层。
 * Controller 只负责收发统一结果体，统计口径与 VO 组装集中在此，Mapper 只做数据访问。
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    /** 游戏数据访问接口，用于统计游戏总数 */
    private final GameMapper gameMapper;

    /** 资源数据访问接口，用于统计资源总数 */
    private final ResourceMapper resourceMapper;

    /**
     * 统计站内游戏数与资源条目总数。
     *
     * @return 统计概览视图对象；两表均为空时两个计数为 0，不会返回 null
     */
    @Override
    public StatsOverviewVO getOverview() {
        StatsOverviewVO vo = new StatsOverviewVO();
        // selectCount(null) 表示不加 where 条件，统计全表行数
        vo.setGameCount(gameMapper.selectCount(null));
        vo.setResourceCount(resourceMapper.selectCount(null));
        return vo;
    }
}
