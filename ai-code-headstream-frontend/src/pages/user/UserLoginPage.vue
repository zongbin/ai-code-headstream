<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogin } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import AuthCard from '@/components/AuthCard.vue'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

// 提交中状态，防止重复点击
const submitting = ref(false)

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

/**
 * 获取登录后要跳回的路径
 * redirect 可能是相对路径（如 /app/edit/1），也可能是完整地址（如 http://xxx/app/edit/1）
 */
const getRedirectPath = (): string => {
  const redirect = route.query.redirect as string | undefined
  if (!redirect) {
    return '/'
  }
  if (/^https?:\/\//i.test(redirect)) {
    try {
      return new URL(redirect).pathname + new URL(redirect).search
    } catch {
      return '/'
    }
  }
  return redirect
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserLoginRequest) => {
  submitting.value = true
  try {
    const res = await userLogin(values)
    // 登录成功，把登录态保存到全局状态中
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      // 登录前被拦截的页面，通过 redirect 参数跳回
      router.push({
        path: getRedirectPath(),
        replace: true,
      })
    } else {
      message.error('登录失败，' + res.data.message)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <auth-card title="用户登录" subtitle="欢迎回来，请登录你的账号">
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input
          v-model:value="formState.userAccount"
          size="large"
          placeholder="请输入账号"
          allow-clear
        >
          <template #prefix>
            <UserOutlined class="auth-card__input-icon" />
          </template>
        </a-input>
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码不能小于 8 位' },
        ]"
      >
        <a-input-password
          v-model:value="formState.userPassword"
          size="large"
          placeholder="请输入密码"
        >
          <template #prefix>
            <LockOutlined class="auth-card__input-icon" />
          </template>
        </a-input-password>
      </a-form-item>

      <div class="auth-card__tips">
        没有账号？
        <RouterLink to="/user/register">去注册</RouterLink>
      </div>

      <a-form-item class="auth-card__submit">
        <a-button type="primary" size="large" html-type="submit" :loading="submitting">
          登录
        </a-button>
      </a-form-item>
    </a-form>
  </auth-card>
</template>
