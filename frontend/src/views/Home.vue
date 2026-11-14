<!-- 首页：主搜索入口、站内统计、搜索结果与热门游戏列表。 -->
<template>
  <div class="page home-page">
    <section class="hero">
      <div class="hero__content">
        <h1 class="hero__title">一个游戏，<span class="hero__title-accent">一页</span>找齐。</h1>
        <p class="hero__desc">
          Nexus Mod、汉化补丁、风灵月影修改器按游戏聚合。本站只做外链跳转，不托管任何文件。
        </p>

        <form class="hero-search" role="search" @submit.prevent="onSearchSubmit">
          <el-input
            v-model="keyword"
            class="hero-search__input"
            size="large"
            placeholder="输入游戏中文名、英文名或别名，如：老头环"
            clearable
            aria-label="搜索游戏"
          >
            <template #prefix>
              <svg class="hero-search__icon" viewBox="0 0 20 20" fill="none" aria-hidden="true">
                <!-- deslop-ignore-next-line 24: 放大镜是标准两笔图标，非手绘装饰 -->
                <circle cx="8.8" cy="8.8" r="5.6" stroke="currentColor" stroke-width="1.5" />
                <path d="m13.2 13.2 3.4 3.4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
              </svg>
            </template>
          </el-input>
          <button type="submit" class="btn btn--gold hero-search__button">搜索游戏</button>
        </form>

        <ul class="hero__points">
          <li>按游戏聚合</li>
          <li>外链直达来源站</li>
        </ul>
      </div>
    </section>

    <section class="stats-bar" aria-label="站内统计">
      <div class="stats-bar__item">
        <p class="stats-bar__label">收录游戏</p>
        <p class="stats-bar__number">{{ formatStat(stats.gameCount) }}<span>款</span></p>
        <p class="stats-bar__hint">支持中英文名与别名检索</p>
      </div>
      <div class="stats-bar__item">
        <p class="stats-bar__label">资源条目</p>
        <p class="stats-bar__number">{{ formatStat(stats.resourceCount) }}<span>条</span></p>
        <p class="stats-bar__hint">Nexus / 汉化 / 修改器</p>
      </div>
      <div class="stats-bar__item">
        <p class="stats-bar__label">资源类型</p>
        <p class="stats-bar__number">3<span>类</span></p>
        <p class="stats-bar__hint">Nexus Mod · 汉化补丁 · 风灵月影</p>
      </div>
    </section>

    <!-- 搜索结果区：有搜索词时替换热门列表，区分加载中、接口失败与确实无结果。 -->
    <section v-if="searched" class="section">
      <header class="section-head">
        <div>
          <h2 class="section-title">「{{ keyword }}」的搜索结果</h2>
        </div>
        <p v-if="!searching && !searchError" class="section-meta">共 {{ searchResult.total }} 条</p>
      </header>

      <div v-if="searching" class="search-loading" aria-label="搜索中">
        <div v-for="row in 3" :key="row" class="search-loading__row"></div>
      </div>

      <EmptyState
        v-else-if="searchError"
        tag="接口异常"
        title="搜索服务暂不可用"
        description="与后端的连接尚未建立，或接口返回异常。确认后端已启动后，重新提交搜索即可。"
      >
        <template #actions>
          <button type="button" class="btn btn--ghost" @click="runSearch(keyword)">重新搜索</button>
        </template>
      </EmptyState>

      <div v-else-if="resultGames.length" class="game-grid">
        <router-link v-for="item in resultGames" :key="item.id" :to="`/games/${item.id}`" class="game-card">
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

      <EmptyState
        v-else
        tag="暂无结果"
        :title="`没有找到「${keyword}」`"
        description="试试更换中文名、英文名，或只输入关键词的一部分。站内游戏库会持续补充。"
      />
    </section>

    <!-- 热门游戏：进页面即拉取第一页游戏，后端按资源数排序。 -->
    <section v-else class="section">
      <header class="section-head">
        <div>
          <h2 class="section-title">热门游戏</h2>
        </div>
        <p class="section-meta">
          <template v-if="hotLoading">加载中…</template>
          <template v-else-if="hotError">接口异常</template>
          <template v-else>按资源数排序 · 共 {{ hotTotal }} 款</template>
        </p>
      </header>

      <div v-if="hotLoading" class="game-grid" aria-label="热门游戏加载中">
        <div v-for="row in 6" :key="row" class="game-card game-card--skeleton" aria-hidden="true">
          <div class="game-card__cover"></div>
          <div class="game-card__body">
            <span class="skeleton-line skeleton-line--title"></span>
            <span class="skeleton-line"></span>
            <span class="skeleton-line skeleton-line--short"></span>
          </div>
        </div>
      </div>

      <EmptyState
        v-else-if="hotError"
        tag="接口异常"
        title="热门游戏加载失败"
        description="与后端的连接尚未建立，或接口返回异常。确认后端已启动后可重新加载。"
      >
        <template #actions>
          <button type="button" class="btn btn--ghost" @click="loadHotGames">重新加载</button>
        </template>
      </EmptyState>

      <div v-else-if="hotGames.length" class="game-grid">
        <router-link v-for="item in hotGames" :key="item.id" :to="`/games/${item.id}`" class="game-card">
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

      <EmptyState
        v-else
        tag="暂无收录"
        title="站内还没有收录游戏"
        description="后台录入游戏与资源后，热门榜会自动出现在这里。"
      />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchGames } from '@/api/games'
import { getStatsOverview } from '@/api/stats'
import { normalizeGame } from '@/utils/game'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

// 搜索框内容；地址栏带 keyword 时由路由监听回填，保证刷新后仍能看到结果。
const keyword = ref('')
const searching = ref(false)
const searched = ref(false)
const searchError = ref(false)
const searchResult = ref({ items: [], total: 0 })

// 统计数字：接口失败时保持 null，模板统一显示「--」。
const stats = reactive({ gameCount: null, resourceCount: null })

// 热门游戏取搜索接口无关键词的第一页，后端按资源数排序。
const hotGames = ref([])
const hotTotal = ref(0)
const hotLoading = ref(false)
const hotError = ref(false)

/**
 * 搜索结果中可展示的游戏列表。
 * 请求参数：无（读取 searchResult）
 * 返回参数：归一化后的游戏对象数组
 */
const resultGames = computed(() => searchResult.value.items.map(normalizeGame))

/**
 * 把统计值格式化为展示文本。
 * 请求参数：value —— 接口返回的数字，可为 null
 * 返回参数：千分位字符串；无有效数字时返回「--」，避免页面出现 0 误导数据
 */
function formatStat(value) {
  return typeof value === 'number' ? value.toLocaleString('zh-CN') : '--'
}

/**
 * 从统计对象中按候选字段名取第一个有效数字。
 * 请求参数：source —— 统计接口返回对象，可能为空；keys —— 候选字段名数组，按优先级排列
 * 返回参数：命中的非负数字；全部缺失时返回 null
 */
function pickNumber(source, keys) {
  if (!source || typeof source !== 'object') return null
  for (const key of keys) {
    const raw = source[key]
    // 后端可能返回 null / 空串表示暂缺，这类值不能当成数字 0。
    if (raw === null || raw === undefined || raw === '') continue
    const value = Number(raw)
    if (Number.isFinite(value) && value >= 0) return value
  }
  return null
}

/**
 * 加载站内统计数字；失败时保持 null，由页面显示「--」。
 * 请求参数：无
 * 返回参数：无
 */
async function loadStats() {
  try {
    const data = await getStatsOverview()
    stats.gameCount = pickNumber(data, ['gameCount', 'gameTotal', 'games'])
    stats.resourceCount = pickNumber(data, ['resourceCount', 'resourceTotal', 'resources'])
  } catch {
    // 统计接口不可用属于可降级场景：保留占位符，不影响首页其它内容。
    stats.gameCount = null
    stats.resourceCount = null
  }
}

/**
 * 加载热门游戏列表（无关键词的第一页数据）。
 * 请求参数：无
 * 返回参数：无；失败时标记 hotError，由页面展示接口异常状态
 */
async function loadHotGames() {
  hotLoading.value = true
  hotError.value = false
  try {
    const { items, total } = await searchGames({ keyword: '', page: 1, size: 12 })
    hotGames.value = items.map(normalizeGame).filter((item) => item.id !== '')
    hotTotal.value = total
  } catch {
    hotError.value = true
    hotGames.value = []
    hotTotal.value = 0
  } finally {
    hotLoading.value = false
  }
}

/**
 * 按关键词执行搜索并刷新结果区。
 * 请求参数：rawKeyword —— 搜索词，允许为空；空串时清空结果并回到热门展示
 * 返回参数：无
 */
async function runSearch(rawKeyword) {
  const value = rawKeyword.trim()
  keyword.value = value
  if (!value) {
    searched.value = false
    searchError.value = false
    searchResult.value = { items: [], total: 0 }
    return
  }

  searching.value = true
  searched.value = true
  searchError.value = false
  try {
    searchResult.value = await searchGames({ keyword: value, page: 1, size: 12 })
  } catch {
    // 「接口不可用」与「确实没有结果」对用户含义不同，这里分开标记以便展示不同空状态。
    searchError.value = true
    searchResult.value = { items: [], total: 0 }
  } finally {
    searching.value = false
  }
}

/**
 * 提交首页主搜索框：优先把关键词写进地址栏，由路由监听统一发起查询。
 * 请求参数：无（读取 keyword）
 * 返回参数：无
 */
function onSearchSubmit() {
  const value = keyword.value.trim()
  if (!value) {
    router.push({ name: 'home' })
    return
  }
  const routeKeyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  if (value === routeKeyword) {
    // 地址栏关键词未变化时不会触发 watch，手动重查一次以支持失败后重试。
    runSearch(value)
    return
  }
  router.push({ name: 'home', query: { keyword: value } })
}

// 监听地址栏搜索词：顶部导航搜索、浏览器前进后退都通过这里走同一套查询逻辑。
watch(
  () => route.query.keyword,
  (value) => {
    runSearch(typeof value === 'string' ? value : '')
  },
  { immediate: true }
)

// 首页挂载时并行拉取统计与热门游戏；任一接口不可用都不阻塞页面渲染。
onMounted(() => {
  loadStats()
  loadHotGames()
})
</script>
