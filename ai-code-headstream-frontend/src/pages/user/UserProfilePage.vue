<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance, UploadProps } from 'ant-design-vue'
import { LoadingOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { getLoginUser, updateMyUser } from '@/api/userController.ts'
import { uploadAvatarFile } from '@/utils/avatarUpload.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import PageCard from '@/components/PageCard.vue'
import FormActions from '@/components/FormActions.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const formRef = ref<FormInstance>()

// 表单数据
const form = reactive({
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)

const AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']

const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
  if (!AVATAR_TYPES.includes(file.type)) {
    message.error('只能上传 JPG、PNG、GIF、WEBP 图片')
    return false
  }
  if (file.size / 1024 / 1024 >= 2) {
    message.error('头像不能超过 2MB')
    return false
  }
  return true
}

const handleAvatarUpload: UploadProps['customRequest'] = async (options) => {
  const { file, onSuccess, onError } = options
  uploading.value = true
  try {
    const res = await uploadAvatarFile(file as File)
    if (res.data.code === 0 && res.data.data) {
      form.userAvatar = res.data.data
      message.success('头像已上传，请点击保存')
      onSuccess?.(res.data)
    } else {
      message.error('头像上传失败，' + res.data.message)
      onError?.(new Error(res.data.message || '头像上传失败'))
    }
  } catch (error) {
    console.error('头像上传失败', error)
    const serverMessage = (error as any)?.response?.data?.message
    message.error(serverMessage ? '头像上传失败，' + serverMessage : '头像上传失败，请检查网络与后端服务')
    onError?.(error as Error)
  } finally {
    uploading.value = false
  }
}

// 表单校验规则
const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
}

// 加载当前登录用户信息
const loadUserInfo = async () => {
  loading.value = true
  try {
    const res = await getLoginUser()
    if (res.data.code === 0 && res.data.data) {
      form.userAccount = res.data.data.userAccount ?? ''
      form.userName = res.data.data.userName ?? ''
      form.userAvatar = res.data.data.userAvatar ?? ''
      form.userProfile = res.data.data.userProfile ?? ''
    } else {
      message.error('获取个人信息失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取个人信息失败', error)
    message.error('获取个人信息失败，请检查后端服务是否已启动')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})

// 保存个人信息
const doSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    // 校验不通过，交给表单展示错误
    return
  }
  saving.value = true
  try {
    const res = await updateMyUser({
      userName: form.userName,
      userAvatar: form.userAvatar,
      userProfile: form.userProfile,
    })
    if (res.data.code === 0) {
      message.success('保存成功')
      // 刷新登录用户信息，让顶栏的昵称与头像同步更新
      await loginUserStore.fetchLoginUser()
      await router.push('/')
    } else {
      message.error('保存失败，' + res.data.message)
    }
  } catch (error) {
    console.error('保存个人信息失败', error)
    message.error('保存失败，请检查后端服务是否已启动')
  } finally {
    saving.value = false
  }
}

// 取消
const doCancel = () => {
  router.back()
}
</script>

<template>
  <div id="userProfilePage">
    <page-card title="个人设置" :loading="loading">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" @finish="doSubmit">
        <a-form-item label="账号">
          <a-input v-model:value="form.userAccount" disabled placeholder="账号不可修改" />
        </a-form-item>
        <a-form-item label="用户名" name="userName">
          <a-input v-model:value="form.userName" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="头像">
          <a-upload
            name="file"
            class="avatar-uploader"
            list-type="picture-card"
            accept="image/jpeg,image/png,image/gif,image/webp"
            :show-upload-list="false"
            :before-upload="beforeAvatarUpload"
            :custom-request="handleAvatarUpload"
          >
            <img
              v-if="form.userAvatar && !uploading"
              :src="form.userAvatar"
              alt="头像"
              class="avatar-preview"
            />
            <div v-else>
              <LoadingOutlined v-if="uploading" />
              <PlusOutlined v-else />
              <div class="avatar-upload-text">{{ uploading ? '上传中' : '上传头像' }}</div>
            </div>
          </a-upload>
          <div class="avatar-upload-hint">支持 JPG、PNG、GIF、WEBP，不超过 2MB，上传后请点击保存</div>
        </a-form-item>
        <a-form-item label="简介">
          <a-textarea
            v-model:value="form.userProfile"
            :rows="4"
            placeholder="介绍一下自己吧"
            :maxlength="200"
            show-count
          />
        </a-form-item>
        <form-actions :saving="saving" @cancel="doCancel" />
      </a-form>
    </page-card>
  </div>
</template>

<style scoped>
#userProfilePage {
  padding: 8px 0;
}

.avatar-uploader :deep(.ant-upload) {
  width: 104px;
  height: 104px;
  overflow: hidden;
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload-text {
  margin-top: 8px;
}

.avatar-upload-hint {
  margin-top: 8px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
