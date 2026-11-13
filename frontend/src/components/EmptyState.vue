<!-- 通用空状态：用于无数据、接口未接入或占位说明，操作按钮通过 actions 插槽扩展。 -->
<template>
  <div class="empty-state">
    <p v-if="tag" class="empty-state__tag">{{ tag }}</p>
    <h3 class="empty-state__title">{{ title }}</h3>
    <p v-if="description" class="empty-state__desc">{{ description }}</p>
    <div v-if="$slots.actions" class="empty-state__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
// 空状态组件只负责展示，不发起请求；空数据判断与到位后的操作由调用页面负责。
defineProps({
  /** 空状态主标题，必填 */
  title: { type: String, required: true },
  /** 补充说明，说明为什么为空以及后续会有什么，可空 */
  description: { type: String, default: '' },
  /** 左上角小标签，如 STAGE 2 / 待录入，可空 */
  tag: { type: String, default: '' }
})
</script>
