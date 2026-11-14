package com.steamhub.service;

import com.steamhub.common.BizException;
import com.steamhub.entity.PO.AppUser;
import com.steamhub.entity.VO.LibraryDetailVO;
import com.steamhub.entity.VO.LibraryItemVO;
import com.steamhub.entity.VO.LibrarySummaryVO;

import java.util.List;

/**
 * Steam 游戏库业务接口：同步 Steam 拥有游戏到本地表、匹配站内条目，并提供统计与列表查询。
 */
public interface SteamLibraryService {

    /**
     * 同步当前用户的 Steam 游戏库：先清空旧数据再整体写入，保证与 Steam 侧一致。
     *
     * @param user 当前登录用户，非空
     * @return 同步后的库统计
     * @throws BizException 未配置 API Key、同步过于频繁或 Steam 未返回数据时
     */
    LibrarySummaryVO sync(AppUser user);

    /**
     * 清空当前用户的游戏库数据。
     *
     * @param userId 用户主键，非空
     */
    void clear(Long userId);

    /**
     * 查询当前用户的库统计。
     *
     * @param userId 用户主键，非空
     * @return 统计视图；无数据时各项为 0，同步时间为 null
     */
    LibrarySummaryVO summary(Long userId);

    /**
     * 查询当前用户的游戏库列表。
     *
     * @param userId 用户主键，非空
     * @param sort   排序方式：playtime（默认，按时长倒序）或 name（按名称升序）
     * @param filter 筛选条件：unplayed 表示只看从未游玩，其它值不筛选
     * @return 列表；无数据时返回空列表
     */
    List<LibraryItemVO> list(Long userId, String sort, String filter);

    /**
     * 查询库内某款游戏的详情：库记录 + 站内条目匹配 + Steam 商店信息。
     *
     * @param userId 用户主键，非空
     * @param appid  Steam AppID，非空
     * @return 详情对象；Steam 商店信息拉不到时 store 字段为 null
     * @throws BizException 该 AppID 不在当前用户库中时（错误码 404）
     */
    LibraryDetailVO detail(Long userId, Integer appid);
}
