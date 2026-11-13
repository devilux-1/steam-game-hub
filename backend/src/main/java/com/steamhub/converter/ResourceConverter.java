package com.steamhub.converter;

import com.steamhub.entity.PO.Resource;
import com.steamhub.entity.VO.AdminResourceVO;
import com.steamhub.entity.VO.ResourceVO;

/**
 * 资源 PO 与 VO 的转换工具。
 */
public final class ResourceConverter {

    /**
     * 工具类不允许实例化。
     */
    private ResourceConverter() {
    }

    /**
     * 把资源 PO 转换为展示用 VO，过滤掉上下架状态、排序权重等后台字段。
     *
     * @param po 资源 PO，非空
     * @return 资源 VO；po 为 null 时返回 null
     */
    public static ResourceVO toVO(Resource po) {
        if (po == null) {
            return null;
        }
        ResourceVO vo = new ResourceVO();
        vo.setId(po.getId());
        vo.setType(po.getType());
        vo.setTitle(po.getTitle());
        vo.setVersion(po.getVersion());
        vo.setCompatibleVersion(po.getCompatibleVersion());
        vo.setAuthor(po.getAuthor());
        vo.setSourceSite(po.getSourceSite());
        vo.setSourceUrl(po.getSourceUrl());
        vo.setDescription(po.getDescription());
        vo.setInstallNote(po.getInstallNote());
        vo.setClickCount(po.getClickCount() == null ? 0L : po.getClickCount());
        vo.setUpdatedAt(po.getUpdatedAt());
        return vo;
    }

    /**
     * 把资源 PO 转换为后台管理用 VO，额外带上游戏 ID、排序权重与上下架状态。
     *
     * @param po 资源 PO，非空
     * @return 后台资源 VO；po 为 null 时返回 null
     */
    public static AdminResourceVO toAdminVO(Resource po) {
        if (po == null) {
            return null;
        }
        AdminResourceVO vo = new AdminResourceVO();
        // 复用前台字段的搬运逻辑，避免两处字段维护不一致
        ResourceVO base = toVO(po);
        vo.setId(base.getId());
        vo.setType(base.getType());
        vo.setTitle(base.getTitle());
        vo.setVersion(base.getVersion());
        vo.setCompatibleVersion(base.getCompatibleVersion());
        vo.setAuthor(base.getAuthor());
        vo.setSourceSite(base.getSourceSite());
        vo.setSourceUrl(base.getSourceUrl());
        vo.setDescription(base.getDescription());
        vo.setInstallNote(base.getInstallNote());
        vo.setClickCount(base.getClickCount());
        vo.setUpdatedAt(base.getUpdatedAt());
        vo.setGameId(po.getGameId());
        vo.setSortWeight(po.getSortWeight() == null ? 0 : po.getSortWeight());
        vo.setStatus(po.getStatus() == null ? 1 : po.getStatus());
        return vo;
    }
}
