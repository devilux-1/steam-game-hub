// 日期展示工具：后端可能返回 LocalDate（YYYY-MM-DD）或 LocalDateTime（ISO 字符串）。

/**
 * 补足两位数字文本。
 * 请求参数：num —— 0 到 59 的整数
 * 返回参数：两位数文本，如 9 → 「09」
 */
function pad(num) {
  return String(num).padStart(2, '0')
}

/**
 * 把后端日期字符串解析为 Date 对象。
 * 请求参数：value —— 日期字符串或 Date，可为空；兼容「YYYY-MM-DD HH:mm:ss」的空格分隔写法
 * 返回参数：有效 Date；空值或无法解析时返回 null
 */
function toDate(value) {
  if (!value) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  const normalized = String(value).replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

/**
 * 格式化日期。
 * 请求参数：value —— 日期字符串或 Date，可为空
 * 返回参数：「YYYY-MM-DD」文本；无有效值时返回空串
 */
export function formatDate(value) {
  // 纯日期字符串直接截取，避免 UTC 解析造成跨时区偏移一天。
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}/.test(value)) {
    return value.slice(0, 10)
  }
  const date = toDate(value)
  if (!date) return ''
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/**
 * 格式化日期时间。
 * 请求参数：value —— 日期时间字符串或 Date，可为空
 * 返回参数：「YYYY-MM-DD HH:mm」文本；无有效值时返回空串
 */
export function formatDateTime(value) {
  const date = toDate(value)
  if (!date) return ''
  return `${formatDate(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/**
 * 把分钟数格式化为易读的时长文本。
 * 请求参数：minutes —— 分钟数，可为空或负数
 * 返回参数：如「120 小时」「35 分钟」；0 或无效值返回「0 分钟」
 */
export function formatMinutes(minutes) {
  const value = Math.max(Number(minutes) || 0, 0)
  if (value < 60) return `${value} 分钟`
  const hours = value / 60
  // 不足 10 小时保留一位小数，超过后取整，避免长文本撑坏统计卡
  const text = hours < 10 ? hours.toFixed(1) : String(Math.round(hours))
  return `${text} 小时`
}
