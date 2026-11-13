package com.steamhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.steamhub.common.BizException;
import com.steamhub.common.ErrorCode;
import com.steamhub.common.PageResult;
import com.steamhub.converter.ResourceConverter;
import com.steamhub.entity.DTO.ResourceSaveDTO;
import com.steamhub.entity.PO.Game;
import com.steamhub.entity.PO.Resource;
import com.steamhub.entity.VO.AdminResourceVO;
import com.steamhub.entity.VO.ResourceVO;
import com.steamhub.mapper.GameMapper;
import com.steamhub.mapper.ResourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 资源业务层：负责前台资源展示、点击计数与后台增删改。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    /** 资源类型白名单，与设计文档的三类资源一致 */
    private static final Set<String> VALID_TYPES = Set.of("NEXUS_MOD", "HANZI_PATCH", "TRAINER");

    /** 上架状态值 */
    private static final int STATUS_ONLINE = 1;

    /** 资源数据访问接口 */
    private final ResourceMapper resourceMapper;

    /** 游戏数据访问接口，用于校验资源所属游戏是否存在 */
    private final GameMapper gameMapper;

    /**
     * 查询某个游戏下的上架资源。
     *
     * @param gameId 游戏 ID，非空
     * @param type   资源类型，可空；为空时返回该游戏全部类型
     * @return 资源列表，按排序权重倒序、id 倒序；无数据时返回空列表
     * @throws BizException 类型不在白名单内时（错误码 400）
     */
    public List<ResourceVO> listByGame(Long gameId, String type) {
        String normalizedType = normalizeType(type);
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<Resource>()
                .eq(Resource::getGameId, gameId)
                .eq(Resource::getStatus, STATUS_ONLINE)
                .eq(normalizedType != null, Resource::getType, normalizedType)
                .orderByDesc(Resource::getSortWeight)
                .orderByDesc(Resource::getId);
        return resourceMapper.selectList(wrapper).stream().map(ResourceConverter::toVO).toList();
    }

    /**
     * 记录一次资源点击，计数在数据库层原子自增。
     *
     * @param id 资源 ID，非空
     * @throws BizException 资源不存在时（错误码 404）
     */
    public void increaseClick(Long id) {
        int affected = resourceMapper.increaseClick(id);
        if (affected == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "资源不存在");
        }
    }

    /**
     * 后台分页查询资源，包含已下架数据。
     * 返回后台专用 VO，带游戏 ID、排序权重与上下架状态，供编辑弹窗完整回填。
     *
     * @param gameId 游戏 ID，可空；为空时查询全部游戏的资源
     * @param page   页码，从 1 开始
     * @param size   每页条数，1-100
     * @return 分页结果
     */
    public PageResult<AdminResourceVO> adminList(Long gameId, int page, int size) {
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<Resource>()
                .eq(gameId != null, Resource::getGameId, gameId)
                .orderByDesc(Resource::getId);
        IPage<Resource> pageResult = resourceMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminResourceVO> list = pageResult.getRecords().stream().map(ResourceConverter::toAdminVO).toList();
        return PageResult.of(list, pageResult.getTotal(), page, size);
    }

    /**
     * 后台新增资源。
     *
     * @param dto 资源入参，非空；id 字段忽略
     * @return 新增资源的 ID
     * @throws BizException 所属游戏不存在（404）或类型非法（400）
     */
    public Long create(ResourceSaveDTO dto) {
        validateGameExists(dto.getGameId());
        validateType(dto.getType());
        Resource po = new Resource();
        copyEditableFields(dto, po);
        resourceMapper.insert(po);
        log.info("新增资源成功: id={}, gameId={}, type={}", po.getId(), po.getGameId(), po.getType());
        return po.getId();
    }

    /**
     * 后台编辑资源，path 中的 ID 优先于入参 ID。
     * 采用增量更新：入参中为 null 的字段保留库中原值，避免后台表单未展示的字段（排序权重、上下架状态）被重置。
     *
     * @param id  资源 ID，非空
     * @param dto 资源入参，非空
     * @throws BizException 资源不存在（404）、所属游戏不存在（404）或类型非法（400）
     */
    public void update(Long id, ResourceSaveDTO dto) {
        Resource po = resourceMapper.selectById(id);
        if (po == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "资源不存在");
        }
        validateGameExists(dto.getGameId());
        validateType(dto.getType());
        applyEditableFields(dto, po);
        resourceMapper.updateById(po);
        log.info("编辑资源成功: id={}", id);
    }

    /**
     * 后台删除资源。
     *
     * @param id 资源 ID，非空
     * @throws BizException 资源不存在时（错误码 404）
     */
    public void delete(Long id) {
        Resource exists = resourceMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "资源不存在");
        }
        resourceMapper.deleteById(id);
        log.info("删除资源成功: id={}", id);
    }

    /**
     * 校验资源类型是否在白名单内，空值由 DTO 的 @NotBlank 拦截。
     *
     * @param type 资源类型，非空
     * @throws BizException 类型非法时（错误码 400）
     */
    private void validateType(String type) {
        String normalized = normalizeType(type);
        if (normalized == null || !VALID_TYPES.contains(normalized)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "资源类型不合法，仅支持 NEXUS_MOD / HANZI_PATCH / TRAINER");
        }
    }

    /**
     * 校验资源所属游戏存在。
     *
     * @param gameId 游戏 ID，非空
     * @throws BizException 游戏不存在时（错误码 404）
     */
    private void validateGameExists(Long gameId) {
        Game game = gameMapper.selectById(gameId);
        if (game == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "所属游戏不存在");
        }
    }

    /**
     * 归一化资源类型：去空白并转大写，非法值原样返回交由白名单校验处理。
     *
     * @param type 原始类型，可空
     * @return 归一化后的类型；入参为 null 或空白时返回 null
     */
    private String normalizeType(String type) {
        if (type == null) {
            return null;
        }
        String trimmed = type.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    /**
     * 新增场景：把入参写入 PO，并为排序权重、上下架状态补默认值。
     *
     * @param dto 资源入参，非空
     * @param po  新建的 PO，非空
     */
    private void copyEditableFields(ResourceSaveDTO dto, Resource po) {
        applyEditableFields(dto, po);
        // 新增时这两个字段允许不传，落库前补默认值以满足非空约束
        if (po.getSortWeight() == null) {
            po.setSortWeight(0);
        }
        if (po.getStatus() == null) {
            po.setStatus(STATUS_ONLINE);
        }
    }

    /**
     * 编辑场景：只覆盖入参中非 null 的字段，为 null 的保留库中原值。
     * 后台表单未展示排序权重与上下架状态时，不会因此把已下架资源重新上架或清零权重。
     *
     * @param dto 资源入参，非空
     * @param po  已从库中查出的 PO，非空
     */
    private void applyEditableFields(ResourceSaveDTO dto, Resource po) {
        if (dto.getGameId() != null) {
            po.setGameId(dto.getGameId());
        }
        if (dto.getType() != null) {
            po.setType(normalizeType(dto.getType()));
        }
        if (dto.getTitle() != null) {
            po.setTitle(trim(dto.getTitle()));
        }
        if (dto.getVersion() != null) {
            po.setVersion(trim(dto.getVersion()));
        }
        if (dto.getCompatibleVersion() != null) {
            po.setCompatibleVersion(trim(dto.getCompatibleVersion()));
        }
        if (dto.getAuthor() != null) {
            po.setAuthor(trim(dto.getAuthor()));
        }
        if (dto.getSourceSite() != null) {
            po.setSourceSite(trim(dto.getSourceSite()));
        }
        if (dto.getSourceUrl() != null) {
            po.setSourceUrl(trim(dto.getSourceUrl()));
        }
        if (dto.getDescription() != null) {
            po.setDescription(trim(dto.getDescription()));
        }
        if (dto.getInstallNote() != null) {
            po.setInstallNote(trim(dto.getInstallNote()));
        }
        if (dto.getSortWeight() != null) {
            po.setSortWeight(dto.getSortWeight());
        }
        if (dto.getStatus() != null) {
            po.setStatus(dto.getStatus());
        }
    }

    /**
     * 去除首尾空白，空白串统一转为 null。
     *
     * @param value 原始字符串，可空
     * @return 去空白后的字符串；入参为 null 或空白时返回 null
     */
    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
