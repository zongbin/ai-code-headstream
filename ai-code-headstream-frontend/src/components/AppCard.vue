<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, EllipsisOutlined } from '@ant-design/icons-vue'
import { formatDate, getAppCoverUrl, isGoodApp, isPinnedApp } from '@/utils/app.ts'
import { updateApp } from '@/api/appController.ts'

const props = withDefaults(
  defineProps<{
    /** 应用信息 */
    app: API.AppVO
    /** 作者名兜底文案（如「我」「官方」） */
    authorFallback?: string
    /** 卡片右上角标签（如「精选」） */
    tag?: string
    /** 是否展示更多操作菜单（编辑、删除） */
    editable?: boolean
  }>(),
  {
    authorFallback: '',
    tag: '',
    editable: false,
  },
)

/**
 * 卡片右上角标签
 *
 * 未指定固定标签时，置顶应用展示「置顶」，精选应用展示「精选」。
 */
const displayTag = computed(() => {
  if (props.tag) {
    return props.tag
  }
  if (isPinnedApp(props.app)) {
    return '置顶'
  }
  return isGoodApp(props.app) ? '精选' : ''
})

/** 标签颜色：精选用金色，置选用红色，其余用默认色 */
const tagColor = computed(() => {
  if (displayTag.value === '精选') {
    return 'gold'
  }
  if (displayTag.value === '置顶') {
    return 'red'
  }
  return 'blue'
})

const emit = defineEmits<{
  /** 查看对话 */
  (e: 'view-chat', app: API.AppVO): void
  /** 查看部署后的作品 */
  (e: 'view-deploy', app: API.AppVO): void
  /** 重命名成功 */
  (e: 'rename', app: API.AppVO, appName: string): void
  /** 删除应用 */
  (e: 'delete', app: API.AppVO): void
}>()

/** 是否处于重命名编辑状态 */
const renaming = ref(false)
/** 重命名输入框的值 */
const renameValue = ref('')
/** 重命名保存中 */
const saving = ref(false)
/** 重命名输入框组件（只需暴露 focus 方法） */
const renameInputRef = ref<{ focus?: () => void } | null>(null)
/** 卡片展示的应用名称（重命名成功后本地同步，避免整页刷新） */
const displayName = ref(props.app.appName ?? '')

watch(
  () => props.app.appName,
  (val) => {
    if (!renaming.value) {
      displayName.value = val ?? ''
    }
  },
)

/**
 * 开始重命名：把名称切换成输入框
 */
const startRename = async () => {
  renaming.value = true
  renameValue.value = props.app.appName ?? ''
  await nextTick()
  renameInputRef.value?.focus?.()
}

/**
 * 取消重命名
 */
const cancelRename = () => {
  renaming.value = false
  renameValue.value = ''
}

/**
 * 保存重命名：调用更新接口，成功后同步卡片名称
 */
const saveRename = async () => {
  const appName = renameValue.value.trim()
  if (!appName) {
    message.warning('应用名称不能为空')
    return
  }
  if (appName === props.app.appName) {
    cancelRename()
    return
  }
  if (!props.app.id) {
    message.error('重命名失败，应用不存在')
    return
  }
  saving.value = true
  try {
    const res = await updateApp({ id: props.app.id, appName })
    if (res.data.code === 0) {
      displayName.value = appName
      message.success('重命名成功')
      emit('rename', props.app, appName)
      cancelRename()
    } else {
      message.error('重命名失败，' + res.data.message)
    }
  } catch (error) {
    console.error('重命名失败', error)
    message.error('重命名失败，请检查后端服务是否已启动')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="app-card">
    <div class="app-card__cover">
      <img class="app-card__cover-img" :src="getAppCoverUrl(app)" :alt="app.appName" />
      <div class="app-card__cover-actions" @click.stop>
        <a-button size="small" class="app-card__cover-btn" @click="emit('view-chat', app)">
          查看对话
        </a-button>
        <a-button
          v-if="app.deployKey"
          size="small"
          class="app-card__cover-btn"
          @click="emit('view-deploy', app)"
        >
          查看作品
        </a-button>
      </div>
    </div>
    <div class="app-card__body">
      <div class="app-card__title-row">
        <!-- 重命名状态：名称就地替换为输入框 -->
        <template v-if="renaming">
          <a-input
            ref="renameInputRef"
            v-model:value="renameValue"
            class="app-card__rename-input"
            size="small"
            :maxlength="50"
            placeholder="请输入应用名称"
            @press-enter="saveRename"
            @keydown.esc="cancelRename"
          />
          <a-button
            type="primary"
            size="small"
            class="app-card__rename-btn"
            :loading="saving"
            @click="saveRename"
          >
            保存
          </a-button>
          <a-button size="small" class="app-card__rename-btn" :disabled="saving" @click="cancelRename">
            取消
          </a-button>
        </template>
        <template v-else>
          <div class="app-card__name" :title="displayName">{{ displayName }}</div>
          <a-tag v-if="app.label" color="blue" class="app-card__label">{{ app.label }}</a-tag>
          <a-tag v-if="displayTag" :color="tagColor" class="app-card__tag">{{ displayTag }}</a-tag>
        </template>
      </div>
      <div class="app-card__meta">
        <a-avatar
          class="app-card__avatar"
          :size="22"
          :src="app.user?.userAvatar"
          :style="{ backgroundColor: '#40a9ff' }"
        >
          {{ app.user?.userName?.slice(0, 1) ?? 'U' }}
        </a-avatar>
        <span class="app-card__author-name">{{ app.user?.userName ?? authorFallback }}</span>
        <span v-if="app.createTime" class="app-card__dot" aria-hidden="true">·</span>
        <span v-if="app.createTime" class="app-card__time">
          {{ formatDate(app.createTime) }}
        </span>
        <span v-if="editable" class="app-card__more" @click.stop>
          <a-dropdown :trigger="['click']">
            <a-button type="text" class="app-card__more-btn" aria-label="更多操作">
              <EllipsisOutlined />
            </a-button>
            <template #overlay>
              <a-menu class="app-card__more-menu">
                <a-menu-item @click="startRename">
                  <EditOutlined />
                  重命名
                </a-menu-item>
                <a-menu-item class="app-card__more-menu-item--danger">
                  <a-popconfirm
                    title="确定删除该应用？"
                    ok-text="确定"
                    cancel-text="取消"
                    @confirm="emit('delete', app)"
                  >
                    <span class="app-card__more-menu-trigger">
                      <DeleteOutlined />
                      删除
                    </span>
                  </a-popconfirm>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app-card {
  overflow: hidden;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  transition:
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.app-card:hover {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-4px);
}

.app-card__cover {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: #f5f5f5;
}

/* 悬停图片时显示遮罩与操作按钮 */
.app-card__cover::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.32);
  opacity: 0;
  transition: opacity 0.2s ease;
  pointer-events: none;
}

.app-card__cover:hover::after {
  opacity: 1;
}

.app-card__cover-actions {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 2;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  transform: translate(-50%, -50%);
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.2s ease;
}

.app-card__cover:hover .app-card__cover-actions,
.app-card__cover-actions:focus-within {
  opacity: 1;
  visibility: visible;
}

.app-card__cover-btn {
  color: rgba(0, 0, 0, 0.88);
  background: rgba(255, 255, 255, 0.95);
  border-color: transparent;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.app-card__cover-btn:hover,
.app-card__cover-btn:focus {
  color: #1677ff !important;
  background: #fff !important;
}

.app-card__cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-card__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px 12px;
}

.app-card__more {
  display: inline-flex;
  flex-shrink: 0;
  margin-left: auto;
}

.app-card__more-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  color: rgba(0, 0, 0, 0.45);
  border-radius: 4px;
}

.app-card__more-btn .anticon {
  font-size: 16px;
}

.app-card__more-btn:hover {
  color: #1677ff !important;
  background: rgba(22, 119, 255, 0.08) !important;
}

.app-card__more-btn:focus-visible {
  outline: 2px solid #1677ff;
  outline-offset: 2px;
}

.app-card__more-menu {
  min-width: 110px;
}

.app-card__more-menu-item--danger {
  color: #ff4d4f;
}

.app-card__more-menu-item--danger:hover {
  background: rgba(255, 77, 79, 0.08) !important;
}

.app-card__more-menu-trigger {
  display: flex;
  gap: 6px;
  align-items: center;
  width: 100%;
}

.app-card__avatar {
  flex-shrink: 0;
}

.app-card__title-row {
  display: flex;
  gap: 6px;
  align-items: center;
  min-width: 0;
  min-height: 24px;
}

.app-card__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
  color: rgba(0, 0, 0, 0.88);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-card__label,
.app-card__tag {
  flex-shrink: 0;
  margin-right: 0;
  margin-inline-end: 0;
  line-height: 20px;
}

.app-card__rename-input {
  flex: 1;
  min-width: 0;
}

.app-card__rename-btn {
  flex-shrink: 0;
}

.app-card__meta {
  display: flex;
  gap: 6px;
  align-items: center;
  min-width: 0;
  min-height: 24px;
}

.app-card__author-name {
  min-width: 0;
  overflow: hidden;
  font-size: 13px;
  line-height: 20px;
  color: rgba(0, 0, 0, 0.45);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-card__dot,
.app-card__time {
  flex-shrink: 0;
  font-size: 13px;
  line-height: 20px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
