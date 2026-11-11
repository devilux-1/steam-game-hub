<!-- 通用空状态：用于无数据、接口未接入或占位说明，操作按钮通过 actions 插槽扩展。 -->
<template>
  <div class="empty-state">
    <span class="empty-state__icon" aria-hidden="true">
      <svg viewBox="0 0 48 48" fill="none">
        <rect x="7.5" y="7.5" width="33" height="33" rx="8" stroke="currentColor" stroke-width="1.4" opacity="0.35" />
        <path
          d="M18.5 29.5h11M24 15.5v9"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linecap="round"
          opacity="0.25"
        />
        <path
          d="M17 19.5h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H17a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2Z"
          stroke="currentColor"
          stroke-width="1.5"
        />
        <path d="M19.5 19.5v-1.8a4.5 4.5 0 0 1 9 0v1.8" stroke="currentColor" stroke-width="1.5" />
        <circle cx="20.5" cy="25" r="1" fill="currentColor" />
        <circle cx="27.5" cy="25" r="1" fill="currentColor" />
      </svg>
    </span>
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
