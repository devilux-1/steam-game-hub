package com.steamhub.service;

/**
 * Mod 汉化业务接口：只补缺失字段，已有译文不会重复送翻。
 */
public interface ModTranslationService {

    /**
     * 翻译某个游戏下尚未汉化的 Mod 条目（标题与简介分别判断）。
     *
     * @param gameId     站内游戏 ID，非空
     * @param gameNameCn 游戏中文名，用于让模型译准游戏名，可空
     * @return 本次写入译文的条目数；未配置 Key、演示模式或无待翻译数据时返回 0
     */
    int translateGame(Long gameId, String gameNameCn);
}
