package com.steamhub.converter;

import com.steamhub.entity.PO.Resource;
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
}
