// Axios 实例与统一响应处理：负责解包 { code, message, data }、附加管理端令牌并在 401 时回到登录页。
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getAdminToken, clearAdminSession } from '@/utils/adminAuth'

const http = axios.create({
  // baseURL 留空：开发态由 Vite 代理 /api，生产态由 Nginx 同源反代后端。
  baseURL: '',
  timeout: 8000,
  headers: { 'Content-Type': 'application/json' }
})

/**
 * 判断请求是否为管理端接口；登录接口本身不携带令牌，避免旧令牌让登录请求被鉴权拦截。
 * 请求参数：url —— Axios 请求地址，可能为空
 * 返回参数：布尔值，属于需鉴权的 /api/admin 接口时为 true
 */
function needsAdminToken(url) {
  if (typeof url !== 'string') return false
  return url.startsWith('/api/admin') && !url.startsWith('/api/admin/auth/login')
}

/**
 * 请求拦截成功分支：为管理端接口自动附加 Bearer 令牌。
 * 请求参数：config —— Axios 请求配置，读取 url 与 headers
 * 返回参数：补充 Authorization 头后的请求配置；无令牌时原样返回
 */
function onRequestFulfilled(config) {
  const token = getAdminToken()
  if (token && needsAdminToken(config?.url)) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}

/**
 * 校验并解包后端统一响应体。
 * 请求参数：response —— Axios 响应对象，其 data 约定为 { code, message, data }
 * 返回参数：响应体中的 data 字段；业务码为 0 之外的异常由调用方捕获
 * 异常：响应体不符合约定或 code 非 0 时抛出 Error（message 优先取后端提示）
 */
function unwrapBody(response) {
  const body = response?.data
  if (!body || typeof body.code !== 'number') {
    throw new Error('接口响应格式异常')
  }
  if (body.code !== 0) {
    throw new Error(body.message || '请求失败')
  }
  return body.data
}

/**
 * 清理登录态并跳转管理员登录页。
 * 请求参数：无
 * 返回参数：Promise，动态导入路由后完成跳转；路由不可用时降级为整页跳转
 */
async function redirectToAdminLogin() {
  clearAdminSession()
  try {
    const { default: router } = await import('@/router')
    const current = router.currentRoute.value
    if (current.name === 'admin-login') return
    router.push({ name: 'admin-login', query: { redirect: current.fullPath } })
  } catch {
    // 路由模块尚未就绪时退回整页跳转，保证不会停留在无权限页面上。
    if (window.location.pathname !== '/admin/login') {
      window.location.href = '/admin/login'
    }
  }
}

/**
 * 响应拦截成功分支：解包业务数据，业务失败时统一弹提示并抛出。
 * 请求参数：response —— Axios 响应对象；config.silent 为 true 时不弹提示（如点击计数）
 * 返回参数：Promise / 业务 data；业务码非 0 时返回 rejected Promise
 */
function onResponseFulfilled(response) {
  try {
    return unwrapBody(response)
  } catch (error) {
    if (!response?.config?.silent) {
      ElMessage.error(error.message)
    }
    return Promise.reject(error)
  }
}

/**
 * 响应拦截失败分支：401 清理登录态并跳登录页，其余错误提示后继续抛出供调用方降级。
 * 请求参数：error —— Axios 异常对象，可能包含 response.status、response.data.message 或超时信息；config.silent 为 true 时不弹提示
 * 返回参数：rejected Promise；调用方用 try/catch 或 .catch 决定是否使用兜底数据
 */
function onResponseRejected(error) {
  const status = error?.response?.status
  if (status === 401) {
    ElMessage.error('登录已过期，请重新登录')
    redirectToAdminLogin()
    return Promise.reject(error)
  }
  const backendMessage = error?.response?.data?.message
  const message = backendMessage || (error?.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '服务暂不可用，请检查后端是否已启动')
  if (!error?.config?.silent) {
    ElMessage.error(message)
  }
  return Promise.reject(error)
}

http.interceptors.request.use(onRequestFulfilled)
http.interceptors.response.use(onResponseFulfilled, onResponseRejected)

export default http
