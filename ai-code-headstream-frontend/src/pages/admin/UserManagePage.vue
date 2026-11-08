<template>
  <div id="userManagePage">
    <!-- 搜索表单 -->
    <search-form @search="doSearch" @reset="doReset">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="输入用户名" />
      </a-form-item>
    </search-form>
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      row-key="id"
      size="middle"
      :scroll="{ x: 1280 }"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, text, record }">
        <template v-if="['userName', 'userProfile', 'userAvatar'].includes(column.dataIndex)">
          <div>
            <a-input
              v-if="editableData[record.id]"
              v-model:value="editableData[record.id]![column.dataIndex as keyof API.UserVO]"
              style="margin: -5px 0"
            />
            <template v-else-if="column.dataIndex === 'userAvatar'">
              <a-image :src="record.userAvatar" :width="40" :height="40" class="user-avatar" />
            </template>
            <span v-else class="ellipsis">{{ text }}</span>
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'userRole'">
          <a-select
            v-if="editableData[record.id]"
            v-model:value="editableData[record.id]!.userRole"
            style="width: 120px"
          >
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="user">普通用户</a-select-option>
          </a-select>
          <div v-else-if="record.userRole === 'admin'">
            <a-tag color="green">管理员</a-tag>
          </div>
          <div v-else>
            <a-tag color="blue">普通用户</a-tag>
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatDateTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <div class="row-operations">
            <template v-if="editableData[record.id]">
              <a-typography-link @click="save(record.id)">保存</a-typography-link>
              <a-popconfirm title="确定取消编辑？" @confirm="cancel(record.id)">
                <a>取消</a>
              </a-popconfirm>
            </template>
            <template v-else>
              <a-typography-link @click="edit(record.id)">编辑</a-typography-link>
              <a-button danger size="small" @click="doDelete(record.id)">删除</a-button>
            </template>
          </div>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { onMounted, reactive, toRaw } from 'vue'
import type { UnwrapRef } from 'vue'
import { deleteUser, listUserVoByPage, updateUser } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { formatDateTime } from '@/utils/app.ts'
import { usePagedList } from '@/composables/usePagedList.ts'
import SearchForm from '@/components/SearchForm.vue'

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 200,
    ellipsis: true,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
    width: 160,
    ellipsis: true,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    width: 140,
    ellipsis: true,
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    width: 72,
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
    ellipsis: true,
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
    width: 120,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 140,
    fixed: 'right' as const,
  },
]

// 行内编辑中的数据
const editableData: UnwrapRef<Record<string, API.UserVO>> = reactive({})

const {
  data,
  searchParams,
  pagination,
  fetchData,
  doSearch,
  doReset,
  doTableChange,
} = usePagedList<API.UserVO, API.UserQueryRequest>(
  listUserVoByPage,
  {
    userAccount: undefined,
    userName: undefined,
  },
  { errorTip: '获取用户列表失败' },
)

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

const edit = (id: number) => {
  const current = data.value.find((item) => item.id === id)
  if (!current) {
    return
  }
  editableData[id] = { ...toRaw(current) }
}

const save = async (id: number) => {
  const edited = editableData[id]
  if (!edited) {
    return
  }
  const res = await updateUser({
    id,
    userName: edited.userName,
    userAvatar: edited.userAvatar,
    userProfile: edited.userProfile,
    userRole: edited.userRole,
  })
  if (res.data.code === 0) {
    const current = data.value.find((item) => item.id === id)
    if (current) {
      Object.assign(current, edited)
    }
    delete editableData[id]
    message.success('保存成功')
  } else {
    message.error('保存失败，' + res.data.message)
  }
}

const cancel = (id: number) => {
  delete editableData[id]
}

// 删除数据
const doDelete = async (id: number) => {
  if (!id) {
    return
  }
  const res = await deleteUser({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}
</script>
<style scoped>
.user-avatar {
  display: block;
  object-fit: cover;
  border-radius: 50%;
}

.ellipsis {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-operations {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  align-items: center;
  white-space: nowrap;
}
</style>
