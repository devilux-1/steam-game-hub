// 封面图兜底：图片加载失败时不让浏览器的破图标露出来。
// 两种策略按容器形态区分——卡片/详情页的 img 在独立容器里，隐藏即可；
// 库列表的 img 自己就是容器，改用空态样式类，保持占位块尺寸一致。

/**
 * 隐藏加载失败的图片，露出父容器底色。
 * 参数：event —— img 的 error 事件对象，取其 target
 * 返回参数：无
 */
export function hideBrokenImage(event) {
  const img = event?.target
  if (img) {
    img.style.visibility = 'hidden'
  }
}

/**
 * 给加载失败的封面套上空态样式，保留原有占位块尺寸。
 * 参数：event —— img 的 error 事件对象，取其 target
 * 返回参数：无
 */
export function markBrokenCover(event) {
  const img = event?.target
  if (img) {
    img.classList.add('library-item__cover--empty')
  }
}
