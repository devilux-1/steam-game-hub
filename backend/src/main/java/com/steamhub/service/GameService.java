package com.steamhub.service;

import com.steamhub.common.BizException;
import com.steamhub.common.PageResult;
import com.steamhub.entity.DTO.GameQueryDTO;
import com.steamhub.entity.DTO.GameSaveDTO;
import com.steamhub.entity.VO.GameDetailVO;
import com.steamhub.entity.VO.GameOptionVO;
import com.steamhub.entity.VO.GameVO;

import java.util.List;

/**
 * 游戏业务接口：前台搜索、详情查询与后台增删改。
 */
public interface GameService {

    /**
     * 前台分页搜索游戏，支持中文名、英文名与别名匹配，结果按上架资源数倒序。
     *
     * @param query 查询条件，非空；keyword 可空，page 从 1 开始，size 上限由 DTO 约束
     * @return 分页结果，空页时 list 为空列表
     */
    PageResult<GameVO> search(GameQueryDTO query);

    /**
     * 后台分页搜索游戏，统计口径包含已下架资源。
     *
     * @param query 查询条件，非空；keyword 可空
     * @return 分页结果
     */
    PageResult<GameVO> adminSearch(GameQueryDTO query);

    /**
     * 查询游戏详情，含别名列表。
     *
     * @param id 游戏 ID，非空
     * @return 游戏详情
     * @throws BizException 游戏不存在时（错误码 404）
     */
    GameDetailVO detail(Long id);

    /**
     * 查询全部游戏的下拉选项，供后台资源管理选择所属游戏。
     *
     * @return 选项列表，按 id 倒序；无数据时返回空列表
     */
    List<GameOptionVO> listOptions();

    /**
     * 后台新增游戏，并在同一事务内写入别名。
     *
     * @param dto 游戏入参，非空；id 字段忽略
     * @return 新增游戏的 ID
     * @throws BizException Steam AppID 已被其它游戏占用时（错误码 409）
     */
    Long create(GameSaveDTO dto);

    /**
     * 后台编辑游戏，path 中的 ID 优先于入参 ID，别名按全量覆盖处理。
     *
     * @param id  游戏 ID，非空
     * @param dto 游戏入参，非空
     * @throws BizException 游戏不存在（404）或 Steam AppID 冲突（409）
     */
    void update(Long id, GameSaveDTO dto);

    /**
     * 后台删除游戏，同时清理其别名与资源，避免留下孤儿数据。
     *
     * @param id 游戏 ID，非空
     * @throws BizException 游戏不存在时（错误码 404）
     */
    void delete(Long id);
}
