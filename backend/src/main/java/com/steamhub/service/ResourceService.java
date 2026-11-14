package com.steamhub.service;

import com.steamhub.common.BizException;
import com.steamhub.common.PageResult;
import com.steamhub.entity.DTO.ResourceSaveDTO;
import com.steamhub.entity.VO.AdminResourceVO;
import com.steamhub.entity.VO.ResourceVO;

import java.util.List;

/**
 * 资源业务接口：前台资源展示、点击计数与后台增删改。
 */
public interface ResourceService {

    /**
     * 查询某个游戏下的上架资源。
     *
     * @param gameId 游戏 ID，非空
     * @param type   资源类型，可空；为空时返回该游戏全部类型
     * @return 资源列表，按排序权重倒序、id 倒序；无数据时返回空列表
     * @throws BizException 类型不在白名单内时（错误码 400）
     */
    List<ResourceVO> listByGame(Long gameId, String type);

    /**
     * 记录一次资源点击，计数在数据库层原子自增。
     *
     * @param id 资源 ID，非空
     * @throws BizException 资源不存在时（错误码 404）
     */
    void increaseClick(Long id);

    /**
     * 后台分页查询资源，包含已下架数据。
     *
     * @param gameId 游戏 ID，可空；为空时查询全部游戏的资源
     * @param page   页码，从 1 开始
     * @param size   每页条数，1-100
     * @return 分页结果
     */
    PageResult<AdminResourceVO> adminList(Long gameId, int page, int size);

    /**
     * 后台新增资源。
     *
     * @param dto 资源入参，非空；id 字段忽略
     * @return 新增资源的 ID
     * @throws BizException 所属游戏不存在（404）或类型非法（400）
     */
    Long create(ResourceSaveDTO dto);

    /**
     * 后台编辑资源，path 中的 ID 优先于入参 ID，入参为 null 的字段保留库中原值。
     *
     * @param id  资源 ID，非空
     * @param dto 资源入参，非空
     * @throws BizException 资源不存在（404）、所属游戏不存在（404）或类型非法（400）
     */
    void update(Long id, ResourceSaveDTO dto);

    /**
     * 后台删除资源。
     *
     * @param id 资源 ID，非空
     * @throws BizException 资源不存在时（错误码 404）
     */
    void delete(Long id);
}
