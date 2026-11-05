<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  getAppVoById,
  getAppVoByIdByAdmin,
  updateApp,
  updateAppByAdmin,
} from '@/api/appController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { formatDateTime } from '@/utils/app.ts'
import ACCESS_ENUM from '@/access/accessEnum.ts'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)

// 当前应用 ID
// 后端主键是雪花 ID，会以字符串返回（避免 Long 精度丢失），
// 所以这里全程按字符串传递，openapi 生成的类型定义仍是 number，调用时需要转换一下。
const appId = computed(() => String(route.params.id))

// 是否为管理员
const isAdmin = computed(() => loginUserStore.loginUser.userRole === ACCESS_ENUM.ADMIN)

// 应用详情
const appInfo = ref<API.AppVO>({})

// 表单数据
const form = reactive({
  appName: '',
  cover: '',
  priority: 0,
})

// 表单校验规则
const rules = {
  appName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
}

// 普通用户只能编辑自己的应用
const canEdit = computed(() => {
  if (isAdmin.value) {
    return true
  }
  return !!appInfo.value.id && appInfo.value.userId === loginUserStore.loginUser.id
})

/**
 * 加载应用详情
 */
const loadAppInfo = async () => {
  if (!appId.value) {
    return
  }
  loading.value = true
  try {
    const res = isAdmin.value
      ? await getAppVoByIdByAdmin({ id: appId.value as unknown as number })
      : await getAppVoById({ id: appId.value as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      form.appName = res.data.data.appName ?? ''
      form.cover = res.data.data.cover ?? ''
      form.priority = res.data.data.priority ?? 0
    } else {
      message.error('获取应用信息失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取应用信息失败', error)
    message.error('获取应用信息失败，请检查后端服务是否已启动')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAppInfo()
})

/**
 * 保存应用信息
 */
const doSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    // 校验不通过，交给表单展示错误
    return
  }
  if (!canEdit.value) {
    message.error('只能编辑自己的应用')
    return
  }
  saving.value = true
  try {
    const res = isAdmin.value
      ? await updateAppByAdmin({
          id: appId.value as unknown as number,
          appName: form.appName,
          cover: form.cover,
          priority: form.priority,
        })
      : await updateApp({
          id: appId.value as unknown as number,
          appName: form.appName,
        })
    if (res.data.code === 0) {
      message.success('保存成功')
      router.back()
    } else {
      message.error('保存失败，' + res.data.message)
    }
  } catch (error) {
    console.error('保存应用信息失败', error)
    message.error('保存失败，请检查后端服务是否已启动')
  } finally {
    saving.value = false
  }
}

/**
 * 取消
 */
const doCancel = () => {
  router.back()
}
</script>

<template>
  <div id="appEditPage">
    <a-card title="应用信息修改" :loading="loading" style="max-width: 720px; margin: 0 auto">
      <a-alert
        v-if="!loading && !canEdit"
        type="error"
        show-icon
        message="只能编辑自己的应用"
        style="margin-bottom: 16px"
      />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" @finish="doSubmit">
        <a-form-item label="应用 ID">
          <a-input :value="appInfo.id" disabled />
        </a-form-item>
        <a-form-item label="应用名称" name="appName">
          <a-input v-model:value="form.appName" placeholder="请输入应用名称" :maxlength="256" />
        </a-form-item>
        <a-form-item v-if="isAdmin" label="应用封面">
          <a-input v-model:value="form.cover" placeholder="请输入封面图片地址" :maxlength="512" />
          <a-image
            v-if="form.cover"
            :src="form.cover"
            :width="120"
            style="margin-top: 8px; border-radius: 6px"
          />
        </a-form-item>
        <a-form-item v-if="isAdmin" label="优先级">
          <a-input-number v-model:value="form.priority" :min="0" :max="99" style="width: 200px" />
          <div class="tip">优先级设置为 99 即为精选应用</div>
        </a-form-item>
        <a-form-item label="初始提示词">
          <a-textarea
            :value="appInfo.initPrompt"
            :rows="3"
            disabled
            class="readonly-field"
            placeholder="初始提示词不可修改"
          />
          <div class="tip">初始提示词不可修改</div>
        </a-form-item>
        <a-form-item label="创建时间">
          <span>{{ formatDateTime(appInfo.createTime) }}</span>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="saving" :disabled="!canEdit">
              保存
            </a-button>
            <a-button html-type="button" @click="doCancel">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
#appEditPage {
  padding: 8px 0;
}

.tip {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.readonly-field :deep(textarea) {
  color: rgba(0, 0, 0, 0.45);
  background: #f5f5f5;
  cursor: not-allowed;
}
</style>
