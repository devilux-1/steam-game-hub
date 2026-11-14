package com.steamhub.service;

import com.steamhub.entity.VO.NexusSyncStatusVO;

/**
 * Nexus 数据同步业务接口。
 */
public interface NexusSyncService {

    /**
     * 异步触发一次全量同步。
     *
     * @return true 表示已开始执行；false 表示已有任务在跑，本次未触发
     */
    boolean triggerAsync();

    /**
     * 查询当前同步状态快照。
     *
     * @return 状态视图对象，从末次同步结果复制而来；从未同步时各计数为 0
     */
    NexusSyncStatusVO getStatus();
}
