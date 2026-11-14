// 用户登录与游戏库接口：会话由后端写入的 httpOnly Cookie 承载，前端不保存令牌。
import http from './http'

/** Steam 登录入口地址，直接整页跳转（后端会 302 到 Steam） */
export const STEAM_LOGIN_URL = '/api/auth/steam/login'

/**
 * 查询当前登录用户。
 * 请求参数：无
 * 返回参数：Promise，解析为用户对象；未登录时解析为 null
 */
export function getMe() {
  return http.get('/api/me', { silent: true })
}

/**
 * 退出登录。
 * 请求参数：无
 * 返回参数：Promise，成功后会话 Cookie 被清除
 */
export function logout() {
  return http.post('/api/auth/logout', null, { silent: true })
}

/**
 * 同步 Steam 游戏库。
 * 请求参数：无
 * 返回参数：Promise，解析为库统计对象（游戏数、总时长、近两周时长、匹配数）
 */
export function syncSteamLibrary() {
  return http.post('/api/sync/steam', null, { timeout: 60000 })
}

/**
 * 查询我的游戏库（统计 + 列表）。
 * 请求参数：params —— { sort, filter }，均可空；sort 取 playtime/name，filter 取 unplayed
 * 返回参数：Promise，解析为 { summary, items }
 */
export function getMyLibrary(params) {
  return http.get('/api/me/library', { params })
}

/**
 * 查询库内某款游戏的详情（站内信息 + Steam 商店信息）。
 * 请求参数：appid —— Steam AppID，数字或数字字符串，非空
 * 返回参数：Promise，解析为 { appid, title, playtimeForever, ..., coverUrl, store }；不在库中时后端返回 404
 */
export function getLibraryItem(appid) {
  // silent：未登录 / 不在库中都属于页面自己要展示的状态，不弹全局提示
  return http.get(`/api/me/library/${appid}`, { silent: true })
}

/**
 * 清空我的游戏库数据。
 * 请求参数：无
 * 返回参数：Promise，成功即库已被清空
 */
export function clearSteamLibrary() {
  return http.delete('/api/sync/steam')
}
