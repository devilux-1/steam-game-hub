// 站点统计接口。
import http from './http'

/**
 * 查询站内概览统计数字。
 * 请求参数：无
 * 返回参数：Promise，解析为统计对象，常见字段为游戏数与资源数；接口异常时由拦截器抛错
 */
export function getStatsOverview() {
  return http.get('/api/stats/overview')
}
