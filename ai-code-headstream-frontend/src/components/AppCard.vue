<script setup lang="ts">
import { computed } from 'vue'
import { DeleteOutlined, EditOutlined, EllipsisOutlined } from '@ant-design/icons-vue'
import { formatDate, getAppCoverUrl, isGoodApp, isPinnedApp } from '@/utils/app.ts'

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
  /** 编辑应用 */
  (e: 'edit', app: API.AppVO): void
  /** 删除应用 */
  (e: 'delete', app: API.AppVO): void
}>()
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
      <a-avatar
        class="app-card__avatar"
        :size="40"
        :src="app.user?.userAvatar"
        :style="{ backgroundColor: '#40a9ff' }"
      >
        {{ app.user?.userName?.slice(0, 1) ?? 'U' }}
      </a-avatar>
      <div class="app-card__info">
        <div class="app-card__name">{{ app.appName }}</div>
        <div class="app-card__meta">
          <span class="app-card__author-name">{{ app.user?.userName ?? authorFallback }}</span>
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
                  <a-menu-item @click="emit('edit', app)">
                    <EditOutlined />
                    编辑
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
      <a-tag v-if="displayTag" :color="tagColor" class="app-card__tag">{{ displayTag }}</a-tag>
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
  display: flex;
  align-items: center;
  justify-content: center;
  height: 160px;
  overflow: hidden;
  background-size: cover;
  background-position: center;
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
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-card__body {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 14px 16px 8px;
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

.app-card__info {
  flex: 1;
  min-width: 0;
}

.app-card__name {
  overflow: hidden;
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-card__meta {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
  margin-top: 4px;
}

.app-card__author-name {
  overflow: hidden;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-card__time {
  flex-shrink: 0;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.app-card__tag {
  flex-shrink: 0;
  margin-right: 0;
}
</style>
