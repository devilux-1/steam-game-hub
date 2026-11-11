<!-- 游戏条目页：游戏信息头部 + Nexus Mod / 汉化补丁 / 风灵月影 三个资源 Tab。 -->
<template>
  <div class="page detail-page">
    <nav class="breadcrumb" aria-label="面包屑">
      <router-link to="/">首页</router-link>
      <span class="breadcrumb__sep">/</span>
      <span class="breadcrumb__current">{{ game?.nameCn || game?.nameEn || `游戏 #${gameId}` }}</span>
    </nav>

    <section class="detail-hero">
      <div class="detail-cover" :class="{ 'detail-cover--empty': !game?.coverUrl }">
        <img v-if="game?.coverUrl" :src="game.coverUrl" :alt="game.nameCn || game.nameEn" />
        <svg v-else viewBox="0 0 64 64" fill="none" aria-hidden="true">
          <rect x="12" y="18" width="40" height="29" rx="9" stroke="currentColor" stroke-width="1.7" />
          <path d="M23 32.5h5M25.5 30v5M39 30.5h.2M44 34h.2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
        </svg>
      </div>

      <div class="detail-info">
        <p class="eyebrow">GAME PROFILE</p>
        <h1 class="detail-title">{{ game?.nameCn || game?.nameEn || `游戏 #${gameId}` }}</h1>
        <p class="detail-en">{{ game?.nameEn || '英文名待接入' }}</p>
        <p class="detail-summary">
          {{ game?.summary || '游戏简介将在阶段 1 接入后端后展示；当前页面为骨架占位。' }}
        </p>

        <p v-if="loadError" class="inline-note">
          <span class="inline-note__dot"></span>
          游戏详情接口暂不可用，页面使用占位内容
        </p>

        <div class="detail-actions">
          <button type="button" class="btn btn--gold" @click="goAdvisor">打开画质顾问</button>
          <button type="button" class="btn btn--ghost" :disabled="!game?.steamAppId" @click="openSteam">
            Steam 商店
          </button>
        </div>
      </div>
    </section>

    <section class="resource-section">
      <header class="section-head">
        <div>
          <p class="eyebrow">RESOURCES</p>
          <h2 class="section-title">资源聚合</h2>
        </div>
        <p class="section-meta">外链直达来源站 · 本站不托管文件</p>
      </header>

      <el-tabs v-model="activeTab" class="resource-tabs">
        <el-tab-pane v-for="tab in tabs" :key="tab.name" :label="tab.label" :name="tab.name">
          <div class="resource-panel">
            <EmptyState :tag="tab.tag" :title="tab.title" :description="tab.description" />
            <p class="resource-panel__foot">{{ tab.foot }}</p>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGame } from '@/api/games'
import { normalizeGame } from '@/utils/game'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const gameId = computed(() => String(route.params.id ?? ''))
const loadError = ref(false)
const game = ref(null)
const activeTab = ref('NEXUS_MOD')

// 三个 Tab 与设计文档 F1 的资源类型一一对应；空状态文案说明各自后续接入阶段。
const tabs = [
  {
    name: 'NEXUS_MOD',
    label: 'Nexus Mod',
    tag: 'STAGE 2',
    title: '暂无 Nexus Mod',
    description: 'Nexus 上的热门 Mod 会在阶段 2 同步到站内。Nexus 下载需要登录账号，跳转后可能先落到 Nexus 登录页。',
    foot: '资源卡片上线后展示：名称、作者、版本、适配游戏版本、更新日期、来源站与点击计数。'
  },
  {
    name: 'HANZI_PATCH',
    label: '汉化补丁',
    tag: '待录入',
    title: '暂无汉化补丁',
    description: '汉化补丁由人工录入，这款游戏还没有收录。补丁可能随游戏版本失效，请以来源站说明为准。',
    foot: '补丁上线后展示：汉化组、版本、适配游戏版本、更新日期与安装要点。'
  },
  {
    name: 'TRAINER',
    label: '风灵月影',
    tag: '待录入',
    title: '暂无风灵月影修改器',
    description: '该游戏还没有收录风灵月影修改器，后续会按游戏热度补充。修改器仅适用于单机模式。',
    foot: '修改器上线后展示：版本、适配游戏版本、更新时间与来源站直达链接。'
  }
]

/**
 * 加载当前路由对应的游戏详情，失败时降级为路由 ID 占位。
 * 请求参数：无（读取 gameId）
 * 返回参数：无
 */
async function loadGame() {
  loadError.value = false
  try {
    game.value = normalizeGame(await getGame(gameId.value))
  } catch {
    // 后端未启动或数据未录入时保留页面结构，用 ID 和占位文案继续展示。
    game.value = null
    loadError.value = true
  }
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

// 同一组件在 /games/1 与 /games/2 之间切换时重新拉取详情。
watch(() => route.params.id, loadGame, { immediate: true })
</script>
