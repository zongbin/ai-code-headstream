<script setup lang="ts">
import type { Component } from 'vue'

/**
 * 暂未开放的工具按钮（上传、编辑、优化提示词等）
 * 统一外观与禁用提示，避免各页面重复实现。
 */
withDefaults(
  defineProps<{
    /** 图标组件 */
    icon: Component
    /** 按钮文案 */
    label: string
    /** 悬浮提示，默认为「xx功能暂未开放」 */
    tooltip?: string
    /** 是否使用带底色的样式 */
    filled?: boolean
  }>(),
  {
    tooltip: '',
    filled: false,
  },
)
</script>

<template>
  <a-tooltip :title="tooltip || `${label}功能暂未开放`">
    <span class="tool-button__wrap">
      <a-button
        type="text"
        class="tool-button"
        :class="{ 'tool-button--filled': filled }"
        disabled
        :aria-label="label"
      >
        <component :is="icon" />
        <span>{{ label }}</span>
      </a-button>
    </span>
  </a-tooltip>
</template>

<style scoped>
.tool-button__wrap {
  display: inline-flex;
  line-height: 0;
}

.tool-button {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  height: 32px;
  padding: 0 8px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  border-radius: 8px;
}

.tool-button :deep(.anticon) {
  font-size: 14px;
}

/* 带底色（首页首屏） */
.tool-button--filled {
  gap: 6px;
  padding: 0 12px;
  background: #f5f5f5;
  border: 1px solid #e5e7eb;
  transition:
    color 0.2s ease,
    background 0.2s ease,
    border-color 0.2s ease;
}

.tool-button--filled:not(:disabled):hover {
  color: rgba(0, 0, 0, 0.88) !important;
  background: #eee !important;
  border-color: #d4d4d8 !important;
}

/* 透明底色（对话页） */
.tool-button:not(.tool-button--filled):not(:disabled):hover {
  color: #1677ff !important;
  background: rgba(22, 119, 255, 0.08) !important;
}

.tool-button:disabled,
.tool-button.ant-btn-disabled {
  color: rgba(0, 0, 0, 0.25) !important;
  pointer-events: none;
}

.tool-button--filled:disabled,
.tool-button--filled.ant-btn-disabled {
  background: #fafafa !important;
  border-color: #f0f0f0 !important;
}
</style>
