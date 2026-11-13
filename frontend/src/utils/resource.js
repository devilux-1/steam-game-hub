// 资源类型常量与字段归一化：游戏条目页与后台资源管理共用同一套类型标签。

/** 三类资源，value 与后端 resource.type 枚举一致，顺序即页面 Tab 顺序 */
export const RESOURCE_TYPES = [
  { value: 'NEXUS_MOD', label: 'Nexus Mod' },
  { value: 'HANZI_PATCH', label: '汉化补丁' },
  { value: 'TRAINER', label: '风灵月影' }
]

/**
 * 取资源类型的中文展示名。
 * 请求参数：type —— 资源类型编码，可为空
 * 返回参数：类型标签字符串；未知编码原样返回，空值返回「未分类」
 */
export function resourceTypeLabel(type) {
  const target = RESOURCE_TYPES.find((item) => item.value === type)
  return target ? target.label : type || '未分类'
}

/**
 * 把后端资源对象整理成视图可用的固定结构。
 * 请求参数：resource —— 后端 ResourceVO，可为 null / undefined
 * 返回参数：{ id, type, title, version, compatibleVersion, author, sourceSite, sourceUrl, description, installNote, clickCount, updatedAt }；缺失字段为空串或 0
 */
export function normalizeResource(resource) {
  if (!resource || typeof resource !== 'object') {
    return {
      id: '',
      type: '',
      title: '',
      version: '',
      compatibleVersion: '',
      author: '',
      sourceSite: '',
      sourceUrl: '',
      description: '',
      installNote: '',
      clickCount: 0,
      updatedAt: ''
    }
  }
  return {
    id: resource.id ?? '',
    type: resource.type ?? '',
    title: resource.title ?? '',
    version: resource.version ?? '',
    compatibleVersion: resource.compatibleVersion ?? resource.compatible_version ?? '',
    author: resource.author ?? '',
    sourceSite: resource.sourceSite ?? resource.source_site ?? '',
    sourceUrl: resource.sourceUrl ?? resource.source_url ?? '',
    description: resource.description ?? '',
    installNote: resource.installNote ?? resource.install_note ?? '',
    clickCount: Number(resource.clickCount ?? resource.click_count ?? 0) || 0,
    updatedAt: resource.updatedAt ?? resource.updated_at ?? ''
  }
}
