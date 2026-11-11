<!-- 后台资源管理占位页：阶段 1 接入资源录入、排序与上下架。 -->
<template>
  <div class="page admin-page">
    <header class="page-head">
      <p class="eyebrow">ADMIN · RESOURCES</p>
      <h1 class="page-title">资源管理</h1>
      <p class="page-subtitle">按 Nexus Mod / 汉化补丁 / 风灵月影三类维护资源条目。</p>
    </header>

    <AdminNav />

    <section class="admin-card">
      <EmptyState
        tag="阶段 1"
        title="资源管理尚未接入"
        description="阶段 1 在这里提供：按类型录入资源、调整排序权重、上下架，并维护外链与安装要点。"
      />
    </section>
  </div>
</template>

<script setup>
import AdminNav from '@/components/AdminNav.vue'
import EmptyState from '@/components/EmptyState.vue'
</script>
