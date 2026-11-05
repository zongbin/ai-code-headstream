<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { getLoginUser, updateMyUser } from '@/api/userController.ts'
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
          <a-input v-model:value="form.userAvatar" placeholder="请输入头像地址" />
          <a-image
            v-if="form.userAvatar"
            :src="form.userAvatar"
            :width="80"
            style="margin-top: 8px; border-radius: 6px"
          />
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
</style>
