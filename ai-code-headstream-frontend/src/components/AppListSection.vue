<script setup lang="ts">
import AppCard from '@/components/AppCard.vue'

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
  }>(),
  {
    authorFallback: '',
    tag: '',
    editable: false,
  },
)

/** 搜索关键字（双向绑定） */
const searchName = defineModel<string>('searchName', { required: true })
/** 当前页码（双向绑定） */
const pageNum = defineModel<number>('pageNum', { required: true })

const emit = defineEmits<{
  /** 触发搜索 */
  (e: 'search'): void
  /** 翻页 */
  (e: 'page-change', page: number): void
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
</script>

<template>
  <section class="app-section">
    <div class="app-section__header">
      <h2 class="app-section__title">{{ title }}</h2>
      <a-input-search
        v-model:value="searchName"
        class="app-section__search"
        placeholder="搜索应用名称"
        allow-clear
        @search="emit('search')"
      />
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
  max-width: 1240px;
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

.app-section__search {
  max-width: 260px;
}

.app-section__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.app-section__pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .app-section {
    padding: 16px;
  }
}
</style>
