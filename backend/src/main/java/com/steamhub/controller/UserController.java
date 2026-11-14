package com.steamhub.controller;

import com.steamhub.common.Result;
import com.steamhub.entity.PO.AppUser;
import com.steamhub.entity.VO.LibraryDetailVO;
import com.steamhub.entity.VO.LibraryItemVO;
import com.steamhub.entity.VO.LibrarySummaryVO;
import com.steamhub.entity.VO.LibraryVO;
import com.steamhub.service.SteamLibraryService;
import com.steamhub.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户游戏库接口：同步 Steam 库、清空库数据、查询统计与列表。
 * 全部接口都要求已登录（Cookie 会话），未登录统一返回 401。
 */
@RestController
@Tag(name = "我的游戏库")
@RequiredArgsConstructor
public class UserController {

    /** 用户会话服务 */
    private final UserAuthService userAuthService;

    /** 游戏库业务层 */
    private final SteamLibraryService steamLibraryService;

    /**
     * 同步当前用户的 Steam 游戏库。
     *
     * @param request 当前请求，用于解析登录态
     * @return 同步后的库统计；未配置 Key、冷却中或 Steam 无数据时返回错误
     */
    @PostMapping("/sync/steam")
    @Operation(summary = "同步 Steam 游戏库")
    public Result<LibrarySummaryVO> sync(HttpServletRequest request) {
        AppUser user = userAuthService.requireUser(request);
        return Result.success(steamLibraryService.sync(user));
    }

    /**
     * 清空当前用户的游戏库数据。
     *
     * @param request 当前请求，用于解析登录态
     * @return 成功结果
     */
    @DeleteMapping("/sync/steam")
    @Operation(summary = "清空我的游戏库")
    public Result<Void> clear(HttpServletRequest request) {
        AppUser user = userAuthService.requireUser(request);
        steamLibraryService.clear(user.getId());
        return Result.success();
    }

    /**
     * 查询当前用户的游戏库（统计 + 列表）。
     *
     * @param sort    排序方式：playtime（默认）或 name，可空
     * @param filter  筛选条件：unplayed 表示只看从未游玩，可空
     * @param request 当前请求，用于解析登录态
     * @return 库统计与列表
     */
    @GetMapping("/me/library")
    @Operation(summary = "我的游戏库")
    public Result<LibraryVO> library(@RequestParam(required = false) String sort,
                                     @RequestParam(required = false) String filter,
                                     HttpServletRequest request) {
        AppUser user = userAuthService.requireUser(request);
        LibraryVO vo = new LibraryVO();
        vo.setSummary(steamLibraryService.summary(user.getId()));
        List<LibraryItemVO> items = steamLibraryService.list(user.getId(), sort, filter);
        vo.setItems(items);
        return Result.success(vo);
    }

    /**
     * 查询库内某款游戏的详情，未收录的站内条目会补充 Steam 商店信息。
     *
     * @param appid   Steam AppID，非空，路径参数
     * @param request 当前请求，用于解析登录态
     * @return 库内详情；该 AppID 不在当前用户库中时返回 404
     */
    @GetMapping("/me/library/{appid}")
    @Operation(summary = "库内游戏详情")
    public Result<LibraryDetailVO> detail(@PathVariable Integer appid, HttpServletRequest request) {
        AppUser user = userAuthService.requireUser(request);
        return Result.success(steamLibraryService.detail(user.getId(), appid));
    }
}
