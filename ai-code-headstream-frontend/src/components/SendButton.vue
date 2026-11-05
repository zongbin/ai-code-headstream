<script setup lang="ts">
import { ArrowUpOutlined } from '@ant-design/icons-vue'

withDefaults(
  defineProps<{
    /** 加载中 */
    loading?: boolean
    /** 禁用 */
    disabled?: boolean
    /** 是否使用渐变样式（首页首屏） */
    gradient?: boolean
    /** 按钮直径（px） */
    size?: number
  }>(),
  {
    loading: false,
    disabled: false,
    gradient: false,
    size: 36,
  },
)

const emit = defineEmits<{
  (e: 'click'): void
}>()
</script>

<template>
  <a-button
    type="primary"
    shape="circle"
    class="send-button"
    :class="{ 'send-button--gradient': gradient }"
    :style="{ width: `${size}px`, height: `${size}px` }"
    :loading="loading"
    :disabled="disabled"
    aria-label="发送"
    @click="emit('click')"
  >
    <template #icon>
      <ArrowUpOutlined />
    </template>
  </a-button>
</template>

<style scoped>
.send-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.send-button :deep(.anticon) {
  font-size: 18px;
}

.send-button:focus-visible {
  outline: 2px solid #1677ff;
  outline-offset: 2px;
}

/* 渐变样式 */
.send-button--gradient {
  color: #fff;
  background: linear-gradient(135deg, #22d3ee 0%, #6366f1 100%);
  border-color: transparent;
  box-shadow:
    0 6px 20px rgba(99, 102, 241, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
  transition:
    background 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.send-button--gradient:hover {
  background: linear-gradient(135deg, #38bdf8 0%, #818cf8 100%) !important;
  box-shadow:
    0 8px 26px rgba(99, 102, 241, 0.7),
    inset 0 1px 0 rgba(255, 255, 255, 0.3) !important;
  transform: translateY(-1px);
}

.send-button--gradient:active {
  transform: translateY(0);
}

.send-button:disabled,
.send-button.ant-btn-disabled {
  color: rgba(0, 0, 0, 0.25) !important;
  pointer-events: none;
  background: #f0f0f0 !important;
  border-color: transparent !important;
  box-shadow: none !important;
  transform: none;
}
</style>
