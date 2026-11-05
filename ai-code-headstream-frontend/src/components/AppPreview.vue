<script setup lang="ts">
import { ref, watch } from 'vue'
import { ExportOutlined, ReloadOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  /** 预览页面地址 */
  url: string
  /** 是否已生成完成（可预览） */
  ready: boolean
}>()

const emit = defineEmits<{
  /** 在新窗口打开 */
  (e: 'open'): void
}>()

/** iframe 的 key，变化时强制重新加载 */
const version = ref(0)

watch(
  () => props.url,
  () => {
    version.value++
  },
)

/**
 * 刷新预览
 */
const refresh = () => {
  version.value++
}
</script>

<template>
  <div class="app-preview">
    <div class="app-preview__header">
      <span class="app-preview__title">网页效果</span>
      <a-space :size="4">
        <a-tooltip :title="ready ? '刷新预览' : '生成完成后可刷新'">
          <span class="app-preview__tip">
            <a-button
              type="text"
              class="app-preview__icon-btn"
              :disabled="!ready"
              aria-label="刷新预览"
              @click="refresh"
            >
              <ReloadOutlined />
            </a-button>
          </span>
        </a-tooltip>
        <a-tooltip :title="ready ? '在新窗口打开' : '生成完成后可打开'">
          <span class="app-preview__tip">
            <a-button
              type="text"
              class="app-preview__icon-btn"
              :disabled="!ready"
              aria-label="在新窗口打开"
              @click="emit('open')"
            >
              <ExportOutlined />
            </a-button>
          </span>
        </a-tooltip>
      </a-space>
    </div>
    <div class="app-preview__body">
      <div v-if="!ready" class="app-preview__empty">
        <a-empty description="生成完成后在此预览网页效果" />
      </div>
      <iframe v-else :key="version" class="app-preview__iframe" :src="url" title="网页预览"></iframe>
    </div>
  </div>
</template>

<style scoped>
.app-preview {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.app-preview__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.app-preview__title {
  font-size: 14px;
  font-weight: 600;
}

.app-preview__tip {
  display: inline-flex;
  line-height: 0;
}

.app-preview__icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.65);
  border-radius: 8px;
}

.app-preview__icon-btn:not(:disabled):hover {
  color: #1677ff !important;
  background: rgba(22, 119, 255, 0.08) !important;
}

.app-preview__icon-btn:disabled {
  pointer-events: none;
}

.app-preview__icon-btn:focus-visible {
  outline: 2px solid #1677ff;
  outline-offset: 2px;
}

.app-preview__body {
  flex: 1;
  min-height: 0;
  background: #fafafa;
}

.app-preview__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.app-preview__iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
}

@media (max-width: 992px) {
  .app-preview__body {
    height: 480px;
  }
}
</style>
