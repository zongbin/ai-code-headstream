<script setup lang="ts">
import { DeleteOutlined, EditOutlined } from '@ant-design/icons-vue'
import { formatDateTime } from '@/utils/app.ts'

defineProps<{
  /** 应用详情 */
  app: API.AppVO
  /** 是否可以管理（修改/删除）该应用 */
  canManage?: boolean
  /** 删除中 */
  deleting?: boolean
}>()

/** 弹窗显示状态（双向绑定） */
const open = defineModel<boolean>('open', { required: true })

const emit = defineEmits<{
  /** 修改应用 */
  (e: 'edit'): void
  /** 删除应用 */
  (e: 'delete'): void
}>()
</script>

<template>
  <a-modal v-model:open="open" title="应用详情" :footer="null" :width="420" centered>
    <div class="app-detail">
      <div class="app-detail__section">
        <div class="app-detail__label">创建者</div>
        <div class="app-detail__creator">
          <a-avatar :size="36" :src="app.user?.userAvatar">
            {{ (app.user?.userName ?? '?').charAt(0) }}
          </a-avatar>
          <span class="app-detail__creator-name">{{ app.user?.userName ?? '-' }}</span>
        </div>
      </div>
      <div class="app-detail__section">
        <div class="app-detail__label">创建时间</div>
        <div class="app-detail__value">{{ formatDateTime(app.createTime) }}</div>
      </div>
      <div v-if="canManage" class="app-detail__section">
        <div class="app-detail__label">操作</div>
        <a-space :size="8">
          <a-button size="small" @click="emit('edit')">
            <template #icon>
              <EditOutlined />
            </template>
            修改
          </a-button>
          <a-popconfirm
            title="确定删除该应用？"
            ok-text="确定"
            cancel-text="取消"
            @confirm="emit('delete')"
          >
            <a-button danger size="small" :loading="deleting">
              <template #icon>
                <DeleteOutlined />
              </template>
              删除
            </a-button>
          </a-popconfirm>
        </a-space>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
.app-detail__section {
  margin-bottom: 16px;
}

.app-detail__section:last-child {
  margin-bottom: 0;
}

.app-detail__label {
  margin-bottom: 8px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.app-detail__creator {
  display: flex;
  gap: 10px;
  align-items: center;
}

.app-detail__creator-name {
  font-size: 15px;
  font-weight: 500;
}

.app-detail__value {
  font-size: 14px;
}
</style>
