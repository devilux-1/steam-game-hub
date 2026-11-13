// 游戏相关接口：搜索、详情、按类型取资源列表与资源点击计数。
import http from './http'

/**
 * 把后端分页结构归一化为统一的 { items, total }。
 * 请求参数：result —— 后端分页 data，可能是数组、{ list, total }、{ content, totalElements } 等结构
 * 返回参数：{ items, total }；无法识别结构时返回空列表与 0
 */
function normalizePage(result) {
  if (Array.isArray(result)) {
    return { items: result, total: result.length }
  }
  if (!result || typeof result !== 'object') {
    return { items: [], total: 0 }
  }
  const items = result.items || result.list || result.content || result.records || []
  const total = Number(result.total ?? result.totalElements ?? result.count ?? items.length) || 0
  return { items, total }
}

/**
 * 按关键词分页搜索游戏。
 * 请求参数：params —— 可选对象；keyword 搜索词（空串表示不过滤）、page 页码（从 1 开始，默认 1）、size 每页条数（默认 12）
 * 返回参数：Promise，解析为 { items, total }；接口异常时由 axios 拦截器抛出
 */
export function searchGames(params = {}) {
  const { keyword = '', page = 1, size = 12 } = params
  return http.get('/api/games', { params: { keyword, page, size } }).then(normalizePage)
}

/**
 * 查询单个游戏详情。
 * 请求参数：id —— 游戏 ID，数字或数字字符串，非空
 * 返回参数：Promise，解析为游戏对象（含 aliases 别名数组）；游戏不存在时后端返回 404
 */
export function getGame(id) {
  return http.get(`/api/games/${id}`)
}

/**
 * 查询指定游戏某类资源列表。
 * 请求参数：id —— 游戏 ID；type —— 资源类型，NEXUS_MOD / HANZI_PATCH / TRAINER，省略表示全部
 * 返回参数：Promise，解析为 { items, total }；接口异常时由 axios 拦截器抛出
 */
export function getGameResources(id, type = '') {
  const params = type ? { type } : {}
  return http.get(`/api/games/${id}/resources`, { params }).then(normalizePage)
}

/**
 * 上报一次资源点击，由后端累加点击量。
 * 请求参数：id —— 资源 ID，数字或数字字符串，非空
 * 返回参数：Promise，解析为 null；失败时抛出，由调用方决定是否继续跳转外链
 */
export function clickResource(id) {
  // silent：点击计数失败不弹错误提示，避免打断「打开来源站」的主流程。
  return http.post(`/api/resources/${id}/click`, null, { silent: true })
}
