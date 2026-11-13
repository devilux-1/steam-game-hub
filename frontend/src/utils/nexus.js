// Nexus 同步数据的展示工具：归一化后端 NexusModVO，并格式化点赞 / 下载数量。

/**
 * 把后端 NexusModVO 整理成视图可直接使用的固定结构。
 * 请求参数：mod —— 后端 NexusModVO，可为 null / undefined
 * 返回参数：{ modId, name, author, endorsements, downloads, summary, modUrl, syncedAt }；缺失字段为空串或 0
 */
export function normalizeNexusMod(mod) {
  if (!mod || typeof mod !== 'object') {
    return {
      modId: '',
      name: '',
      author: '',
      endorsements: 0,
      downloads: 0,
      summary: '',
      modUrl: '',
      syncedAt: ''
    }
  }
  return {
    modId: mod.modId ?? '',
    name: mod.name ?? '',
    author: mod.author ?? '',
    endorsements: Number(mod.endorsements ?? 0) || 0,
    downloads: Number(mod.downloads ?? 0) || 0,
    summary: mod.summary ?? '',
    modUrl: mod.modUrl ?? '',
    syncedAt: mod.syncedAt ?? ''
  }
}

/**
 * 把点赞 / 下载数格式化为紧凑文本，避免六位数撑坏卡片布局。
 * 请求参数：value —— 非负整数，可为空
 * 返回参数：小于 1 万返回原数字文本；否则保留一位小数并加「万」单位
 */
export function formatNexusCount(value) {
  const count = Number(value) || 0
  if (count < 10000) return String(count)
  return `${(count / 10000).toFixed(1)} 万`
}
