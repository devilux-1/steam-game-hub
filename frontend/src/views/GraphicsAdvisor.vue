<!-- 画质顾问页：GPU 档位、分辨率、游戏三项输入 + 推荐结果占位区 + 固定免责说明。 -->
<template>
  <div class="page advisor-page">
    <header class="page-head">
      <p class="eyebrow">GRAPHICS ADVISOR</p>
      <h1 class="page-title">画质顾问</h1>
      <p class="page-subtitle">
        选好显卡、分辨率与游戏，拿到「稳住 60 帧」的逐项画质建议。阶段 4 接入真实算法，当前展示输入与结果区结构。
      </p>
    </header>

    <div class="advisor-layout">
      <section class="advisor-form">
        <h2 class="panel-title">配置</h2>

        <div class="form-field">
          <label class="form-label" for="advisor-game">游戏</label>
          <el-select
            id="advisor-game"
            v-model="form.gameId"
            class="form-control"
            filterable
            :loading="gamesLoading"
            :disabled="gamesError"
            :placeholder="gamesError ? '游戏列表不可用' : '选择要调整的游戏'"
            no-data-text="暂无可选游戏"
          >
            <el-option v-for="item in gameOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <p v-if="gamesError" class="form-hint form-hint--warn">游戏列表加载失败，请确认后端已启动。</p>
        </div>

        <div class="form-field">
          <label class="form-label" for="advisor-gpu">GPU 档位</label>
          <el-select id="advisor-gpu" v-model="form.gpuTier" class="form-control" placeholder="选择你的显卡档位">
            <el-option v-for="tier in GPU_TIERS" :key="tier.value" :label="tier.label" :value="tier.value" />
          </el-select>
        </div>

        <div class="form-field">
          <label class="form-label" for="advisor-resolution">分辨率</label>
          <el-select id="advisor-resolution" v-model="form.resolution" class="form-control" placeholder="选择显示器分辨率">
            <el-option v-for="item in RESOLUTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>

        <button type="button" class="btn btn--gold advisor-form__submit" @click="generateRecommendation">
          生成画质建议
        </button>
        <p class="form-hint">输入不保存，刷新页面后需要重新选择。</p>
      </section>

      <section class="advisor-result">
        <header class="advisor-result__head">
          <h2 class="panel-title">推荐结果</h2>
          <span v-if="submitted" class="tag-pill">占位结果</span>
        </header>

        <EmptyState
          v-if="!submitted"
          tag="阶段 4"
          title="尚未生成建议"
          description="左侧选择游戏、GPU 档位与分辨率后点击「生成画质建议」，逐项画质档位与预估帧率会展示在这里。"
        />

        <template v-else>
          <div class="result-summary">
            <p class="result-summary__label">预估帧率区间</p>
            <p class="result-summary__fps">-- <span>FPS</span></p>
            <p class="result-summary__meta">{{ selectedGameLabel }} · {{ selectedGpuLabel }} · {{ form.resolution }}</p>
          </div>

          <ul class="result-options">
            <li v-for="row in resultRows" :key="row.name" class="result-option">
              <span class="result-option__name">{{ row.name }}</span>
              <span class="result-option__value">{{ row.hint }}</span>
            </li>
          </ul>

          <p class="result-placeholder-note">以上为结果区结构占位，真实推荐将在阶段 4 由后端画质算法返回。</p>
        </template>

        <p class="advisor-disclaimer">
          <strong>免责说明：</strong>预估值来自社区数据推算，实际帧率受 CPU、内存、驱动版本与游戏场景影响，建议结合实测调整。
        </p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { searchGames } from '@/api/games'
import { normalizeGame } from '@/utils/game'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()

// GPU 档位为预置列表，编码与后端 fps_baseline.gpu_tier 保持一致。
const GPU_TIERS = [
  { value: 'GTX1660S', label: 'GTX 1660 SUPER · 1080P 入门' },
  { value: 'RTX3060', label: 'RTX 3060 · 1080P 主力' },
  { value: 'RTX4060', label: 'RTX 4060 · 1080P 流畅' },
  { value: 'RTX4070', label: 'RTX 4070 · 2K 流畅' },
  { value: 'RTX4080', label: 'RTX 4080 · 2K 高画质' },
  { value: 'RTX4090', label: 'RTX 4090 · 4K 旗舰' }
]

// 分辨率取值与后端 fps_baseline.resolution 枚举一致。
const RESOLUTIONS = [
  { value: '1080P', label: '1080P（1920 × 1080）' },
  { value: '2K', label: '2K（2560 × 1440）' },
  { value: '4K', label: '4K（3840 × 2160）' }
]

// 结果区占位行，对应设计文档的逐项推荐；真实档位由阶段 4 接口填充。
const resultRows = [
  { name: '整体预设', hint: '待计算' },
  { name: '阴影质量', hint: '待计算' },
  { name: '纹理质量', hint: '待计算' },
  { name: '体积雾 / 环境光遮蔽', hint: '待计算' },
  { name: '光线追踪', hint: '建议关闭' }
]

const form = reactive({ gameId: '', gpuTier: '', resolution: '1080P' })
const games = ref([])
const gamesLoading = ref(false)
const gamesError = ref(false)
const submitted = ref(false)

/**
 * 游戏下拉选项：把接口数据转换成 Element Plus 需要的 { value, label }。
 * 请求参数：无（读取 games）
 * 返回参数：选项数组，value 为字符串 ID，label 优先中文名
 */
const gameOptions = computed(() =>
  games.value.map((item) => ({
    value: String(item.id),
    label: item.nameCn || item.nameEn || `游戏 #${item.id}`
  }))
)

/**
 * 当前选中游戏的展示名，用于结果摘要。
 * 请求参数：无（读取 form.gameId 与 games）
 * 返回参数：游戏名称；未匹配到时返回「游戏 #ID」，未选择时返回「未选择游戏」
 */
const selectedGameLabel = computed(() => {
  const target = games.value.find((item) => String(item.id) === form.gameId)
  if (target) return target.nameCn || target.nameEn || `游戏 #${target.id}`
  return form.gameId ? `游戏 #${form.gameId}` : '未选择游戏'
})

/**
 * 当前选中 GPU 档位的展示名，用于结果摘要。
 * 请求参数：无（读取 form.gpuTier）
 * 返回参数：档位标签；未选中时返回「未选择显卡」
 */
const selectedGpuLabel = computed(() => {
  const target = GPU_TIERS.find((item) => item.value === form.gpuTier)
  return target ? target.label : '未选择显卡'
})

/**
 * 把路由中的游戏 ID 预选到下拉框；接口结果里没有该游戏时补一个展示用选项。
 * 请求参数：无（读取 route.params.id）
 * 返回参数：无
 */
function preselectRouteGame() {
  const routeId = route.params.id
  if (!routeId) return
  const id = String(routeId)
  form.gameId = id
  // 游戏可能不在本次分页结果内，补一个占位选项避免下拉显示为空白。
  if (!games.value.some((item) => String(item.id) === id)) {
    games.value = [{ id, nameCn: `游戏 #${id}`, nameEn: '' }, ...games.value]
  }
}

/**
 * 加载可选游戏列表，并在完成后预选路由中的游戏。
 * 请求参数：无
 * 返回参数：无；失败时标记 gamesError，下拉框禁用并提示
 */
async function loadGames() {
  gamesLoading.value = true
  gamesError.value = false
  try {
    const { items } = await searchGames({ keyword: '', page: 1, size: 100 })
    games.value = items.map(normalizeGame).filter((item) => item.id !== '')
  } catch {
    // 列表接口不可用时仍保留页面，禁用下拉并提示，避免用户误以为站内没有游戏。
    gamesError.value = true
    games.value = []
  } finally {
    gamesLoading.value = false
  }
  preselectRouteGame()
}

/**
 * 校验输入并切换到推荐结果占位区。
 * 请求参数：无（读取 form）
 * 返回参数：无；缺少必填项时用消息提示并停留在表单
 */
function generateRecommendation() {
  if (!form.gameId || !form.gpuTier || !form.resolution) {
    ElMessage.warning('请先选择游戏、GPU 档位与分辨率')
    return
  }
  submitted.value = true
}

// 顾问页需要真实游戏列表做下拉选项，挂载时加载一次。
onMounted(loadGames)
</script>
