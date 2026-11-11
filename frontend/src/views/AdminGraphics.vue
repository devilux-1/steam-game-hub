<!-- 后台画质数据占位页：阶段 4 接入画质选项与基准帧率维护。 -->
<template>
  <div class="page admin-page">
    <header class="page-head">
      <p class="eyebrow">ADMIN · GRAPHICS</p>
      <h1 class="page-title">画质数据</h1>
      <p class="page-subtitle">维护画质选项、权重与各 GPU / 分辨率下的基准帧率。</p>
    </header>

    <AdminNav />

    <section class="admin-card">
      <EmptyState
        tag="阶段 4"
        title="画质数据维护尚未接入"
        description="阶段 4 在这里提供：画质选项与档位维护、视觉收益与性能代价权重、基准帧率以及帧率反馈校准结果。"
      />
    </section>
  </div>
</template>

<script setup>
import AdminNav from '@/components/AdminNav.vue'
import EmptyState from '@/components/EmptyState.vue'
</script>
