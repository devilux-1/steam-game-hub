<!-- 管理员登录页：阶段 0 为占位登录，只写入本地令牌以便演示路由守卫。 -->
<template>
  <div class="page admin-login-page">
    <section class="admin-login">
      <p class="eyebrow">ADMIN CONSOLE</p>
      <h1 class="page-title">管理员登录</h1>
      <p class="page-subtitle">后台与普通用户账号完全隔离。阶段 0 为占位登录，不会校验真实凭据。</p>

      <form class="admin-login__form" @submit.prevent="onSubmit">
        <div class="form-field">
          <label class="form-label" for="admin-username">管理员账号</label>
          <el-input
            id="admin-username"
            v-model="form.username"
            placeholder="任意填写即可（占位）"
            autocomplete="username"
          />
        </div>
        <div class="form-field">
          <label class="form-label" for="admin-password">密码</label>
          <el-input
            id="admin-password"
            v-model="form.password"
            type="password"
            show-password
            placeholder="任意填写即可（占位）"
            autocomplete="current-password"
          />
        </div>
        <button type="submit" class="btn btn--gold admin-login__submit">进入后台（占位登录）</button>
      </form>

      <p class="admin-login__note">阶段 1 接入 /api/admin/auth/login 后，这里会校验账号密码并写入真实管理员凭据。</p>
      <router-link class="admin-login__back" to="/">← 返回站点首页</router-link>
    </section>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { saveAdminToken } from '@/utils/adminAuth'

const route = useRoute()
const router = useRouter()
const form = reactive({ username: '', password: '' })

/**
 * 提交占位登录：两项非空时写入本地令牌，并跳转后台首页。
 * 请求参数：无（读取 form.username / form.password）
 * 返回参数：无；字段缺失时提示并停留当前页
 */
function onSubmit() {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请填写管理员账号和密码')
    return
  }
  saveAdminToken()
  ElMessage.success('占位登录成功')
  // 只允许回跳到后台路径，避免 redirect 参数被用作任意站外跳转。
  const redirect =
    typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/admin/')
      ? route.query.redirect
      : '/admin/games'
  router.push(redirect)
}
</script>
