// 路由表：公开页面与后台管理页面；后台访问控制依赖登录接口写入本地的令牌。
// 除首页外的页面全部按需加载，首屏只下载首页所需代码，减小首次打开的包体积。
import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import { hasAdminToken } from '@/utils/adminAuth'

const routes = [
  { path: '/', name: 'home', component: Home, meta: { title: '首页' } },
  {
    path: '/games',
    name: 'game-list',
    component: () => import('@/views/GameList.vue'),
    meta: { title: '全部游戏' }
  },
  {
    path: '/games/:id',
    name: 'game-detail',
    component: () => import('@/views/GameDetail.vue'),
    meta: { title: '游戏条目' }
  },
  {
    path: '/library/:appid',
    name: 'library-detail',
    component: () => import('@/views/LibraryDetail.vue'),
    meta: { title: '我的游戏' }
  },
  {
    path: '/me/library',
    name: 'my-library',
    component: () => import('@/views/MyLibrary.vue'),
    meta: { title: '我的游戏库' }
  },
  {
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('@/views/AdminLogin.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/admin/games',
    name: 'admin-games',
    component: () => import('@/views/AdminGames.vue'),
    meta: { title: '游戏管理', requiresAdmin: true }
  },
  {
    path: '/admin/resources',
    name: 'admin-resources',
    component: () => import('@/views/AdminResources.vue'),
    meta: { title: '资源管理', requiresAdmin: true }
  },
  // 未匹配路径回首页，避免骨架阶段出现空白页。
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 路由切换后回到页面顶部；带锚点的站内跳转不受影响。
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
})

/**
 * 全局前置守卫：更新浏览器标题，并拦截未登录访问的后台页面。
 * 请求参数：to —— 即将进入的目标路由对象，读取 meta.title / meta.requiresAdmin；from —— 来源路由对象（未使用）；next —— 放行或重定向回调
 * 返回参数：无；始终通过 next 结束导航
 */
router.beforeEach((to, from, next) => {
  const pageTitle = to.meta.title
  document.title = pageTitle ? `${pageTitle} · 游戏百宝箱` : '游戏百宝箱'

  // 本地令牌只做前端放行判断，令牌是否有效由管理端接口返回 401 时再校验。
  if (to.meta.requiresAdmin && !hasAdminToken()) {
    next({ name: 'admin-login', query: { redirect: to.fullPath } })
    return
  }
  next()
})

export default router
