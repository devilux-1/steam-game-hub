package com.steamhub.service;

import com.steamhub.entity.VO.StatsOverviewVO;

/**
 * 站内统计业务接口。
 */
public interface StatsService {

    /**
     * 统计站内游戏数与资源条目总数。
     *
     * @return 统计概览视图对象；两表均为空时两个计数为 0，不会返回 null
     */
    StatsOverviewVO getOverview();
}
