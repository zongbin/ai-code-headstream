<template>
  <div id="appManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
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
          <a-select-option value="html">原生 HTML 模式</a-select-option>
          <a-select-option value="multi_file">原生多文件模式</a-select-option>
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
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
        <a-button style="margin-left: 8px" html-type="button" @click="doReset">重置</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      row-key="id"
      :scroll="{ x: 1680 }"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'cover'">
          <a-image v-if="record.cover" :src="record.cover" :width="80" />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dataIndex === 'initPrompt'">
          <a-tooltip :title="record.initPrompt">
            <span class="ellipsis">{{ record.initPrompt }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'codeGenType'">
          <a-tag :color="record.codeGenType === 'multi_file' ? 'blue' : 'green'">
            {{ record.codeGenType === 'multi_file' ? '多文件' : 'HTML' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'priority'">
          <a-tag v-if="isGoodApp(record)" color="gold">精选</a-tag>
          <span v-else>{{ record.priority ?? 0 }}</span>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { deleteAppByAdmin, listAppVoByPageByAdmin, updateAppByAdmin } from '@/api/appController.ts'
import { DEFAULT_APP_PRIORITY, formatDateTime, GOOD_APP_PRIORITY } from '@/utils/app.ts'

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
    width: 120,
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

// 数据
const data = ref<API.AppVO[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  try {
    const res = await listAppVoByPageByAdmin({
      ...searchParams,
    })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取应用列表失败', error)
    message.error('获取数据失败，请检查后端服务是否已启动')
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格变化处理
const doTableChange = (page: { current?: number; pageSize?: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索
const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

// 重置
const doReset = () => {
  searchParams.id = undefined
  searchParams.appName = undefined
  searchParams.userId = undefined
  searchParams.codeGenType = undefined
  searchParams.deployKey = undefined
  searchParams.priority = undefined
  searchParams.pageNum = 1
  fetchData()
}

// 编辑应用
const doEdit = (id: number) => {
  router.push(`/app/edit/${id}`)
}

const isGoodApp = (record: API.AppVO) => record.priority === GOOD_APP_PRIORITY

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
