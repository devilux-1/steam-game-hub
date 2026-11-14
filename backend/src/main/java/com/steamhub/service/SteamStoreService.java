package com.steamhub.service;

import com.steamhub.entity.VO.StoreGameVO;

/**
 * Steam 商店信息缓存业务接口。
 */
public interface SteamStoreService {

    /**
     * 读取商店信息，命中缓存时直接返回。
     *
     * @param appid Steam AppID，可空
     * @return 商店信息；拿不到时返回 null（失败结果也会短暂缓存）
     */
    StoreGameVO get(Integer appid);
}
