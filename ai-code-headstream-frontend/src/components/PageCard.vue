<script setup lang="ts">
/**
 * 表单页通用卡片
 * 统一卡片标题、加载态与最大宽度，内容通过默认插槽传入。
 */
withDefaults(
  defineProps<{
    /** 卡片标题 */
    title: string
    /** 加载中 */
    loading?: boolean
    /** 卡片最大宽度（px） */
    maxWidth?: number
  }>(),
  {
    loading: false,
    maxWidth: 640,
  },
)
</script>

<template>
  <a-card
    :title="title"
    :loading="loading"
    :style="{ maxWidth: `${maxWidth}px`, margin: '0 auto' }"
  >
    <slot />
  </a-card>
</template>
