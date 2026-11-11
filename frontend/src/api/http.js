// Axios 实例与统一响应处理：负责解包 { code, message, data } 并在失败时提示。
import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  // baseURL 留空：开发态由 Vite 代理 /api，生产态由 Nginx 同源反代后端。
  baseURL: '',
  timeout: 8000,
  headers: { 'Content-Type': 'application/json' }
})

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
 * 响应拦截成功分支：解包业务数据，业务失败时统一弹提示并抛出。
 * 请求参数：response —— Axios 响应对象
 * 返回参数：Promise / 业务 data；业务码非 0 时返回 rejected Promise
 */
function onResponseFulfilled(response) {
  try {
    return unwrapBody(response)
  } catch (error) {
    ElMessage.error(error.message)
    return Promise.reject(error)
  }
}

/**
 * 响应拦截失败分支：处理网络错误与 HTTP 异常，提示后继续抛出供调用方降级。
 * 请求参数：error —— Axios 异常对象，可能包含 response.data.message 或超时信息
 * 返回参数：rejected Promise；调用方用 try/catch 或 .catch 决定是否使用兜底数据
 */
function onResponseRejected(error) {
  const backendMessage = error?.response?.data?.message
  const message = backendMessage || (error?.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '服务暂不可用，请检查后端是否已启动')
  ElMessage.error(message)
  return Promise.reject(error)
}

http.interceptors.response.use(onResponseFulfilled, onResponseRejected)

export default http
