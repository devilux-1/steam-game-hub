<!-- 管理员登录页：调用后端登录接口，成功后保存令牌并进入后台。 -->
<template>
  <div class="page admin-login-page">
    <section class="admin-login">
      <h1 class="page-title">管理员登录</h1>
      <p class="page-subtitle">后台与普通用户账号完全隔离，登录凭据由后端校验。</p>

      <form class="admin-login__form" @submit.prevent="onSubmit">
        <div class="form-field">
          <label class="form-label" for="admin-username">管理员账号</label>
          <el-input
            id="admin-username"
            v-model="form.username"
            placeholder="请输入管理员账号"
            autocomplete="username"
            :disabled="submitting"
          />
        </div>
        <div class="form-field">
          <label class="form-label" for="admin-password">密码</label>
          <el-input
            id="admin-password"
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            autocomplete="current-password"
            :disabled="submitting"
          />
        </div>

        <p v-if="errorMessage" class="form-hint form-hint--warn" role="alert">{{ errorMessage }}</p>

        <button type="submit" class="btn btn--primary admin-login__submit" :disabled="submitting">
          {{ submitting ? '登录中…' : '进入后台' }}
        </button>
      </form>

      <p class="admin-login__note">令牌保存在本地浏览器，失效后接口会返回 401 并自动回到本页重新登录。</p>
      <router-link class="admin-login__back" to="/">← 返回站点首页</router-link>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminLogin } from '@/api/admin'
import { saveAdminSession } from '@/utils/adminAuth'

const route = useRoute()
const router = useRouter()
const form = reactive({ username: '', password: '' })
const submitting = ref(false)
const errorMessage = ref('')

/**
 * 解析登录成功后的回跳地址。
 * 请求参数：无（读取 route.query.redirect）
 * 返回参数：站内后台路径；参数缺失或不是后台路径时回退到游戏管理页，避免被用作站外跳转
 */
function resolveRedirect() {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/admin/')) {
    return redirect
  }
  return '/admin/games'
}

/**
 * 提交管理员登录：校验入参、调用登录接口并保存令牌。
 * 请求参数：无（读取 form.username / form.password）
 * 返回参数：无；字段缺失或登录失败时停留在当前页并展示后端提示
 */
async function onSubmit() {
  if (submitting.value) return
  const username = form.username.trim()
  if (!username || !form.password) {
    ElMessage.warning('请填写管理员账号和密码')
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    const data = await adminLogin({ username, password: form.password })
    saveAdminSession(data?.token, data?.username || username)
    ElMessage.success('登录成功')
    router.push(resolveRedirect())
  } catch (error) {
    // 拦截器已弹出提示，这里保留页面内提示，方便用户对照输入框修改。
    errorMessage.value = error?.response?.data?.message || error?.message || '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

