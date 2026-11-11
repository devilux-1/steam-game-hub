// 游戏字段归一化：兼容后端 camelCase 与数据库下划线两种常见返回风格。
// 页面只消费归一化后的字段，避免各视图重复写兼容分支。

/**
 * 把后端游戏对象整理成视图可用的固定结构。
 * 请求参数：game —— 后端返回的游戏对象，可为 null / undefined
 * 返回参数：{ id, nameCn, nameEn, summary, coverUrl, steamAppId }；缺失字段为空串或 null
 */
export function normalizeGame(game) {
  if (!game || typeof game !== 'object') {
    return { id: '', nameCn: '', nameEn: '', summary: '', coverUrl: '', steamAppId: null }
  }
  return {
    id: game.id ?? '',
    nameCn: game.nameCn ?? game.name_cn ?? '',
    nameEn: game.nameEn ?? game.name_en ?? '',
    summary: game.summary ?? '',
    coverUrl: game.coverUrl ?? game.cover_url ?? '',
    steamAppId: game.steamAppid ?? game.steam_appid ?? null
  }
}
