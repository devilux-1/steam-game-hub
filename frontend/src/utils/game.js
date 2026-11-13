// 游戏字段归一化：兼容后端 camelCase 与数据库下划线两种常见返回风格。
// 页面只消费归一化后的字段，避免各视图重复写兼容分支。

/**
 * 把后端游戏对象整理成视图可用的固定结构。
 * 请求参数：game —— 后端返回的游戏对象，可为 null / undefined
 * 返回参数：{ id, nameCn, nameEn, summary, coverUrl, steamAppId, resourceCount, releaseDate, aliases }；缺失字段为空串、0 或空数组
 */
export function normalizeGame(game) {
  if (!game || typeof game !== 'object') {
    return {
      id: '',
      nameCn: '',
      nameEn: '',
      summary: '',
      coverUrl: '',
      steamAppId: null,
      resourceCount: 0,
      releaseDate: '',
      aliases: []
    }
  }
  return {
    id: game.id ?? '',
    nameCn: game.nameCn ?? game.name_cn ?? '',
    nameEn: game.nameEn ?? game.name_en ?? '',
    summary: game.summary ?? '',
    coverUrl: game.coverUrl ?? game.cover_url ?? '',
    steamAppId: game.steamAppid ?? game.steam_appid ?? null,
    // 列表接口未返回资源数时按 0 展示，避免卡片出现 NaN。
    resourceCount: Number(game.resourceCount ?? game.resource_count ?? 0) || 0,
    releaseDate: game.releaseDate ?? game.release_date ?? '',
    aliases: Array.isArray(game.aliases) ? game.aliases.filter((alias) => typeof alias === 'string' && alias) : []
  }
}
