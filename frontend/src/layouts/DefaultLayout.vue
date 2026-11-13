<!-- 站点默认布局：顶部导航、页面出口与站点页脚。 -->
<template>
  <div class="app-shell">
    <header class="site-header">
      <div class="site-header__inner">
        <router-link class="brand" to="/" aria-label="返回首页">
          <span class="brand__mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M3.6 9.6h16.8v8.6a1.8 1.8 0 0 1-1.8 1.8H5.4a1.8 1.8 0 0 1-1.8-1.8V9.6Z"
                stroke="currentColor"
                stroke-width="1.5"
              />
              <path
                d="M2.6 6a1.8 1.8 0 0 1 1.8-1.8h15.2A1.8 1.8 0 0 1 21.4 6v3.6H2.6V6Z"
                stroke="currentColor"
                stroke-width="1.5"
              />
              <path d="M10 13.2h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
          </span>
          <span class="brand__text">
            <strong>游戏百宝箱</strong>
            <em>STEAM GAME HUB</em>
          </span>
        </router-link>

        <form class="header-search" role="search" @submit.prevent="submitSearch">
          <el-input
            v-model="searchKeyword"
            class="header-search__input"
            placeholder="搜索游戏中文名 / 英文名 / 别名"
            clearable
            aria-label="搜索游戏"
          >
            <template #prefix>
              <svg class="header-search__icon" viewBox="0 0 20 20" fill="none" aria-hidden="true">
                <!-- deslop-ignore-next-line 24: 放大镜是标准两笔图标，非手绘装饰 -->
                <circle cx="8.8" cy="8.8" r="5.6" stroke="currentColor" stroke-width="1.5" />
                <path d="m13.2 13.2 3.4 3.4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
              </svg>
            </template>
          </el-input>
          <button type="submit" class="header-search__submit">搜索</button>
        </form>

        <div class="header-actions">
          <!-- 未登录：直接跳转 Steam 授权 -->
          <button v-if="!userStore.isLogin" type="button" class="btn btn--primary header-sync" @click="goLogin">
            <span>Steam 登录</span>
          </button>

          <template v-else>
            <button type="button" class="btn btn--ghost header-sync" :disabled="syncing" @click="onSyncClick">
              <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                <path d="M16.2 8.2A6.4 6.4 0 1 0 16 12.4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                <path
                  d="M16.6 4.6v3.9h-3.9"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
              <span>{{ syncing ? '同步中…' : '同步游戏库' }}</span>
            </button>

            <el-dropdown trigger="click" @command="onUserCommand">
              <button type="button" class="header-user" aria-label="账号菜单">
                <img v-if="userStore.user?.avatarUrl" class="header-user__avatar" :src="userStore.user.avatarUrl" alt="" />
                <span class="header-user__name">{{ userStore.displayName }}</span>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="library">我的游戏库</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </div>
    </header>

    <main class="site-main">
      <router-view />
    </main>

    <footer class="site-footer">
      <p>游戏百宝箱 · 仅聚合外部资源链接，本站不托管任何游戏文件</p>
    </footer>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { STEAM_LOGIN_URL, syncSteamLibrary } from '@/api/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const searchKeyword = ref('')
/** 是否正在同步游戏库，用于禁用按钮防重复点击 */
const syncing = ref(false)

/**
 * 顶部搜索提交：把关键词写入首页地址栏，由首页监听后执行实际查询。
 * 请求参数：无（读取 searchKeyword 的当前值）
 * 返回参数：无
 */
function submitSearch() {
  const keyword = searchKeyword.value.trim()
  router.push({ name: 'home', query: keyword ? { keyword } : {} })
}

/**
 * 跳转 Steam 登录：整页跳转到后端登录入口，由后端 302 到 Steam 授权页。
 * 请求参数：无
 * 返回参数：无
 */
function goLogin() {
  window.location.href = STEAM_LOGIN_URL
}

/**
 * 「同步游戏库」：未登录先去登录，已登录则调同步接口并通知当前页面刷新。
 * 请求参数：无
 * 返回参数：无；失败时由 axios 拦截器提示
 */
async function onSyncClick() {
  if (!userStore.isLogin) {
    goLogin()
    return
  }
  if (syncing.value) return
  syncing.value = true
  try {
    const summary = await syncSteamLibrary()
    ElMessage.success(`同步完成，共 ${summary.totalGames} 款游戏`)
    // 我的游戏库页面监听该事件后重新拉取列表
    window.dispatchEvent(new CustomEvent('library:sync'))
  } catch {
    // 拦截器已提示具体原因（未配置 Key、隐私设置、冷却中）
  } finally {
    syncing.value = false
  }
}

/**
 * 账号下拉菜单动作。
 * 请求参数：command —— library 表示去我的游戏库，logout 表示退出登录
 * 返回参数：无
 */
async function onUserCommand(command) {
  if (command === 'library') {
    router.push({ name: 'my-library' })
    return
  }
  if (command === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    if (route.name === 'my-library') {
      router.push({ name: 'home' })
    }
  }
}

/**
 * 把地址栏里的搜索词回填到导航搜索框，保证刷新或前进后退时输入框同步。
 * 请求参数：keyword —— 路由 query.keyword，非字符串时按空串处理
 * 返回参数：无
 */
function syncKeyword(keyword) {
  searchKeyword.value = typeof keyword === 'string' ? keyword : ''
}

watch(() => route.query.keyword, syncKeyword, { immediate: true })

// 布局只在应用启动时挂载一次，用户信息在这里拉取避免每个页面重复请求
onMounted(() => {
  if (!userStore.loaded) {
    userStore.fetchMe()
  }
})
</script>

