<script setup lang="ts">
import { reactive } from 'vue'

/**
 * 管理页共用的行内搜索表单
 * 搜索项通过默认插槽传入，表单自带「搜索 / 重置」按钮。
 *
 * Ant Design Vue 只有在传入 model 时才会触发 finish，
 * 具体查询值由各搜索项自己的 v-model 维护，这里只提供一个空模型。
 */
const formModel = reactive<Record<string, unknown>>({})

const emit = defineEmits<{
  /** 点击搜索 */
  (e: 'search'): void
  /** 点击重置 */
  (e: 'reset'): void
}>()
</script>

<template>
  <a-form layout="inline" :model="formModel" @finish="emit('search')">
    <slot />

    <a-form-item>
      <a-button type="primary" html-type="submit">搜索</a-button>
      <a-button style="margin-left: 8px" html-type="button" @click="emit('reset')">重置</a-button>
    </a-form-item>
  </a-form>
</template>
