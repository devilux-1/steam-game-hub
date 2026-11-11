<!-- 后台游戏管理占位页：阶段 1 接入游戏与别名的 CRUD。 -->
<template>
  <div class="page admin-page">
    <header class="page-head">
      <p class="eyebrow">ADMIN · GAMES</p>
      <h1 class="page-title">游戏管理</h1>
      <p class="page-subtitle">维护站内游戏、别名以及 Steam / Nexus 绑定信息。</p>
    </header>

    <AdminNav />

    <section class="admin-card">
      <EmptyState
        tag="阶段 1"
        title="游戏管理尚未接入"
        description="阶段 1 在这里提供：新增 / 编辑 / 删除游戏，维护别名，绑定 Steam AppID 与 Nexus domain。"
      />
    </section>
  </div>
</template>

<script setup>
import AdminNav from '@/components/AdminNav.vue'
import EmptyState from '@/components/EmptyState.vue'
</script>
