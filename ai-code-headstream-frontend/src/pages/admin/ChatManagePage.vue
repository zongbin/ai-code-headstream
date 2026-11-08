<template>
  <div id="chatManagePage">
    <!-- 搜索表单 -->
    <search-form @search="doSearch" @reset="doReset">
      <a-form-item label="应用 ID">
        <a-input-number v-model:value="searchParams.appId" placeholder="输入应用 ID" :min="1" />
      </a-form-item>
      <a-form-item label="用户账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入用户账号" />
      </a-form-item>
      <a-form-item label="消息类型">
        <a-select v-model:value="searchParams.messageType" placeholder="消息类型" allow-clear>
          <a-select-option :value="MESSAGE_TYPE_USER">用户消息</a-select-option>
          <a-select-option :value="MESSAGE_TYPE_AI">AI 回复</a-select-option>
        </a-select>
      </a-form-item>
    </search-form>
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      row-key="id"
      size="middle"
      :scroll="{ x: 1460 }"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'messageType'">
          <a-tag :color="record.messageType === MESSAGE_TYPE_USER ? 'blue' : 'green'">
            {{ record.messageTypeText ?? getMessageTypeText(record.messageType) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'message'">
          <a-tooltip :title="record.message">
            <span class="ellipsis">{{ record.message }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'appName'">
          <a-typography-link class="ellipsis" @click="goAppChat(record)">
            {{ record.appName ?? record.appId ?? '-' }}
          </a-typography-link>
        </template>
        <template v-else-if="column.dataIndex === 'userAccount'">
          {{ record.userAccount ?? '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatDateTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <div class="row-operations">
            <a-typography-link @click="goAppChat(record)">查看对话</a-typography-link>
          </div>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listChatHistoryPageByAdmin } from '@/api/chatHistoryController.ts'
import { formatDateTime } from '@/utils/app.ts'
import { MESSAGE_TYPE_AI, MESSAGE_TYPE_USER } from '@/utils/chatHistory.ts'
import { usePagedList } from '@/composables/usePagedList.ts'
import SearchForm from '@/components/SearchForm.vue'

const router = useRouter()

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 200,
    ellipsis: true,
  },
  {
    title: '应用 ID',
    dataIndex: 'appId',
    width: 200,
    ellipsis: true,
  },
  {
    title: '应用名称',
    dataIndex: 'appName',
    width: 180,
    ellipsis: true,
  },
  {
    title: '消息类型',
    dataIndex: 'messageType',
    width: 110,
  },
  {
    title: '消息内容',
    dataIndex: 'message',
    width: 320,
    ellipsis: true,
  },
  {
    title: '用户账号',
    dataIndex: 'userAccount',
    width: 160,
    ellipsis: true,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 110,
    fixed: 'right' as const,
  },
]

const {
  data,
  searchParams,
  pagination,
  fetchData,
  doSearch,
  doReset,
  doTableChange,
} = usePagedList<API.ChatHistoryVO, API.ChatHistoryQueryRequest>(
  listChatHistoryPageByAdmin,
  {
    appId: undefined,
    userAccount: undefined,
    messageType: undefined,
  },
  { errorTip: '获取对话历史列表失败' },
)

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

/**
 * 消息类型文案（后端没返回描述时兜底）
 */
const getMessageTypeText = (messageType?: string) => {
  if (messageType === MESSAGE_TYPE_USER) {
    return '用户消息'
  }
  if (messageType === MESSAGE_TYPE_AI) {
    return 'AI 回复'
  }
  return messageType ?? '-'
}

/**
 * 查看某个应用的对话详情
 */
const goAppChat = (record: API.ChatHistoryVO) => {
  if (!record.appId) {
    return
  }
  router.push(`/app/chat/${record.appId}`)
}
</script>
<style scoped>
.ellipsis {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
