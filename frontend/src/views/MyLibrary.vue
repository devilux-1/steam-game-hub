<!-- 我的游戏库：未登录引导 → 已登录未同步 → 统计卡 + 可排序筛选的库列表。 -->
<template>
  <div class="page library-page">
    <header class="page-head">
      <h1 class="page-title">我的游戏库</h1>
      <p class="page-subtitle">同步 Steam 已拥有游戏后，按时长排序查看，并直接跳到站内游戏条目。</p>
    </header>

    <!-- 未登录：引导去 Steam 授权，OpenID 登录不需要密码 -->
    <section v-if="!userStore.isLogin" class="library-panel">
      <EmptyState
        tag="需登录"
        title="登录 Steam 后同步游戏库"
        description="使用 Steam 账号授权登录（不需要输入密码），登录后可一键同步已拥有游戏、总时长与最近游玩记录。"
      >
        <template #actions>
          <button type="button" class="btn btn--primary" @click="goLogin">Steam 登录</button>
          <router-link class="btn btn--ghost" to="/">先去逛逛</router-link>
        </template>
      </EmptyState>
    </section>

    <!-- 已登录但库里没有数据 -->
    <section v-else-if="!summary.totalGames" class="library-panel">
      <EmptyState
        tag="未同步"
        title="还没有同步游戏库"
        description="点下面的按钮从 Steam 拉取你的已拥有游戏。若同步结果为空，请到 Steam「隐私设置 → 游戏详情」把可见性改成「公开」。"
      >
        <template #actions>
          <button type="button" class="btn btn--primary" :disabled="syncing" @click="onSync">
            {{ syncing ? '同步中…' : '同步游戏库' }}
          </button>
          <router-link class="btn btn--ghost" to="/">先去逛逛</router-link>
        </template>
      </EmptyState>
    </section>

    <!-- 已同步：统计 + 工具条 + 列表 -->
    <template v-else>
      <div class="library-stats">
        <div class="library-stat">
          <span class="library-stat__label">游戏总数</span>
          <strong class="library-stat__value">{{ summary.totalGames }}</strong>
        </div>
        <div class="library-stat">
          <span class="library-stat__label">总时长</span>
          <strong class="library-stat__value">{{ formatMinutes(summary.totalMinutes) }}</strong>
        </div>
        <div class="library-stat">
          <span class="library-stat__label">近两周</span>
          <strong class="library-stat__value">{{ formatMinutes(summary.twoWeeksMinutes) }}</strong>
        </div>
        <div class="library-stat">
          <span class="library-stat__label">站内已收录</span>
          <strong class="library-stat__value">{{ summary.matchedGames }}</strong>
        </div>
      </div>

      <div class="library-toolbar">
        <div class="library-toolbar__left">
          <el-select v-model="sort" class="library-toolbar__select" @change="loadLibrary">
            <el-option label="按时长排序" value="playtime" />
            <el-option label="按名称排序" value="name" />
          </el-select>
          <label class="library-toolbar__switch">
            <el-switch v-model="onlyUnplayed" @change="loadLibrary" />
            <span>只看从未游玩</span>
          </label>
        </div>
        <div class="library-toolbar__right">
          <span v-if="summary.lastSyncedAt" class="library-toolbar__time">
            最近同步 {{ formatDateTime(summary.lastSyncedAt) }}
          </span>
          <button type="button" class="btn btn--ghost" :disabled="syncing" @click="onSync">
            {{ syncing ? '同步中…' : '重新同步' }}
          </button>
          <button type="button" class="btn btn--ghost" @click="onClear">清空</button>
        </div>
      </div>

      <div v-if="loading" class="resource-loading" aria-label="列表加载中">
        <div v-for="row in 3" :key="row" class="resource-loading__row"></div>
      </div>

      <EmptyState
        v-else-if="!items.length"
        tag="没有匹配结果"
        title="当前筛选条件下没有游戏"
        description="换一个排序方式，或关闭「只看从未游玩」再试。"
      />

      <div v-else class="library-list">
        <article
          v-for="item in items"
          :key="item.appid"
          class="library-item library-item--clickable"
          @click="openItem(item)"
        >
          <img v-if="item.coverUrl" class="library-item__cover" :src="item.coverUrl" alt="" loading="lazy" @error="markBrokenCover" />
          <div v-else class="library-item__cover library-item__cover--empty" aria-hidden="true"></div>

          <div class="library-item__main">
            <h3 class="library-item__title">{{ item.gameNameCn || item.title }}</h3>
            <p class="library-item__sub">
              {{ item.gameNameCn ? item.title : `AppID ${item.appid}` }}
            </p>
          </div>

          <div class="library-item__playtime">
            <strong>{{ formatMinutes(item.playtimeForever) }}</strong>
            <span>近两周 {{ formatMinutes(item.playtime2weeks) }}</span>
            <span class="library-item__last">
              {{ item.lastPlayedAt ? `最后游玩 ${formatDateTime(item.lastPlayedAt)}` : '从未游玩' }}
            </span>
          </div>

          <router-link
            class="library-item__link"
            :to="item.gameId ? `/games/${item.gameId}` : `/library/${item.appid}`"
            @click.stop
          >
            {{ item.gameId ? '查看条目' : '查看详情' }}
          </router-link>
        </article>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '@/components/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import { STEAM_LOGIN_URL, clearSteamLibrary, getMyLibrary, syncSteamLibrary } from '@/api/user'
import { formatDateTime, formatMinutes } from '@/utils/format'
import { markBrokenCover } from '@/utils/image'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 库统计，未同步时各项为 0 */
const summary = reactive({
  totalGames: 0,
  totalMinutes: 0,
  twoWeeksMinutes: 0,
  matchedGames: 0,
  lastSyncedAt: ''
})
/** 当前列表 */
const items = ref([])
/** 列表加载中 */
const loading = ref(false)
/** 同步请求进行中 */
const syncing = ref(false)
/** 排序方式：playtime / name */
const sort = ref('playtime')
/** 是否只看从未游玩 */
const onlyUnplayed = ref(false)

/**
 * 跳转 Steam 登录。
 * 请求参数：无
 * 返回参数：无
 */
function goLogin() {
  window.location.href = STEAM_LOGIN_URL
}

/**
 * 拉取我的游戏库（统计 + 列表）。
 * 请求参数：无（读取 sort / onlyUnplayed 的当前值）
 * 返回参数：无；结果写入 summary 与 items，失败时清空列表
 */
async function loadLibrary() {
  if (!userStore.isLogin) return
  loading.value = true
  try {
    const data = await getMyLibrary({
      sort: sort.value,
      filter: onlyUnplayed.value ? 'unplayed' : undefined
    })
    Object.assign(summary, data.summary)
    items.value = data.items || []
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 触发 Steam 库同步。
 * 请求参数：无
 * 返回参数：无；成功后刷新列表并提示游戏数
 */
async function onSync() {
  if (syncing.value) return
  syncing.value = true
  try {
    const result = await syncSteamLibrary()
    Object.assign(summary, result)
    await loadLibrary()
    ElMessage.success(`同步完成，共 ${result.totalGames} 款游戏`)
  } catch {
    // 具体原因（未配置 Key / 隐私设置 / 冷却中）由拦截器提示
  } finally {
    syncing.value = false
  }
}

/**
 * 清空我的游戏库，二次确认后执行。
 * 请求参数：无
 * 返回参数：无；确认后清空服务端数据并刷新页面状态
 */
async function onClear() {
  try {
    await ElMessageBox.confirm('清空后需要重新同步才能恢复，确定清空吗？', '清空游戏库', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    // 用户取消，不做任何处理
    return
  }
  try {
    await clearSteamLibrary()
    items.value = []
    Object.assign(summary, { totalGames: 0, totalMinutes: 0, twoWeeksMinutes: 0, matchedGames: 0, lastSyncedAt: '' })
    ElMessage.success('已清空游戏库')
  } catch {
    // 拦截器已提示
  }
}

/**
 * 打开库内某款游戏的详情：站内已收录跳条目页，未收录跳 Steam 信息页。
 * 请求参数：item —— 库列表项，需含 gameId 与 appid
 * 返回参数：无
 */
function openItem(item) {
  if (item.gameId) {
    router.push(`/games/${item.gameId}`)
  } else {
    router.push(`/library/${item.appid}`)
  }
}

/**
 * 处理登录回跳带来的 query 参数并清理地址栏。
 * 请求参数：无（读取 route.query.login）
 * 返回参数：无
 */
function handleLoginResult() {
  const result = route.query.login
  if (result === 'ok') {
    ElMessage.success('登录成功')
  } else if (result === 'fail') {
    ElMessage.error('Steam 登录校验失败，请重试')
  }
  if (result) {
    router.replace({ query: {} })
  }
}

// 顶部导航点「同步游戏库」后同步完成会派发该事件，这里刷新列表
function onLibrarySyncEvent() {
  loadLibrary()
}

watch(
  () => userStore.isLogin,
  (login) => {
    if (login) loadLibrary()
    else items.value = []
  }
)

onMounted(async () => {
  handleLoginResult()
  if (!userStore.loaded) {
    await userStore.fetchMe()
  }
  if (userStore.isLogin) {
    await loadLibrary()
  }
  window.addEventListener('library:sync', onLibrarySyncEvent)
})

onUnmounted(() => {
  window.removeEventListener('library:sync', onLibrarySyncEvent)
})
</script>

<style scoped>
/* 整行可点：提示可点击，并给键盘聚焦留出可见轮廓 */
.library-item--clickable {
  cursor: pointer;
}

.library-item--clickable:focus-within {
  outline: 2px solid var(--accent-line, rgba(29, 78, 216, 0.35));
  outline-offset: 2px;
}
</style>

