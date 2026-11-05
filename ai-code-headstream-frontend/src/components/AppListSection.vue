<script setup lang="ts">
import AppCard from '@/components/AppCard.vue'
import type { AppLabelOption } from '@/utils/app.ts'

const props = withDefaults(
  defineProps<{
    /** 区块标题 */
    title: string
    /** 应用列表 */
    apps: API.AppVO[]
    /** 总条数 */
    total: number
    /** 每页条数 */
    pageSize: number
    /** 空列表提示 */
    emptyText: string
    /** 作者名兜底文案（如「我」「官方」） */
    authorFallback?: string
    /** 卡片右上角标签（如「精选」） */
    tag?: string
    /** 是否展示更多操作菜单（编辑、删除） */
    editable?: boolean
    /** 标签筛选项（为空数组时不展示标签筛选） */
    labelOptions?: AppLabelOption[]
  }>(),
  {
    authorFallback: '',
    tag: '',
    editable: false,
    labelOptions: () => [],
  },
)

/** 当前页码（双向绑定） */
const pageNum = defineModel<number>('pageNum', { required: true })
/** 当前选中的标签（双向绑定，空字符串表示全部） */
const label = defineModel<string>('label', { required: false, default: '' })

const emit = defineEmits<{
  /** 翻页 */
  (e: 'page-change', page: number): void
  /** 切换标签筛选 */
  (e: 'label-change', label: string): void
  /** 查看对话 */
  (e: 'view-chat', app: API.AppVO): void
  /** 查看部署后的作品 */
  (e: 'view-deploy', app: API.AppVO): void
  /** 编辑应用 */
  (e: 'edit', app: API.AppVO): void
  /** 删除应用 */
  (e: 'delete', app: API.AppVO): void
}>()

/**
 * 翻页：同步页码并通知父级重新请求
 */
const onPageChange = (page: number) => {
  pageNum.value = page
  emit('page-change', page)
}

/**
 * 切换标签：同步选中项并通知父级重新请求
 */
const onLabelChange = (value: string) => {
  label.value = value
  emit('label-change', value)
}
</script>

<template>
  <section class="app-section">
    <div class="app-section__header">
      <h2 class="app-section__title">{{ title }}</h2>
    </div>

    <!-- 标签筛选（案例广场按标签分类展示） -->
    <div v-if="props.labelOptions.length > 0" class="app-section__labels">
      <button
        v-for="option in props.labelOptions"
        :key="option.value || 'all'"
        type="button"
        class="app-section__label"
        :class="{ 'app-section__label--active': label === option.value }"
        @click="onLabelChange(option.value)"
      >
        {{ option.text }}
      </button>
    </div>

    <a-empty v-if="props.apps.length === 0" :description="emptyText" />
    <div v-else class="app-section__grid">
      <app-card
        v-for="app in props.apps"
        :key="app.id"
        :app="app"
        :author-fallback="authorFallback"
        :tag="tag"
        :editable="editable"
        @view-chat="(item) => emit('view-chat', item)"
        @view-deploy="(item) => emit('view-deploy', item)"
        @edit="(item) => emit('edit', item)"
        @delete="(item) => emit('delete', item)"
      />
    </div>

    <div v-if="props.total > props.pageSize" class="app-section__pagination">
      <a-pagination
        :current="pageNum"
        :page-size="props.pageSize"
        :total="props.total"
        show-less-items
        @change="onPageChange"
      />
    </div>
  </section>
</template>

<style scoped>
.app-section {
  position: relative;
  z-index: 1;
  max-width: 1600px;
  margin: 24px auto 0;
  padding: 24px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 18px;
}

.app-section__header {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.app-section__title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.app-section__labels {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.app-section__label {
  padding: 4px 14px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  cursor: pointer;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  transition:
    color 0.2s ease,
    background 0.2s ease,
    border-color 0.2s ease;
}

.app-section__label:hover {
  color: #1677ff;
  border-color: #91caff;
}

.app-section__label--active {
  color: #fff;
  background: #1677ff;
  border-color: #1677ff;
}

.app-section__label--active:hover {
  color: #fff;
  background: #4096ff;
  border-color: #4096ff;
}

/* 卡片一行显示 4 个，卡片宽度自适应容器 */
.app-section__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.app-section__pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 1280px) {
  .app-section__grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1024px) {
  .app-section__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .app-section {
    padding: 16px;
  }

  .app-section__grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
