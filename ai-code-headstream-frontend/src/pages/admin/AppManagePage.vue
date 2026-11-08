<template>
  <div id="appManagePage">
    <!-- 搜索表单 -->
    <search-form @search="doSearch" @reset="doReset">
      <a-form-item label="应用 ID">
        <a-input v-model:value="searchParams.id" placeholder="输入应用 ID" style="width: 120px" />
      </a-form-item>
      <a-form-item label="应用名称">
        <a-input
          v-model:value="searchParams.appName"
          placeholder="输入应用名称"
          style="width: 160px"
        />
      </a-form-item>
      <a-form-item label="创建者 ID">
        <a-input
          v-model:value="searchParams.userId"
          placeholder="输入用户 ID"
          style="width: 120px"
        />
      </a-form-item>
      <a-form-item label="生成类型">
        <a-select
          v-model:value="searchParams.codeGenType"
          placeholder="生成类型"
          style="width: 160px"
          allow-clear
        >
          <a-select-option
            v-for="item in CODE_GEN_TYPE_OPTIONS"
            :key="item.value"
            :value="item.value"
          >
            {{ item.text }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="部署标识">
        <a-input
          v-model:value="searchParams.deployKey"
          placeholder="部署标识"
          style="width: 140px"
        />
      </a-form-item>
      <a-form-item label="优先级">
        <a-input-number
          v-model:value="searchParams.priority"
          placeholder="优先级"
          style="width: 120px"
        />
      </a-form-item>
      <a-form-item label="可见状态">
        <a-select
          v-model:value="searchParams.visible"
          placeholder="可见状态"
          style="width: 120px"
          allow-clear
        >
          <a-select-option :value="APP_VISIBLE_PUBLIC">公开</a-select-option>
          <a-select-option :value="APP_VISIBLE_PRIVATE">私有</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="应用标签">
        <a-select
          v-model:value="searchParams.label"
          placeholder="应用标签"
          style="width: 140px"
          allow-clear
        >
          <a-select-option v-for="label in APP_LABELS" :key="label" :value="label">
            {{ label }}
          </a-select-option>
        </a-select>
      </a-form-item>
    </search-form>
    <a-divider />
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      row-key="id"
      :scroll="{ x: 1760 }"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'cover'">
          <a-image v-if="record.cover" :src="record.cover" :width="80" />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dataIndex === 'visible'">
          <a-tag :color="record.visible === APP_VISIBLE_PUBLIC ? 'green' : 'default'">
            {{ getAppVisibleText(record.visible) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'initPrompt'">
          <a-tooltip :title="record.initPrompt">
            <span class="ellipsis">{{ record.initPrompt }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'codeGenType'">
          <a-tag :color="getCodeGenTypeColor(record.codeGenType)">
            {{ getCodeGenTypeText(record.codeGenType) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'priority'">
          <a-tag v-if="isPinnedApp(record)" color="red">置顶</a-tag>
          <a-tag v-else-if="isGoodApp(record)" color="gold">精选</a-tag>
          <span v-else>{{ record.priority ?? 0 }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'label'">
          <a-tag v-if="record.label" color="blue">{{ record.label }}</a-tag>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dataIndex === 'user'">
          <span>{{ record.user?.userName ?? record.userId ?? '-' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatDateTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'deployedTime'">
          {{ record.deployedTime ? formatDateTime(record.deployedTime) : '-' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <div class="row-operations">
            <a-typography-link @click="doEdit(record.id)">编辑</a-typography-link>
            <a-typography-link @click="doToggleGood(record)">
              {{ isGoodApp(record) ? '取消精选' : '精选' }}
            </a-typography-link>
            <a-typography-link @click="doToggleTop(record)">
              {{ isPinnedApp(record) ? '取消置顶' : '置顶' }}
            </a-typography-link>
            <a-popconfirm
              title="确定删除该应用？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="doDelete(record.id)"
            >
              <a-button danger size="small" class="delete-button">删除</a-button>
            </a-popconfirm>
          </div>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  deleteAppByAdmin,
  listAppVoByPageByAdmin,
  updateAppByAdmin,
} from '@/api/appController.ts'
import {
  APP_LABELS,
  APP_VISIBLE_PRIVATE,
  APP_VISIBLE_PUBLIC,
  CODE_GEN_TYPE_OPTIONS,
  DEFAULT_APP_PRIORITY,
  formatDateTime,
  getAppVisibleText,
  getCodeGenTypeColor,
  getCodeGenTypeText,
  GOOD_APP_PRIORITY,
  isGoodApp,
  isPinnedApp,
  PINNED_APP_PRIORITY,
} from '@/utils/app.ts'
import { usePagedList } from '@/composables/usePagedList.ts'
import SearchForm from '@/components/SearchForm.vue'

const router = useRouter()

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 100,
  },
  {
    title: '应用名称',
    dataIndex: 'appName',
    width: 180,
  },
  {
    title: '可见状态',
    dataIndex: 'visible',
    width: 100,
  },
  {
    title: '封面',
    dataIndex: 'cover',
    width: 100,
  },
  {
    title: '初始提示词',
    dataIndex: 'initPrompt',
    width: 240,
  },
  {
    title: '生成类型',
    dataIndex: 'codeGenType',
    width: 160,
  },
  {
    title: '部署标识',
    dataIndex: 'deployKey',
    width: 120,
  },
  {
    title: '部署时间',
    dataIndex: 'deployedTime',
    width: 180,
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    width: 100,
  },
  {
    title: '标签',
    dataIndex: 'label',
    width: 100,
  },
  {
    title: '创建者',
    dataIndex: 'user',
    width: 140,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 220,
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
} = usePagedList<API.AppVO, API.AppQueryRequest>(
  listAppVoByPageByAdmin,
  {
    id: undefined,
    appName: undefined,
    userId: undefined,
    codeGenType: undefined,
    deployKey: undefined,
    priority: undefined,
    visible: undefined,
    label: undefined,
  },
  { errorTip: '获取应用列表失败' },
)

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

// 编辑应用
const doEdit = (id: number) => {
  router.push(`/app/edit/${id}`)
}

// 设为精选 / 取消精选（通过优先级切换）
const doToggleGood = async (record: API.AppVO) => {
  if (!record.id) {
    return
  }
  const good = isGoodApp(record)
  const res = await updateAppByAdmin({
    id: record.id,
    priority: good ? DEFAULT_APP_PRIORITY : GOOD_APP_PRIORITY,
  })
  if (res.data.code === 0) {
    message.success(good ? '已取消精选' : '已设为精选应用')
    fetchData()
  } else {
    message.error((good ? '取消精选失败，' : '设置精选失败，') + res.data.message)
  }
}

// 置顶 / 取消置顶（通过标记优先级实现：置顶为 999，取消置顶回到默认 0）
const doToggleTop = async (record: API.AppVO) => {
  if (!record.id) {
    return
  }
  const pinned = isPinnedApp(record)
  const res = await updateAppByAdmin({
    id: record.id,
    priority: pinned ? DEFAULT_APP_PRIORITY : PINNED_APP_PRIORITY,
  })
  if (res.data.code === 0) {
    message.success(pinned ? '已取消置顶' : '已置顶')
    fetchData()
  } else {
    message.error((pinned ? '取消置顶失败，' : '置顶失败，') + res.data.message)
  }
}

// 删除数据
const doDelete = async (id: number) => {
  if (!id) {
    return
  }
  const res = await deleteAppByAdmin({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败，' + res.data.message)
  }
}
</script>
<style scoped>
.row-operations a {
  margin-right: 8px;
}

.row-operations .delete-button {
  border-radius: 6px;
  padding: 0 12px;
  height: 26px;
}

.ellipsis {
  display: inline-block;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}
</style>
