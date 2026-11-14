<!-- 后台公用导航：在管理页面之间切换，展示当前管理员并退出登录。 -->
<template>
  <nav class="admin-nav" aria-label="后台导航">
    <router-link v-for="item in adminMenus" :key="item.name" :to="{ name: item.name }" class="admin-nav__link">
      {{ item.label }}
    </router-link>
    <div class="admin-nav__right">
      <span v-if="username" class="admin-nav__user">{{ username }}</span>
      <button type="button" class="admin-nav__logout" @click="onLogout">退出登录</button>
    </div>
  </nav>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { clearAdminSession, getAdminUsername } from '@/utils/adminAuth'

const router = useRouter()

// 登录用户名只在进入后台时读取一次，令牌失效后由 401 拦截器统一跳回登录页。
const username = getAdminUsername()

// 后台菜单与路由表一一对应。
const adminMenus = [
  { name: 'admin-games', label: '游戏管理' },
  { name: 'admin-resources', label: '资源管理' }
]

/**
 * 退出后台登录：清理本地令牌与用户名并回到登录页。
 * 请求参数：无
 * 返回参数：无
 */
function onLogout() {
  clearAdminSession()
  ElMessage.success('已退出登录')
  router.push({ name: 'admin-login' })
}
</script>
