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
                <circle cx="8.8" cy="8.8" r="5.6" stroke="currentColor" stroke-width="1.5" />
                <path d="m13.2 13.2 3.4 3.4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
              </svg>
            </template>
          </el-input>
          <button type="submit" class="header-search__submit">搜索</button>
        </form>

        <div class="header-actions">
          <button type="button" class="btn btn--ghost header-sync" @click="onSyncClick">
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
            <span>同步游戏库</span>
          </button>
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
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const searchKeyword = ref('')

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
 * 「同步游戏库」占位动作：库同步属于阶段 3 功能，当前只提示。
 * 请求参数：无
 * 返回参数：无
 */
function onSyncClick() {
  ElMessage.info('阶段 3 接入 Steam 登录与游戏库同步')
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
</script>
