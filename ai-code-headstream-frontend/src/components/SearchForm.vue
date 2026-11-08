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
  <a-form class="search-form" layout="inline" :model="formModel" @finish="emit('search')">
    <slot />

    <a-form-item class="search-form__actions">
      <a-button type="primary" html-type="submit">搜索</a-button>
      <a-button html-type="button" @click="emit('reset')">重置</a-button>
    </a-form-item>
  </a-form>
</template>

<style scoped>
.search-form.ant-form-inline {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px 20px;
  margin-bottom: 16px;
}

.search-form :deep(.ant-form-item) {
  margin-inline-end: 0;
  margin-bottom: 0;
}

.search-form :deep(.ant-form-item-label) {
  width: 84px;
}

.search-form :deep(.ant-form-item-label > label) {
  height: 32px;
}

.search-form :deep(.ant-form-item-control) {
  flex: 1;
  min-width: 0;
}

.search-form :deep(.ant-input),
.search-form :deep(.ant-input-number),
.search-form :deep(.ant-select) {
  width: 100%;
}

.search-form__actions {
  grid-column: 1 / -1;
}

@media (max-width: 575px) {
  .search-form :deep(.ant-form-item-row) {
    flex-wrap: wrap;
  }

  .search-form :deep(.ant-form-item-label),
  .search-form :deep(.ant-form-item-control) {
    flex: 0 0 100%;
    width: 100%;
    max-width: 100%;
  }
}

.search-form__actions :deep(.ant-form-item-control-input-content) {
  display: flex;
  gap: 8px;
}
</style>
