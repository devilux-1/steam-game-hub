// 后台占位登录态：阶段 0 只在前端记录标记，阶段 1 换成后端签发的登录凭据。
// 令牌仅代表「已通过占位登录」，不携带任何权限信息。
const ADMIN_TOKEN_KEY = 'sgh-admin-token'

/**
 * 判断本地是否存在后台占位令牌。
 * 请求参数：无
 * 返回参数：布尔值，存在则 true；浏览器禁用 localStorage 时按未登录处理
 */
export function hasAdminToken() {
  try {
    return window.localStorage.getItem(ADMIN_TOKEN_KEY) === '1'
  } catch {
    return false
  }
}

/**
 * 写入后台占位令牌，用于演示路由守卫。
 * 请求参数：无
 * 返回参数：无；存储不可用时静默失败，后续导航仍会被视为未登录
 */
export function saveAdminToken() {
  try {
    window.localStorage.setItem(ADMIN_TOKEN_KEY, '1')
  } catch {
    // 隐私模式等场景下 localStorage 不可写，放弃占位登录即可，不阻断页面渲染。
  }
}

/**
 * 清除后台占位令牌。
 * 请求参数：无
 * 返回参数：无
 */
export function clearAdminToken() {
  try {
    window.localStorage.removeItem(ADMIN_TOKEN_KEY)
  } catch {
    // 清理失败不影响当前页面，令牌会在下次可写时被覆盖或由守卫忽略。
  }
}
