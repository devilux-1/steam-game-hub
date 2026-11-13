<!-- 游戏条目页：游戏信息头部 + Nexus Mod / 汉化补丁 / 风灵月影 三个资源 Tab。 -->
<template>
  <div class="page detail-page">
    <nav class="breadcrumb" aria-label="面包屑">
      <router-link to="/">首页</router-link>
      <span class="breadcrumb__sep">/</span>
      <span class="breadcrumb__current">{{ game?.nameCn || game?.nameEn || `游戏 #${gameId}` }}</span>
    </nav>

    <!-- 详情加载中：用骨架屏占住头部结构，避免页面高度跳动。 -->
    <section v-if="detailLoading" class="detail-hero" aria-label="游戏详情加载中">
      <div class="detail-cover detail-skeleton"></div>
      <div class="detail-info">
        <div class="detail-skeleton detail-skeleton--title"></div>
        <div class="detail-skeleton detail-skeleton--line"></div>
        <div class="detail-skeleton detail-skeleton--line detail-skeleton--short"></div>
      </div>
    </section>

    <!-- 详情失败：404 与接口异常的文案和操作不同，分开展示。 -->
    <section v-else-if="detailError" class="detail-fallback">
      <EmptyState
        :tag="detailError === 'notFound' ? '404' : '接口异常'"
        :title="detailError === 'notFound' ? '这款游戏不存在' : '游戏详情加载失败'"
        :description="
          detailError === 'notFound'
            ? '链接可能已失效，或这款游戏还没有收录到站内。返回首页看看其它游戏。'
            : '与后端的连接尚未建立，或接口返回异常。确认后端已启动后可重新加载。'
        "
      >
        <template #actions>
          <button v-if="detailError !== 'notFound'" type="button" class="btn btn--ghost" @click="loadDetail">
            重新加载
          </button>
          <router-link class="btn btn--ghost" to="/">返回首页</router-link>
        </template>
      </EmptyState>
    </section>

    <section v-else class="detail-hero">
      <div class="detail-cover" :class="{ 'detail-cover--empty': !game?.coverUrl }">
        <img v-if="game?.coverUrl" :src="game.coverUrl" :alt="game.nameCn || game.nameEn" />
        <svg v-else viewBox="0 0 64 64" fill="none" aria-hidden="true">
          <rect x="12" y="18" width="40" height="29" rx="9" stroke="currentColor" stroke-width="1.7" />
          <path d="M23 32.5h5M25.5 30v5M39 30.5h.2M44 34h.2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
        </svg>
      </div>

      <div class="detail-info">
        <h1 class="detail-title">{{ game?.nameCn || game?.nameEn || `游戏 #${gameId}` }}</h1>
        <p class="detail-en">{{ game?.nameEn || 'NO ENGLISH TITLE' }}</p>
        <p class="detail-summary">{{ game?.summary || '暂无简介。' }}</p>

        <div v-if="game?.aliases.length" class="detail-aliases" aria-label="游戏别名">
          <span v-for="alias in game.aliases" :key="alias" class="detail-alias">{{ alias }}</span>
        </div>

        <p class="detail-meta">
          <span v-if="game?.releaseDate">发售 {{ formatDate(game.releaseDate) }}</span>
          <span>收录 {{ game?.resourceCount ?? 0 }} 条资源</span>
        </p>

        <div class="detail-actions">
          <button type="button" class="btn btn--gold" @click="goAdvisor">打开画质顾问</button>
          <button type="button" class="btn btn--ghost" :disabled="!game?.steamAppId" @click="openSteam">
            Steam 商店
          </button>
        </div>
      </div>
    </section>

    <section v-if="!detailLoading && !detailError" class="resource-section">
      <header class="section-head">
        <div>
          <h2 class="section-title">资源聚合</h2>
        </div>
        <p class="section-meta">外链直达来源站 · 本站不托管文件</p>
      </header>

      <el-tabs v-model="activeTab" class="resource-tabs" @tab-change="onTabChange">
        <el-tab-pane v-for="tab in tabs" :key="tab.name" :label="tab.label" :name="tab.name">
          <div class="resource-panel">
            <!-- Nexus 每日精选：后台同步缓存的 Mod，仅 Nexus Tab 展示，与下方手工资源分开展示。 -->
            <section v-if="tab.name === 'NEXUS_MOD'" class="nexus-feature" aria-label="Nexus 每日精选">
              <div v-if="nexusState.loading" class="nexus-loading" aria-label="Nexus 精选加载中">
                <div v-for="row in 2" :key="row" class="nexus-loading__row"></div>
              </div>

              <!-- 失败时只提示这一块不可用，手工资源仍照常展示。 -->
              <p v-else-if="nexusState.error" class="nexus-feature__error">
                Nexus 精选暂不可用，下方收录资源不受影响。
              </p>

              <template v-else-if="nexusState.items.length">
                <header class="nexus-feature__head">
                  <h3 class="nexus-feature__title">Nexus 每日精选</h3>
                  <p class="nexus-feature__meta">按点赞数排序 · 后台同步缓存</p>
                </header>

                <div class="nexus-grid">
                  <!-- 同步卡片直接外链到 Nexus，不计入站内点击量。 -->
                  <a
                    v-for="mod in nexusState.items"
                    :key="mod.modId"
                    class="nexus-card"
                    :href="mod.modUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <header class="nexus-card__head">
                      <span class="nexus-card__badge">NEXUS 同步</span>
                      <span v-if="mod.author" class="nexus-card__author">{{ mod.author }}</span>
                    </header>
                    <h4 class="nexus-card__title">{{ mod.name }}</h4>
                    <p v-if="mod.summary" class="nexus-card__desc">{{ mod.summary }}</p>
                    <footer class="nexus-card__stats">
                      <span class="nexus-card__stat">
                        <span class="nexus-card__stat-label">赞</span>{{ formatNexusCount(mod.endorsements) }}
                      </span>
                      <span class="nexus-card__stat">
                        <span class="nexus-card__stat-label">下载</span>{{ formatNexusCount(mod.downloads) }}
                      </span>
                      <span class="nexus-card__go">打开 Nexus ↗</span>
                    </footer>
                  </a>
                </div>
              </template>
            </section>

            <div v-if="resourceStates[tab.name].loading" class="resource-loading" aria-label="资源加载中">
              <div v-for="row in 3" :key="row" class="resource-loading__row"></div>
            </div>

            <EmptyState
              v-else-if="resourceStates[tab.name].error"
              tag="接口异常"
              title="资源列表加载失败"
              description="与后端的连接尚未建立，或接口返回异常。确认后端已启动后可重新加载。"
            >
              <template #actions>
                <button type="button" class="btn btn--ghost" @click="loadResources(tab.name, true)">重新加载</button>
              </template>
            </EmptyState>

            <div v-else-if="resourceStates[tab.name].items.length" class="resource-list">
              <article
                v-for="item in resourceStates[tab.name].items"
                :key="item.id"
                class="resource-card"
                role="link"
                tabindex="0"
                :aria-label="`打开资源：${item.title}`"
                @click="openResource(item)"
                @keydown.enter.prevent="openResource(item)"
                @keydown.space.prevent="openResource(item)"
              >
                <header class="resource-card__head">
                  <h3 class="resource-card__title">{{ item.title }}</h3>
                  <span class="resource-card__clicks">{{ item.clickCount }} 次点击</span>
                </header>

                <p v-if="item.description" class="resource-card__desc">{{ item.description }}</p>

                <dl class="resource-card__meta">
                  <div>
                    <dt>版本</dt>
                    <dd>{{ item.version || '—' }}</dd>
                  </div>
                  <div>
                    <dt>适配版本</dt>
                    <dd>{{ item.compatibleVersion || '—' }}</dd>
                  </div>
                  <div>
                    <dt>作者</dt>
                    <dd>{{ item.author || '—' }}</dd>
                  </div>
                  <div>
                    <dt>来源</dt>
                    <dd>{{ item.sourceSite || '—' }}</dd>
                  </div>
                  <div>
                    <dt>更新</dt>
                    <dd>{{ formatDateTime(item.updatedAt) || '—' }}</dd>
                  </div>
                </dl>

                <p v-if="item.installNote" class="resource-card__note">安装要点：{{ item.installNote }}</p>
                <span class="resource-card__go">打开来源站 ↗</span>
              </article>
            </div>

            <EmptyState v-else :tag="tab.tag" :title="tab.emptyTitle" :description="tab.emptyDesc" />

            <p class="resource-panel__foot">{{ tab.foot }}</p>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGame, getGameResources, getGameNexusMods, clickResource } from '@/api/games'
import { normalizeGame } from '@/utils/game'
import { normalizeResource } from '@/utils/resource'
import { normalizeNexusMod, formatNexusCount } from '@/utils/nexus'
import { formatDate, formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const gameId = computed(() => String(route.params.id ?? ''))
const detailLoading = ref(false)
// '' 表示正常，notFound 表示 404，error 表示网络或服务异常。
const detailError = ref('')
const game = ref(null)
const activeTab = ref('NEXUS_MOD')

// 三个 Tab 与设计文档 F1 的资源类型一一对应；空状态文案说明该游戏暂未收录的原因。
const tabs = [
  {
    name: 'NEXUS_MOD',
    label: 'Nexus Mod',
    tag: '暂无收录',
    emptyTitle: '暂无 Nexus Mod',
    emptyDesc: '这款游戏还没有收录 Nexus Mod。Nexus 下载需要登录账号，跳转后可能先落到 Nexus 登录页。',
    foot: '外链直达 Nexus 页面，本站不托管任何文件。'
  },
  {
    name: 'HANZI_PATCH',
    label: '汉化补丁',
    tag: '暂无收录',
    emptyTitle: '暂无汉化补丁',
    emptyDesc: '这款游戏还没有收录汉化补丁。补丁可能随游戏版本失效，请以来源站说明为准。',
    foot: '外链直达汉化组页面，安装前请核对适配版本。'
  },
  {
    name: 'TRAINER',
    label: '风灵月影',
    tag: '暂无收录',
    emptyTitle: '暂无风灵月影修改器',
    emptyDesc: '这款游戏还没有收录风灵月影修改器，后续会按游戏热度补充。修改器仅适用于单机模式。',
    foot: '外链直达风灵月影官网页面，仅限单机模式使用。'
  }
]

/**
 * 创建单个 Tab 的资源列表状态。
 * 请求参数：无
 * 返回参数：{ items 资源数组, loading 加载中, error 接口失败, loaded 是否已成功加载过 }
 */
function createResourceState() {
  return { items: [], loading: false, error: false, loaded: false }
}

// 三个 Tab 独立维护加载态与数据，切换时按需请求并缓存，避免重复请求后端。
const resourceStates = reactive({
  NEXUS_MOD: createResourceState(),
  HANZI_PATCH: createResourceState(),
  TRAINER: createResourceState()
})

// Nexus 精选独立三态：接口失败或暂无数据都不影响手工资源的展示。
const nexusState = reactive({ items: [], loading: false, error: false })

// 请求序号：切换游戏时递增，用于丢弃上一个游戏晚到的响应，防止串数据。
let requestSeq = 0

/**
 * 重置三个 Tab 的资源状态，切换到另一款游戏时调用。
 * 请求参数：无
 * 返回参数：无
 */
function resetResourceStates() {
  Object.values(resourceStates).forEach((state) => {
    state.items = []
    state.loading = false
    state.error = false
    state.loaded = false
  })
}

/**
 * 重置 Nexus 精选区域，切换到另一款游戏时调用。
 * 请求参数：无
 * 返回参数：无
 */
function resetNexusState() {
  nexusState.items = []
  nexusState.loading = false
  nexusState.error = false
}

/**
 * 加载指定类型的资源列表，已成功加载过则直接复用缓存。
 * 请求参数：type —— 资源类型 NEXUS_MOD / HANZI_PATCH / TRAINER；force —— 是否忽略缓存强制重查，默认 false
 * 返回参数：无；失败时标记该 Tab 的 error 由页面展示重试入口
 */
async function loadResources(type, force = false) {
  const state = resourceStates[type]
  if (!state) return
  if (state.loading || (state.loaded && !force)) return

  const seq = requestSeq
  state.loading = true
  state.error = false
  try {
    const { items } = await getGameResources(gameId.value, type)
    // 游戏已切换时丢弃旧响应，避免把上一款游戏的资源写进当前页面。
    if (seq !== requestSeq) return
    state.items = items.map(normalizeResource).filter((item) => item.id !== '')
    state.loaded = true
  } catch {
    if (seq !== requestSeq) return
    state.error = true
    state.items = []
  } finally {
    if (seq === requestSeq) state.loading = false
  }
}

/**
 * 加载当前游戏的 Nexus 每日精选数据。
 * 请求参数：无（读取 gameId 与请求序号）
 * 返回参数：无；失败时仅标记 Nexus 区域不可用，手工资源列表不受影响
 */
async function loadNexusMods() {
  const seq = requestSeq
  nexusState.loading = true
  nexusState.error = false
  try {
    const list = await getGameNexusMods(gameId.value, 20)
    if (seq !== requestSeq) return
    // 没有 modUrl 的脏数据无法跳转，直接过滤掉。
    nexusState.items = list.map(normalizeNexusMod).filter((item) => item.modUrl)
  } catch {
    if (seq !== requestSeq) return
    nexusState.items = []
    nexusState.error = true
  } finally {
    if (seq === requestSeq) nexusState.loading = false
  }
}

/**
 * Tab 切换时按需加载对应资源。
 * 请求参数：name —— 目标 Tab 的资源类型名
 * 返回参数：无
 */
function onTabChange(name) {
  loadResources(name)
}

/**
 * 加载当前路由对应的游戏详情，并在成功后加载默认 Tab 的资源。
 * 请求参数：无（读取 gameId）
 * 返回参数：无；404 与其它异常分别标记，交给页面展示不同提示
 */
async function loadDetail() {
  const seq = ++requestSeq
  detailLoading.value = true
  detailError.value = ''
  resetResourceStates()
  resetNexusState()
  try {
    const data = await getGame(gameId.value)
    if (seq !== requestSeq) return
    game.value = normalizeGame(data)
    loadResources(activeTab.value)
    loadNexusMods()
  } catch (error) {
    if (seq !== requestSeq) return
    game.value = null
    detailError.value = error?.response?.status === 404 ? 'notFound' : 'error'
  } finally {
    if (seq === requestSeq) detailLoading.value = false
  }
}

/**
 * 点击资源卡片：先上报点击量，再在新窗口打开来源站。
 * 请求参数：item —— 已归一化的资源对象，需含 id 与 sourceUrl
 * 返回参数：无；点击统计失败不影响跳转，保证外链始终可用
 */
async function openResource(item) {
  if (!item?.sourceUrl) return
  try {
    await clickResource(item.id)
  } catch {
    // 计数失败不阻断跳转：资源直达是主流程，统计只是辅助数据。
  }
  window.open(item.sourceUrl, '_blank')
}

/**
 * 在新窗口打开该游戏的 Steam 商店页。
 * 请求参数：无（读取已加载游戏对象的 steamAppId）
 * 返回参数：无；缺少 AppID 时直接返回
 */
function openSteam() {
  const appId = game.value?.steamAppId
  if (!appId) return
  window.open(`https://store.steampowered.com/app/${appId}/`, '_blank', 'noopener')
}

/**
 * 跳转到当前游戏的画质顾问页。
 * 请求参数：无（读取 gameId）
 * 返回参数：无
 */
function goAdvisor() {
  router.push({ name: 'graphics-advisor', params: { id: gameId.value } })
}

// 同一组件在 /games/1 与 /games/2 之间切换时重新拉取详情与资源。
watch(() => route.params.id, loadDetail, { immediate: true })
</script>
