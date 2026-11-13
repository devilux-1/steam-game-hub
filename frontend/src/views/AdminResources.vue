<!-- 后台资源管理页：按游戏筛选的分页列表，以及资源新增 / 编辑 / 删除。 -->
<template>
  <div class="page admin-page">
    <header class="page-head">
      <h1 class="page-title">资源管理</h1>
      <p class="page-subtitle">按 Nexus Mod / 汉化补丁 / 风灵月影三类维护资源条目。</p>
    </header>

    <AdminNav />

    <section class="admin-card">
      <div class="admin-toolbar">
        <el-select
          v-model="gameFilter"
          class="admin-toolbar__select"
          placeholder="全部游戏"
          clearable
          :loading="optionsLoading"
          @change="onFilterChange"
        >
          <el-option v-for="item in gameOptions" :key="item.id" :label="item.label" :value="String(item.id)" />
        </el-select>
        <p class="admin-toolbar__hint">{{ filterHint }}</p>
        <button type="button" class="btn btn--gold admin-toolbar__primary" @click="openCreate">新增资源</button>
      </div>

      <p v-if="optionsError" class="form-hint form-hint--warn">
        游戏下拉选项加载失败，暂时无法新增资源；确认后端可用后可刷新页面重试。
      </p>

      <EmptyState
        v-if="loadError"
        tag="接口异常"
        title="资源列表加载失败"
        description="请确认后端已启动、登录未过期后再试。"
      >
        <template #actions>
          <button type="button" class="btn btn--ghost" @click="loadList">重新加载</button>
        </template>
      </EmptyState>

      <template v-else>
        <el-table v-loading="loading" :data="rows" class="admin-table" :empty-text="emptyText">
          <el-table-column label="资源名称" min-width="200">
            <template #default="{ row }">
              <p class="admin-table__title">{{ row.title }}</p>
              <p class="admin-table__sub">{{ row.description || '暂无说明' }}</p>
            </template>
          </el-table-column>
          <!-- 类型列留足宽度，避免「Nexus Mod」被单元格省略号截断 -->
          <el-table-column label="类型" width="128">
            <template #default="{ row }">
              <span class="type-pill">{{ resourceTypeLabel(row.type) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="版本" width="96">
            <template #default="{ row }">{{ row.version || '—' }}</template>
          </el-table-column>
          <el-table-column label="适配版本" width="104">
            <template #default="{ row }">{{ row.compatibleVersion || '—' }}</template>
          </el-table-column>
          <el-table-column label="作者" width="120">
            <template #default="{ row }">{{ row.author || '—' }}</template>
          </el-table-column>
          <el-table-column label="来源站" width="96">
            <template #default="{ row }">{{ row.sourceSite || '—' }}</template>
          </el-table-column>
          <el-table-column label="点击量" width="84" align="right">
            <template #default="{ row }">{{ row.clickCount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <span class="status-pill" :class="row.status === 0 ? 'status-pill--off' : 'status-pill--on'">
                {{ row.status === 0 ? '下架' : '上架' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="140">
            <template #default="{ row }">{{ formatDateTime(row.updatedAt) || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="right">
            <template #default="{ row }">
              <button type="button" class="link-btn" @click="openEdit(row)">编辑</button>
              <button type="button" class="link-btn link-btn--danger" @click="onDelete(row)">删除</button>
            </template>
          </el-table-column>
        </el-table>

        <div class="admin-pagination">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="page"
            @current-change="onPageChange"
          />
        </div>
      </template>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑资源' : '新增资源'"
      width="min(720px, 92vw)"
      class="admin-dialog"
      :close-on-click-modal="false"
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top" class="admin-form">
        <div class="admin-form-grid">
          <el-form-item label="所属游戏（必填）" prop="gameId">
            <el-select v-model="form.gameId" placeholder="选择所属游戏" class="form-control" :loading="optionsLoading">
              <el-option v-for="item in gameOptions" :key="item.id" :label="item.label" :value="String(item.id)" />
            </el-select>
          </el-form-item>
          <el-form-item label="资源类型（必填）" prop="type">
            <el-select v-model="form.type" class="form-control">
              <el-option v-for="item in RESOURCE_TYPES" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="资源名称（必填）" prop="title" class="admin-form-grid__full">
            <el-input v-model="form.title" placeholder="如：画质增强 ReShade 预设" maxlength="300" />
          </el-form-item>
          <el-form-item label="版本">
            <el-input v-model="form.version" placeholder="如 v1.2.3，可空" maxlength="50" />
          </el-form-item>
          <el-form-item label="适配游戏版本">
            <el-input v-model="form.compatibleVersion" placeholder="如 1.10.1，可空" maxlength="100" />
          </el-form-item>
          <el-form-item label="作者 / 汉化组">
            <el-input v-model="form.author" placeholder="可空" maxlength="100" />
          </el-form-item>
          <el-form-item label="来源站">
            <el-input v-model="form.sourceSite" placeholder="如 Nexus / 3DM / 风灵月影" maxlength="50" />
          </el-form-item>
          <el-form-item label="跳转地址（必填）" prop="sourceUrl" class="admin-form-grid__full">
            <el-input v-model="form.sourceUrl" placeholder="https://…" maxlength="1000" />
          </el-form-item>
          <el-form-item label="一句话说明" class="admin-form-grid__full">
            <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item label="安装要点" class="admin-form-grid__full">
            <el-input v-model="form.installNote" type="textarea" :rows="2" maxlength="1000" show-word-limit />
          </el-form-item>
          <el-form-item label="排序权重（越大越靠前）">
            <el-input-number v-model="form.sortWeight" :min="0" :max="99999" :controls="false" class="form-control" />
          </el-form-item>
          <el-form-item label="上下架">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <button type="button" class="btn btn--ghost" :disabled="saving" @click="dialogVisible = false">取消</button>
        <button type="button" class="btn btn--gold" :disabled="saving" @click="onSubmit">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getGameOptions,
  listAdminResources,
  createResource,
  updateResource,
  deleteResource
} from '@/api/admin'
import { RESOURCE_TYPES, resourceTypeLabel, normalizeResource } from '@/utils/resource'
import { formatDateTime } from '@/utils/format'
import AdminNav from '@/components/AdminNav.vue'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(false)
const loadError = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const optionsLoading = ref(false)
const optionsError = ref(false)
const gameOptions = ref([])
const gameFilter = ref('')

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref(null)

/**
 * 生成空白的资源表单。
 * 请求参数：无
 * 返回参数：与 ResourceSaveDTO 字段对应的表单对象；状态默认上架、权重默认 0
 */
function createForm() {
  return {
    gameId: '',
    type: 'NEXUS_MOD',
    title: '',
    version: '',
    compatibleVersion: '',
    author: '',
    sourceSite: '',
    sourceUrl: '',
    description: '',
    installNote: '',
    sortWeight: 0,
    status: 1
  }
}

const form = reactive(createForm())

const formRules = {
  gameId: [{ required: true, message: '请选择所属游戏', trigger: 'change' }],
  type: [{ required: true, message: '请选择资源类型', trigger: 'change' }],
  title: [{ required: true, message: '资源名称不能为空', trigger: 'blur' }],
  sourceUrl: [{ required: true, message: '跳转地址不能为空', trigger: 'blur' }]
}

/**
 * 当前筛选说明，帮助用户理解表格展示范围。
 * 请求参数：无（读取 optionsLoading、gameOptions、gameFilter）
 * 返回参数：提示文本；无需提示时返回空串
 */
const filterHint = computed(() => {
  if (optionsLoading.value) return '游戏列表加载中…'
  if (!gameOptions.value.length) return '还没有可选游戏，请先在「游戏管理」中新增游戏'
  if (!gameFilter.value) return '当前展示全部游戏的资源'
  const current = gameOptions.value.find((item) => String(item.id) === gameFilter.value)
  return current ? `当前展示《${current.label}》的资源` : ''
})

/**
 * 表格空数据文案，区分「没有游戏」与「当前筛选无资源」。
 * 请求参数：无（读取 gameOptions、gameFilter）
 * 返回参数：空状态提示字符串
 */
const emptyText = computed(() => {
  if (!gameOptions.value.length) return '请先在「游戏管理」中新增游戏，再回来录入资源'
  if (!gameFilter.value) return '站内还没有资源，点击右上角「新增资源」开始录入'
  return '该游戏暂无资源，点击右上角「新增资源」开始录入'
})

/**
 * 读取表单文本并去掉首尾空白。
 * 请求参数：value —— 表单字段值，可能不是字符串
 * 返回参数：去空白后的字符串；非字符串输入返回空串
 */
function trimText(value) {
  return typeof value === 'string' ? value.trim() : ''
}

/**
 * 加载游戏下拉选项；首次加载后默认选中第一款游戏，保证编辑时能确定所属游戏。
 * 请求参数：无
 * 返回参数：无；失败时标记 optionsError 并禁用后续新增（下拉为空）
 */
async function loadOptions() {
  optionsLoading.value = true
  optionsError.value = false
  try {
    const data = await getGameOptions()
    gameOptions.value = (Array.isArray(data) ? data : []).map((item) => ({
      id: item.id,
      label: item.nameCn || item.nameEn || `游戏 #${item.id}`
    }))
    if (gameOptions.value.length && !gameFilter.value) {
      gameFilter.value = String(gameOptions.value[0].id)
    }
  } catch {
    optionsError.value = true
    gameOptions.value = []
  } finally {
    optionsLoading.value = false
  }
}

/**
 * 按当前游戏筛选与页码加载资源列表。
 * 请求参数：无（读取 gameFilter、page、pageSize）
 * 返回参数：无；失败时标记 loadError 并清空表格数据
 */
async function loadList() {
  loading.value = true
  loadError.value = false
  try {
    const data = await listAdminResources({
      gameId: gameFilter.value || '',
      page: page.value,
      size: pageSize.value
    })
    rows.value = data.items.map((item) => ({
      ...normalizeResource(item),
      // 后台列表已返回所属游戏、排序权重与状态，归一化后供表格与编辑弹窗直接使用。
      gameId: item.gameId ?? item.game_id ?? '',
      sortWeight: Number(item.sortWeight ?? item.sort_weight ?? 0) || 0,
      status: item.status === 0 || item.status === '0' ? 0 : 1
    }))
    total.value = data.total
    // 以响应页码为准，避免删除末页数据后停留在不存在的页码。
    page.value = data.page || page.value
  } catch {
    rows.value = []
    total.value = 0
    loadError.value = true
  } finally {
    loading.value = false
  }
}

/**
 * 切换游戏筛选后回到第一页重新查询。
 * 请求参数：无（读取 gameFilter）
 * 返回参数：无
 */
function onFilterChange() {
  page.value = 1
  loadList()
}

/**
 * 切换分页。
 * 请求参数：value —— 目标页码，从 1 开始
 * 返回参数：无
 */
function onPageChange(value) {
  page.value = value
  loadList()
}

/**
 * 打开新增资源弹窗并清空表单，所属游戏默认取当前筛选。
 * 请求参数：无
 * 返回参数：无
 */
function openCreate() {
  editingId.value = null
  Object.assign(form, createForm(), { gameId: gameFilter.value ? String(gameFilter.value) : '' })
  dialogVisible.value = true
}

/**
 * 打开编辑资源弹窗并回填当前行数据。
 * 请求参数：row —— 列表中的资源行，含 gameId、sortWeight、status 等后台字段
 * 返回参数：无
 */
function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, createForm(), {
    gameId: row.gameId ? String(row.gameId) : '',
    type: row.type || 'NEXUS_MOD',
    title: row.title,
    version: row.version,
    compatibleVersion: row.compatibleVersion,
    author: row.author,
    sourceSite: row.sourceSite,
    sourceUrl: row.sourceUrl,
    description: row.description,
    installNote: row.installNote,
    sortWeight: Number(row.sortWeight) || 0,
    status: row.status === 0 || row.status === '0' ? 0 : 1
  })
  dialogVisible.value = true
}

/**
 * 关闭弹窗后清理表单与校验状态。
 * 请求参数：无
 * 返回参数：无
 */
function onDialogClosed() {
  editingId.value = null
  Object.assign(form, createForm())
  formRef.value?.clearValidate()
}

/**
 * 组装资源保存入参。
 * 请求参数：无（读取 form 与 editingId）
 * 返回参数：ResourceSaveDTO 结构的普通对象，gameId、status、sortWeight 一并下发
 */
function buildResourcePayload() {
  return {
    id: editingId.value || undefined,
    gameId: Number(form.gameId),
    type: form.type,
    title: trimText(form.title),
    version: trimText(form.version),
    compatibleVersion: trimText(form.compatibleVersion),
    author: trimText(form.author),
    sourceSite: trimText(form.sourceSite),
    sourceUrl: trimText(form.sourceUrl),
    description: trimText(form.description),
    installNote: trimText(form.installNote),
    sortWeight: Number(form.sortWeight) || 0,
    status: form.status === 0 ? 0 : 1
  }
}

/**
 * 提交资源表单：编辑调用 PUT，新增调用 POST，成功后刷新列表。
 * 请求参数：无（读取 form 与 editingId）
 * 返回参数：无；校验不通过或接口失败时保留弹窗
 */
async function onSubmit() {
  if (saving.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = buildResourcePayload()
    if (editingId.value) {
      await updateResource(editingId.value, payload)
      ElMessage.success('资源已更新')
    } else {
      await createResource(payload)
      ElMessage.success('资源已新增')
    }
    dialogVisible.value = false
    await loadList()
  } catch {
    // 后端校验信息已由拦截器提示，保留弹窗让用户继续修改。
  } finally {
    saving.value = false
  }
}

/**
 * 删除资源：二次确认后调用删除接口，并处理末页删空的情况。
 * 请求参数：row —— 列表中的资源行，需含 id 与 title
 * 返回参数：无；用户取消或接口失败时不改变列表
 */
async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除资源「${row.title}」吗？删除后游戏条目页将不再展示。`, '删除资源', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    // 用户点击取消或关闭确认框，按放弃删除处理。
    return
  }

  try {
    await deleteResource(row.id)
    ElMessage.success('资源已删除')
    if (rows.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadList()
  } catch {
    // 删除失败由拦截器提示，列表保持原样。
  }
}

// 首次进入先取游戏选项，再按默认游戏加载资源列表。
onMounted(async () => {
  await loadOptions()
  await loadList()
})
</script>
