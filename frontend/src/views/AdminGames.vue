<!-- 后台游戏管理页：分页列表、关键词搜索，以及含别名的游戏新增 / 编辑 / 删除。 -->
<template>
  <div class="page admin-page">
    <header class="page-head">
      <h1 class="page-title">游戏管理</h1>
      <p class="page-subtitle">维护站内游戏、别名以及 Steam / Nexus 绑定信息。</p>
    </header>

    <AdminNav />

    <section class="admin-card">
      <div class="admin-toolbar">
        <el-input
          v-model="searchKeyword"
          class="admin-toolbar__search"
          placeholder="按中文名 / 英文名搜索"
          clearable
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <button type="button" class="btn btn--ghost" @click="onSearch">搜索</button>
        <button type="button" class="btn btn--gold admin-toolbar__primary" @click="openCreate">新增游戏</button>
      </div>

      <EmptyState
        v-if="loadError"
        tag="接口异常"
        title="游戏列表加载失败"
        description="请确认后端已启动、登录未过期后再试。"
      >
        <template #actions>
          <button type="button" class="btn btn--ghost" @click="loadList">重新加载</button>
        </template>
      </EmptyState>

      <template v-else>
        <el-table
          v-loading="loading || preparing"
          :data="rows"
          class="admin-table"
          empty-text="暂无游戏，点击右上角「新增游戏」开始录入"
        >
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column label="游戏名称" min-width="220">
            <template #default="{ row }">
              <p class="admin-table__title">{{ row.nameCn || row.nameEn }}</p>
              <p class="admin-table__sub">{{ row.nameEn }}</p>
            </template>
          </el-table-column>
          <el-table-column label="Steam AppID" width="130">
            <template #default="{ row }">{{ row.steamAppId || '—' }}</template>
          </el-table-column>
          <el-table-column label="资源数" width="90" align="right">
            <template #default="{ row }">{{ row.resourceCount }}</template>
          </el-table-column>
          <el-table-column label="发售日期" width="120">
            <template #default="{ row }">{{ formatDate(row.releaseDate) || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="130" align="right">
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
      :title="editingId ? '编辑游戏' : '新增游戏'"
      width="min(680px, 92vw)"
      class="admin-dialog"
      :close-on-click-modal="false"
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top" class="admin-form">
        <div class="admin-form-grid">
          <el-form-item label="中文名" prop="nameCn">
            <el-input v-model="form.nameCn" placeholder="如：艾尔登法环" maxlength="200" />
          </el-form-item>
          <el-form-item label="英文名（必填）" prop="nameEn">
            <el-input v-model="form.nameEn" placeholder="如：ELDEN RING" maxlength="200" />
          </el-form-item>
          <el-form-item label="Steam AppID" prop="steamAppid">
            <el-input v-model="form.steamAppid" placeholder="如 1245620，可空" />
          </el-form-item>
          <el-form-item label="Nexus domain">
            <el-input v-model="form.nexusDomain" placeholder="如 eldenring，可空" maxlength="100" />
          </el-form-item>
          <el-form-item label="发售日期">
            <el-date-picker
              v-model="form.releaseDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择发售日期"
              class="form-control"
            />
          </el-form-item>
          <el-form-item label="封面图地址">
            <el-input v-model="form.coverUrl" placeholder="https://…，可空" maxlength="500" />
          </el-form-item>
          <el-form-item label="别名（英文逗号分隔）" class="admin-form-grid__full">
            <el-input v-model="form.aliases" placeholder="如：老头环，法环，Elden Ring" maxlength="600" />
          </el-form-item>
          <el-form-item label="简介" class="admin-form-grid__full">
            <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="1000" show-word-limit />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listAdminGames,
  createGame,
  updateGame,
  deleteGame
} from '@/api/admin'
import { getGame } from '@/api/games'
import { normalizeGame } from '@/utils/game'
import { formatDate } from '@/utils/format'
import AdminNav from '@/components/AdminNav.vue'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(false)
const preparing = ref(false)
const loadError = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref(null)

/**
 * 生成空白的游戏表单。
 * 请求参数：无
 * 返回参数：与 GameSaveDTO 字段对应的表单对象；别名用逗号分隔的字符串承接输入
 */
function createForm() {
  return {
    nameCn: '',
    nameEn: '',
    steamAppid: '',
    nexusDomain: '',
    coverUrl: '',
    summary: '',
    releaseDate: '',
    aliases: ''
  }
}

const form = reactive(createForm())

const formRules = {
  nameEn: [{ required: true, message: '英文名不能为空', trigger: 'blur' }],
  // Steam AppID 是可空的数字，用正则保证用户清空后也能通过校验。
  steamAppid: [{ pattern: /^\d*$/, message: 'Steam AppID 只能填写数字', trigger: 'blur' }]
}

/**
 * 把逗号分隔的别名字符串拆成数组。
 * 请求参数：input —— 用户输入的别名字符串，允许为空；支持中英文逗号与分号
 * 返回参数：去空白、去重后的别名字符串数组；无有效别名时返回空数组
 */
function parseAliases(input) {
  if (typeof input !== 'string') return []
  return [...new Set(input.split(/[,，;；]/).map((item) => item.trim()).filter(Boolean))]
}

/**
 * 读取表单文本并去掉首尾空白。
 * 请求参数：value —— 表单字段值，可能不是字符串
 * 返回参数：去空白后的字符串；非字符串输入返回空串
 */
function trimText(value) {
  return typeof value === 'string' ? value.trim() : ''
}

/**
 * 组装游戏保存入参。
 * 请求参数：无（读取 form 与 editingId）
 * 返回参数：GameSaveDTO 结构的普通对象；空文本转 null，别名转为字符串数组
 */
function buildGamePayload() {
  const appIdText = trimText(form.steamAppid)
  return {
    id: editingId.value || undefined,
    nameCn: trimText(form.nameCn),
    nameEn: trimText(form.nameEn),
    steamAppid: appIdText ? Number(appIdText) : null,
    nexusDomain: trimText(form.nexusDomain),
    coverUrl: trimText(form.coverUrl),
    summary: trimText(form.summary),
    releaseDate: form.releaseDate || null,
    aliases: parseAliases(form.aliases)
  }
}

/**
 * 按当前搜索词与页码加载后台游戏列表。
 * 请求参数：无（读取 searchKeyword、page、pageSize）
 * 返回参数：无；失败时标记 loadError 并清空表格数据
 */
async function loadList() {
  loading.value = true
  loadError.value = false
  try {
    const data = await listAdminGames({
      keyword: searchKeyword.value.trim(),
      page: page.value,
      size: pageSize.value
    })
    rows.value = data.items.map(normalizeGame)
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
 * 执行搜索：关键词变化后回到第一页重新查询。
 * 请求参数：无（读取 searchKeyword）
 * 返回参数：无
 */
function onSearch() {
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
 * 打开新增游戏弹窗并清空表单。
 * 请求参数：无
 * 返回参数：无
 */
function openCreate() {
  editingId.value = null
  Object.assign(form, createForm())
  dialogVisible.value = true
}

/**
 * 打开编辑游戏弹窗，并从详情接口补齐别名等列表接口没有的字段。
 * 请求参数：row —— 列表中的游戏行，需含 id
 * 返回参数：无；详情加载失败时不打开弹窗，避免保存时把别名清空
 */
async function openEdit(row) {
  preparing.value = true
  try {
    const detail = await getGame(row.id)
    editingId.value = row.id
    Object.assign(form, {
      nameCn: detail.nameCn ?? '',
      nameEn: detail.nameEn ?? '',
      steamAppid: detail.steamAppid ?? '',
      nexusDomain: detail.nexusDomain ?? detail.nexus_domain ?? '',
      coverUrl: detail.coverUrl ?? '',
      summary: detail.summary ?? '',
      releaseDate: detail.releaseDate ? formatDate(detail.releaseDate) : '',
      aliases: Array.isArray(detail.aliases) ? detail.aliases.join('，') : ''
    })
    dialogVisible.value = true
  } catch {
    // 详情接口失败时提示后放弃编辑，避免把空别名当成用户输入提交、覆盖线上数据。
    ElMessage.error('无法读取游戏详情，请稍后重试')
  } finally {
    preparing.value = false
  }
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
 * 提交游戏表单：编辑调用 PUT，新增调用 POST，成功后刷新列表。
 * 请求参数：无（读取 form 与 editingId）
 * 返回参数：无；校验不通过或接口失败时保留弹窗
 */
async function onSubmit() {
  if (saving.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = buildGamePayload()
    if (editingId.value) {
      await updateGame(editingId.value, payload)
      ElMessage.success('游戏已更新')
    } else {
      await createGame(payload)
      ElMessage.success('游戏已新增')
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
 * 删除游戏：二次确认后调用删除接口，并处理末页删空的情况。
 * 请求参数：row —— 列表中的游戏行，需含 id 与名称
 * 返回参数：无；用户取消或接口失败时不改变列表
 */
async function onDelete(row) {
  const name = row.nameCn || row.nameEn || `游戏 #${row.id}`
  try {
    await ElMessageBox.confirm(
      `确定删除《${name}》吗？该游戏的别名与资源会一并删除，且不可恢复。`,
      '删除游戏',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    // 用户点击取消或关闭确认框，按放弃删除处理。
    return
  }

  try {
    await deleteGame(row.id)
    ElMessage.success('游戏已删除')
    if (rows.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadList()
  } catch {
    // 删除失败由拦截器提示，列表保持原样。
  }
}

// 首次进入后台游戏管理时加载第一页。
onMounted(loadList)
</script>
