<script setup lang="ts">
/**
 * 管理页共用的行内搜索表单
 * 搜索项通过默认插槽传入，表单自带「搜索 / 重置」按钮。
 */
const emit = defineEmits<{
  /** 点击搜索 */
  (e: 'search'): void
  /** 点击重置 */
  (e: 'reset'): void
}>()
</script>

<template>
  <a-form layout="inline" @finish="emit('search')">
    <slot />

    <a-form-item>
      <a-button type="primary" html-type="submit">搜索</a-button>
      <a-button style="margin-left: 8px" html-type="button" @click="emit('reset')">重置</a-button>
    </a-form-item>
  </a-form>
</template>
