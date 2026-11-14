<!-- 库内游戏兜底页：库内游戏还没生成站内条目时展示，引导回游戏库重新同步。 -->
<template>
  <div class="page library-detail-page">
    <nav class="detail-breadcrumb" aria-label="面包屑">
      <router-link to="/me/library">我的游戏库</router-link>
      <span aria-hidden="true">/</span>
      <span>{{ displayName }}</span>
    </nav>

    <div v-if="loading" class="resource-loading" aria-label="加载中">
      <div v-for="row in 2" :key="row" class="resource-loading__row"></div>
    </div>

    <EmptyState v-else-if="error" tag="无法打开" :title="errorTitle" :description="errorDescription">
      <template #actions>
        <router-link class="btn btn--ghost" to="/me/library">返回我的游戏库</router-link>
      </template>
    </EmptyState>

    <template v-else>
      <section class="detail-hero">
        <div class="detail-cover" :class="{ 'detail-cover--empty': !detail.coverUrl }">
          <img v-if="detail.coverUrl" :src="detail.coverUrl" :alt="displayName" @error="hideBrokenImage" />
        </div>

        <div class="detail-info">
          <h1 class="detail-title">{{ displayName }}</h1>
          <p class="detail-en">{{ detail.title }}</p>

          <div v-if="genres.length" class="detail-aliases" aria-label="游戏类型">
            <span v-for="genre in genres" :key="genre" class="detail-alias">{{ genre }}</span>
          </div>

          <p class="detail-summary">
            {{ detail.store?.shortDescription || '本站暂未收录这款游戏的简介，可点下方按钮到 Steam 商店查看详情。' }}
          </p>

          <p class="detail-meta">
            <span v-if="detail.store?.releaseDate">发售 {{ detail.store.releaseDate }}</span>
            <span v-if="detail.store?.developers">开发 {{ detail.store.developers }}</span>
            <span>{{ detail.gameId ? '本站已收录' : '本站暂未收录' }}</span>
          </p>

          <div class="detail-actions">
            <router-link v-if="detail.gameId" class="btn btn--primary" :to="`/games/${detail.gameId}`">
              查看站内条目
            </router-link>
            <a class="btn btn--ghost" :href="storeUrl" target="_blank" rel="noopener">在 Steam 商店打开</a>
          </div>
        </div>
      </section>

      <div class="library-stats">
        <div class="library-stat">
          <span class="library-stat__label">总时长</span>
          <strong class="library-stat__value">{{ formatMinutes(detail.playtimeForever) }}</strong>
        </div>
        <div class="library-stat">
          <span class="library-stat__label">近两周</span>
          <strong class="library-stat__value">{{ formatMinutes(detail.playtime2weeks) }}</strong>
        </div>
        <div class="library-stat">
          <span class="library-stat__label">最后游玩</span>
          <strong class="library-stat__value">
            {{ detail.lastPlayedAt ? formatDate(detail.lastPlayedAt) : '从未游玩' }}
          </strong>
        </div>
      </div>

      <section class="detail-pending">
        <h2 class="section-title">资源聚合</h2>
        <p class="detail-pending__text">
          这款游戏已从你的 Steam 库同步，站内条目还没生成（通常是库数据未刷新）。
          回到游戏库点「重新同步」后，它就会像其他游戏一样打开资源聚合页。
        </p>
        <router-link class="btn btn--primary" to="/me/library">返回游戏库重新同步</router-link>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getLibraryItem } from '@/api/user'
import { formatDate, formatMinutes } from '@/utils/format'
import { hideBrokenImage } from '@/utils/image'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()

/** 详情数据，加载完成前为 null */
const detail = ref(null)
/** 是否正在加载 */
const loading = ref(true)
/** 是否处于错误态 */
const error = ref(false)
/** 错误态标题 */
const errorTitle = ref('')
/** 错误态说明 */
const errorDescription = ref('')

/**
 * 展示名：优先站内中文名，其次 Steam 商店名，最后是库内记录的英文名。
 * 请求参数：无（读取 detail）
 * 返回参数：字符串，均为空时返回「游戏详情」
 */
const displayName = computed(
  () => detail.value?.gameNameCn || detail.value?.store?.name || detail.value?.title || '游戏详情'
)

/** 类型标签列表，无数据时为空数组 */
const genres = computed(() => detail.value?.store?.genres ?? [])

/**
 * Steam 商店地址：优先用接口返回的地址，取不到时按 appid 拼一个。
 * 请求参数：无（读取 detail 与路由 appid）
 * 返回参数：完整的 https 地址
 */
const storeUrl = computed(
  () => detail.value?.store?.storeUrl || `https://store.steampowered.com/app/${route.params.appid}/`
)

/**
 * 加载库内详情，并按 401 / 404 / 其它错误区分提示。
 * 请求参数：无（读取路由参数 appid）
 * 返回参数：无；失败时设置 error 与提示文案，页面不再渲染详情
 */
async function loadDetail() {
  const appid = String(route.params.appid ?? '')
  if (!appid) {
    error.value = true
    errorTitle.value = '缺少游戏 ID'
    errorDescription.value = '地址里没有 Steam AppID，请从「我的游戏库」列表点进游戏。'
    loading.value = false
    return
  }
  loading.value = true
  error.value = false
  try {
    detail.value = await getLibraryItem(appid)
  } catch (err) {
    detail.value = null
    error.value = true
    const status = err?.response?.status
    if (status === 401) {
      errorTitle.value = '请先登录 Steam'
      errorDescription.value = '登录后才能查看自己的游戏库详情，可从右上角「Steam 登录」进入。'
    } else if (status === 404) {
      errorTitle.value = '你的游戏库里没有这款游戏'
      errorDescription.value = '该 AppID 不在当前账号的 Steam 库中，或库数据已过期，可回到游戏库重新同步。'
    } else {
      errorTitle.value = '详情加载失败'
      errorDescription.value = '服务暂不可用，请确认后端已启动后重试。'
    }
  } finally {
    loading.value = false
  }
}

// 同一组件在 /library/1 与 /library/2 之间切换时重新拉取。
watch(() => route.params.appid, loadDetail, { immediate: true })
onMounted(() => {
  // watch immediate 已覆盖首次加载，这里只保留挂载钩子便于后续扩展
})
</script>

<style scoped>
/* 面包屑：详情页顶部的一行路径，避免用户失去层级感 */
.detail-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: var(--text-dim);
  font-size: 13px;
}

.detail-breadcrumb a {
  color: var(--text-soft);
  text-decoration: none;
}

.detail-breadcrumb a:hover {
  color: var(--accent);
}

/* 收录提示区：说明为什么还没有资源列表 */
.detail-pending {
  margin-top: 28px;
  padding: 22px 24px;
  border: 1px dashed var(--line-strong, rgba(23, 35, 58, 0.22));
  border-radius: var(--radius-lg, 14px);
  background: var(--surface, rgba(255, 255, 255, 0.5));
}

.detail-pending__text {
  margin: 10px 0 16px;
  color: var(--text-dim);
  font-size: 13px;
  line-height: 1.8;
}
</style>
