// 后台登录态：令牌与用户名存 localStorage，接口请求头由 api/http.js 从这里读取。
// 键名固定为 admin_token / admin_username，401 时由 http.js 调用 clearAdminSession 清理。
const ADMIN_TOKEN_KEY = 'admin_token'
const ADMIN_USERNAME_KEY = 'admin_username'

/**
 * 读取 localStorage 中的字符串值。
 * 请求参数：key —— 存储键名，非空字符串
 * 返回参数：非空字符串；键不存在、值为空或浏览器禁用 localStorage 时返回空串
 */
function readStorage(key) {
  try {
    const value = window.localStorage.getItem(key)
    return typeof value === 'string' ? value : ''
  } catch {
    // 隐私模式等场景下 localStorage 不可读，按未登录处理，不阻断页面渲染。
    return ''
  }
}

/**
 * 写入 localStorage，并忽略存储异常。
 * 请求参数：key —— 存储键名；value —— 待写入的字符串，空串表示删除该键
 * 返回参数：无
 */
function writeStorage(key, value) {
  try {
    if (value) {
      window.localStorage.setItem(key, value)
    } else {
      window.localStorage.removeItem(key)
    }
  } catch {
    // 存储不可用时静默失败，本次会话内的管理请求会由后端 401 分支兜底。
  }
}

/**
 * 读取后台访问令牌。
 * 请求参数：无
 * 返回参数：令牌字符串；未登录或存储不可用时返回空串
 */
export function getAdminToken() {
  return readStorage(ADMIN_TOKEN_KEY)
}

/**
 * 判断本地是否存有后台令牌，供路由守卫拦截未登录访问。
 * 请求参数：无
 * 返回参数：布尔值，存在非空令牌时为 true
 */
export function hasAdminToken() {
  return getAdminToken() !== ''
}

/**
 * 读取当前登录的管理员用户名，用于后台导航展示。
 * 请求参数：无
 * 返回参数：用户名字符串；未登录或未记录时返回空串
 */
export function getAdminUsername() {
  return readStorage(ADMIN_USERNAME_KEY)
}

/**
 * 保存登录成功后返回的令牌与用户名。
 * 请求参数：token —— 后端签发的访问令牌，非空字符串；username —— 登录用户名，可空字符串
 * 返回参数：无；存储不可用时静默失败，后续管理请求会收到 401
 */
export function saveAdminSession(token, username) {
  writeStorage(ADMIN_TOKEN_KEY, typeof token === 'string' ? token : '')
  writeStorage(ADMIN_USERNAME_KEY, typeof username === 'string' ? username : '')
}

/**
 * 清除本地后台登录态。
 * 请求参数：无
 * 返回参数：无；清理失败不影响当前页面，后续请求由 401 分支再次触发跳转
 */
export function clearAdminSession() {
  writeStorage(ADMIN_TOKEN_KEY, '')
  writeStorage(ADMIN_USERNAME_KEY, '')
}
