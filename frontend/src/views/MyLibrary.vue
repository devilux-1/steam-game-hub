<!-- 我的游戏库：阶段 0 展示未登录/未同步的空状态与同步入口。 -->
<template>
  <div class="page library-page">
    <header class="page-head">
      <h1 class="page-title">我的游戏库</h1>
      <p class="page-subtitle">同步 Steam 已拥有游戏后，按时长排序查看，并直接跳到站内游戏条目。</p>
    </header>

    <section class="library-panel">
      <EmptyState
        tag="需登录"
        title="还没有同步游戏库"
        description="登录 Steam 并同步后，这里会显示总游戏数、总时长、近两周游玩时长与游戏列表。Steam「游戏详情」需设为公开，否则同步结果为空。"
      >
        <template #actions>
          <button type="button" class="btn btn--gold" @click="onSyncClick">同步游戏库</button>
          <router-link class="btn btn--ghost" to="/">先去逛逛</router-link>
        </template>
      </EmptyState>
      <p class="library-panel__foot">阶段 3 接入 Steam OpenID 登录（无密码）与 IPlayerService 游戏库同步。</p>
    </section>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import EmptyState from '@/components/EmptyState.vue'

/**
 * 「同步游戏库」占位动作：同步能力属于阶段 3，当前仅提示。
 * 请求参数：无
 * 返回参数：无
 */
function onSyncClick() {
  ElMessage.info('阶段 3 接入 Steam 登录与游戏库同步')
}
</script>
