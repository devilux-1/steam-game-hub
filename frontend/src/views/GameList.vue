<!-- 全部游戏页：按资源数排序、分页展示站内所有游戏，卡片样式与首页热门区保持一致。 -->
<template>
  <div class="page game-list-page">
    <header class="page-head">
      <h1 class="page-title">全部游戏</h1>
      <p class="page-subtitle">
        <template v-if="loading">加载中…</template>
        <template v-else-if="error">接口异常</template>
        <template v-else>按资源数排序 · 共 {{ total }} 款</template>
      </p>
    </header>

    <div v-if="loading" class="game-grid" aria-label="游戏列表加载中">
      <div v-for="row in skeletonCount" :key="row" class="game-card game-card--skeleton" aria-hidden="true">
        <div class="game-card__cover"></div>
        <div class="game-card__body">
          <span class="skeleton-line skeleton-line--title"></span>
          <span class="skeleton-line"></span>
          <span class="skeleton-line skeleton-line--short"></span>
        </div>
      </div>
    </div>

    <EmptyState
      v-else-if="error"
      tag="接口异常"
      title="游戏列表加载失败"
      description="与后端的连接尚未建立，或接口返回异常。确认后端已启动后可重新加载。"
    >
      <template #actions>
        <button type="button" class="btn btn--ghost" @click="load">重新加载</button>
      </template>
    </EmptyState>

    <template v-else-if="games.length">
      <div class="game-grid">
        <router-link v-for="item in games" :key="item.id" :to="`/games/${item.id}`" class="game-card">
          <div class="game-card__cover">
            <span class="game-card__count">{{ item.resourceCount }} 资源</span>
            <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.nameCn || item.nameEn" loading="lazy" />
            <svg v-else viewBox="0 0 64 64" fill="none" aria-hidden="true">
              <rect x="13" y="19" width="38" height="27" rx="9" stroke="currentColor" stroke-width="1.6" />
              <path d="M23 32.5h5M25.5 30v5M39 30.5h.2M44 34h.2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            </svg>
          </div>
          <div class="game-card__body">
            <h3 class="game-card__title">{{ item.nameCn || item.nameEn || '未命名游戏' }}</h3>
            <p class="game-card__en">{{ item.nameEn || 'NO ENGLISH TITLE' }}</p>
            <p class="game-card__tag">{{ item.summary || '暂无简介' }}</p>
          </div>
        </router-link>
      </div>

      <div class="game-list-pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="PAGE_SIZE"
          :current-page="page"
          @current-change="onPageChange"
        />
      </div>
    </template>

    <EmptyState
      v-else
      tag="页码超出范围"
      title="这一页没有游戏"
      :description="`站内共 ${total} 款游戏，当前页码已经超出范围，回到第一页继续浏览。`"
    >
      <template #actions>
        <button type="button" class="btn btn--ghost" @click="onPageChange(1)">回到第一页</button>
      </template>
    </EmptyState>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchGames } from '@/api/games'
import { normalizeGame } from '@/utils/game'
import EmptyState from '@/components/EmptyState.vue'

/** 每页条数：6 列网格正好 4 行，也与后端 size 上限（100）无冲突 */
const PAGE_SIZE = 24

/** 骨架卡片数量：取半页，避免首屏铺满 24 个占位块 */
const skeletonCount = computed(() => Math.min(PAGE_SIZE, 12))

const route = useRoute()
const router = useRouter()

/** 当前页游戏列表 */
const games = ref([])
/** 游戏总数 */
const total = ref(0)
/** 是否正在加载 */
const loading = ref(false)
/** 是否处于接口异常态 */
const error = ref(false)
/** 当前页码 */
const page = ref(1)

/**
 * 解析路由里的页码，非法值一律回落第 1 页。
 * 请求参数：raw —— 路由 query.page，可为 undefined / 字符串 / 数组
 * 返回参数：大于等于 1 的整数页码
 */
function parsePage(raw) {
  const value = Array.isArray(raw) ? raw[0] : raw
  const parsed = Number.parseInt(String(value ?? ''), 10)
  return Number.isFinite(parsed) && parsed >= 1 ? parsed : 1
}

/**
 * 按当前页码拉取一页游戏。
 * 请求参数：无（读取 page）
 * 返回参数：无；失败时置 error 并清空列表
 */
async function load() {
  loading.value = true
  error.value = false
  try {
    const { items, total: count } = await searchGames({ keyword: '', page: page.value, size: PAGE_SIZE })
    games.value = items.map(normalizeGame).filter((item) => item.id !== '')
    total.value = count
  } catch {
    // 拦截器已提示，这里只切到错误态，避免页面停留在旧数据上
    games.value = []
    total.value = 0
    error.value = true
  } finally {
    loading.value = false
  }
}

/**
 * 切换页码：写回地址栏后由 watch 触发加载，保证刷新与分享保持位置。
 * 请求参数：next —— 目标页码，大于等于 1
 * 返回参数：无
 */
function onPageChange(next) {
  const target = Math.max(1, Number(next) || 1)
  window.scrollTo({ top: 0, behavior: 'smooth' })
  if (target === page.value) {
    load()
    return
  }
  router.replace({ query: { ...route.query, page: String(target) } })
}

// 页码来自路由，进入页面与翻页都走同一条链路。
watch(
  () => route.query.page,
  (raw) => {
    page.value = parsePage(raw)
    load()
  },
  { immediate: true }
)
</script>

<style scoped>
/* 分页条：居中放在网格下方，与卡片列宽留出呼吸 */
.game-list-pager {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
