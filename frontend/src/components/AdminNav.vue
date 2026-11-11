<!-- 后台占位页公用导航：在管理页面之间切换，并可退出占位登录。 -->
<template>
  <nav class="admin-nav" aria-label="后台导航">
    <router-link v-for="item in adminMenus" :key="item.name" :to="{ name: item.name }" class="admin-nav__link">
      {{ item.label }}
    </router-link>
    <button type="button" class="admin-nav__logout" @click="onLogout">退出占位登录</button>
  </nav>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { clearAdminToken } from '@/utils/adminAuth'

const router = useRouter()

// 后台菜单与路由表一一对应，阶段 0 只覆盖三类管理页。
const adminMenus = [
  { name: 'admin-games', label: '游戏管理' },
  { name: 'admin-resources', label: '资源管理' },
  { name: 'admin-graphics', label: '画质数据' }
]

/**
 * 退出后台占位登录并回到管理员登录页。
 * 请求参数：无
 * 返回参数：无
 */
function onLogout() {
  clearAdminToken()
  ElMessage.success('已退出后台占位登录')
  router.push({ name: 'admin-login' })
}
</script>
