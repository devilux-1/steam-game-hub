package com.steamhub.converter;

import com.steamhub.entity.PO.Game;
import com.steamhub.entity.VO.GameDetailVO;
import com.steamhub.entity.VO.GameVO;

import java.util.List;

/**
 * 游戏 PO 与 VO 的转换工具。
 * 转换只做字段搬运，不查库、不做业务判断，便于在 Service 中集中编排。
 */
public final class GameConverter {

    /**
     * 工具类不允许实例化。
     */
    private GameConverter() {
    }

    /**
     * 把游戏 PO 转换为列表展示用的 VO。
     *
     * @param po           游戏 PO，非空
     * @param resourceCount 该游戏的上架资源数，可为 null，null 时按 0 处理
     * @return 游戏 VO，不含别名
     */
    public static GameVO toVO(Game po, Long resourceCount) {
        GameVO vo = new GameVO();
        fillBase(vo, po);
        vo.setResourceCount(resourceCount == null ? 0L : resourceCount);
        return vo;
    }

    /**
     * 把游戏 PO 转换为详情 VO。
     *
     * @param po           游戏 PO，非空
     * @param resourceCount 该游戏的资源数，可为 null，null 时按 0 处理
     * @param aliases      别名列表，可为 null，null 时返回空列表
     * @return 游戏详情 VO
     */
    public static GameDetailVO toDetailVO(Game po, Long resourceCount, List<String> aliases) {
        GameDetailVO vo = new GameDetailVO();
        fillBase(vo, po);
        vo.setResourceCount(resourceCount == null ? 0L : resourceCount);
        vo.setAliases(aliases == null ? List.of() : List.copyOf(aliases));
        return vo;
    }

    /**
     * 拷贝游戏 PO 的基础字段到 VO，列表与详情共用。
     *
     * @param vo 目标 VO，非空
     * @param po 游戏 PO，非空
     */
    private static void fillBase(GameVO vo, Game po) {
        vo.setId(po.getId());
        vo.setNameCn(po.getNameCn());
        vo.setNameEn(po.getNameEn());
        vo.setSteamAppid(po.getSteamAppid());
        vo.setCoverUrl(po.getCoverUrl());
        vo.setSummary(po.getSummary());
        vo.setReleaseDate(po.getReleaseDate());
    }
}
