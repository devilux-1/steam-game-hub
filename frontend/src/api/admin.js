// 管理端接口：登录、游戏 CRUD、游戏下拉选项与资源 CRUD。
// 除登录外都需 Authorization: Bearer <token>，请求头由 api/http.js 统一附加。
import http from './http'

/**
 * 归一化后台分页响应。
 * 请求参数：result —— 后端分页 data，约定为 { list, total, page, size }
 * 返回参数：{ items, total, page, size }；结构不符时返回空列表与默认分页
 */
function normalizeAdminPage(result) {
  if (!result || typeof result !== 'object') {
    return { items: [], total: 0, page: 1, size: 20 }
  }
  const items = Array.isArray(result.list) ? result.list : result.items || result.records || []
  return {
    items,
    total: Number(result.total ?? items.length) || 0,
    page: Number(result.page) || 1,
    size: Number(result.size) || items.length || 20
  }
}

/**
 * 管理员登录。
 * 请求参数：payload —— { username 用户名, password 密码 }，均非空字符串
 * 返回参数：Promise，解析为 { token, username }；账号密码错误时由拦截器抛出后端 message
 */
export function adminLogin(payload) {
  return http.post('/api/admin/auth/login', payload)
}

/**
 * 分页查询后台游戏列表。
 * 请求参数：params —— 可选对象；keyword 搜索词（空串不过滤）、page 页码（从 1 开始）、size 每页条数
 * 返回参数：Promise，解析为 { items, total, page, size }；items 为 GameVO 数组
 */
export function listAdminGames(params = {}) {
  const { keyword = '', page = 1, size = 20 } = params
  return http.get('/api/admin/games', { params: { keyword, page, size } }).then(normalizeAdminPage)
}

/**
 * 查询游戏下拉选项，供资源管理选择所属游戏。
 * 请求参数：无
 * 返回参数：Promise，解析为 [{ id, nameCn, nameEn }]；无数据时为空数组
 */
export function getGameOptions() {
  return http.get('/api/admin/games/options')
}

/**
 * 新增游戏。
 * 请求参数：payload —— GameSaveDTO，nameEn 必填，其余字段可空；aliases 为别名字符串数组
 * 返回参数：Promise，解析为新游戏 ID
 */
export function createGame(payload) {
  return http.post('/api/admin/games', payload)
}

/**
 * 编辑游戏。
 * 请求参数：id —— 游戏 ID；payload —— GameSaveDTO，id 以路径为准
 * 返回参数：Promise，解析为 null；接口异常时由拦截器抛出
 */
export function updateGame(id, payload) {
  return http.put(`/api/admin/games/${id}`, payload)
}

/**
 * 删除游戏，同时删除其别名与资源。
 * 请求参数：id —— 游戏 ID
 * 返回参数：Promise，解析为 null；接口异常时由拦截器抛出
 */
export function deleteGame(id) {
  return http.delete(`/api/admin/games/${id}`)
}

/**
 * 分页查询后台资源列表。
 * 请求参数：params —— 可选对象；gameId 所属游戏 ID（空表示全部游戏）、page 页码、size 每页条数
 * 返回参数：Promise，解析为 { items, total, page, size }；items 为 ResourceVO 数组
 */
export function listAdminResources(params = {}) {
  const { gameId = '', page = 1, size = 20 } = params
  // 未选择游戏时不传 gameId，避免空串被后端当作数字解析。
  const query = { page, size }
  if (gameId !== '' && gameId !== null && gameId !== undefined) {
    query.gameId = gameId
  }
  return http.get('/api/admin/resources', { params: query }).then(normalizeAdminPage)
}

/**
 * 新增资源。
 * 请求参数：payload —— ResourceSaveDTO，gameId、type、title、sourceUrl 必填
 * 返回参数：Promise，解析为新资源 ID
 */
export function createResource(payload) {
  return http.post('/api/admin/resources', payload)
}

/**
 * 编辑资源。
 * 请求参数：id —— 资源 ID；payload —— ResourceSaveDTO，id 以路径为准
 * 返回参数：Promise，解析为 null；接口异常时由拦截器抛出
 */
export function updateResource(id, payload) {
  return http.put(`/api/admin/resources/${id}`, payload)
}

/**
 * 删除资源。
 * 请求参数：id —— 资源 ID
 * 返回参数：Promise，解析为 null；接口异常时由拦截器抛出
 */
export function deleteResource(id) {
  return http.delete(`/api/admin/resources/${id}`)
}
