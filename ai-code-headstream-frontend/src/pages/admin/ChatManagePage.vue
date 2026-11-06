<template>
  <div id="chatManagePage">
    <!-- 搜索表单 -->
    <search-form @search="doSearch" @reset="doReset">
      <a-form-item label="应用 ID">
        <a-input-number
          v-model:value="searchParams.appId"
          placeholder="输入应用 ID"
          style="width: 140px"
          :min="1"
        />
      </a-form-item>
      <a-form-item label="用户账号">
        <a-input
          v-model:value="searchParams.userAccount"
          placeholder="输入用户账号"
          style="width: 160px"
        />
      </a-form-item>
      <a-form-item label="消息类型">
        <a-select
          v-model:value="searchParams.messageType"
          placeholder="消息类型"
          style="width: 140px"
          allow-clear
        >
          <a-select-option :value="MESSAGE_TYPE_USER">用户消息</a-select-option>
          <a-select-option :value="MESSAGE_TYPE_AI">AI 回复</a-select-option>
        </a-select>
      </a-form-item>
    </search-form>
    <a-divider />
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      row-key="id"
      :scroll="{ x: 1540 }"
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
          <a-typography-link @click="goAppChat(record)">
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
    width: 180,
  },
  {
    title: '应用 ID',
    dataIndex: 'appId',
    width: 180,
  },
  {
    title: '应用名称',
    dataIndex: 'appName',
    width: 180,
  },
  {
    title: '消息类型',
    dataIndex: 'messageType',
    width: 100,
  },
  {
    title: '消息内容',
    dataIndex: 'message',
    width: 420,
  },
  {
    title: '用户账号',
    dataIndex: 'userAccount',
    width: 180,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
    fixed: 'right',
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
.row-operations a {
  margin-right: 8px;
}

.ellipsis {
  display: inline-block;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}
</style>
