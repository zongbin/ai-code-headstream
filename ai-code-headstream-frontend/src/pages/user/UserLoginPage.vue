<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogin } from '@/api/userController.ts'
import { message } from 'ant-design-vue'

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
const handleSubmit = async (values: any) => {
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
  <div class="login-page">
    <div class="login-card">
      <div class="login-card__brand">
        <img class="login-card__logo" src="@/assets/logo.png" alt="logo" />
      </div>

      <h2 class="login-card__title">用户登录</h2>
      <p class="login-card__subtitle">欢迎回来，请登录你的账号</p>

      <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
        <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input
            v-model:value="formState.userAccount"
            size="large"
            placeholder="请输入账号"
            allow-clear
          >
            <template #prefix>
              <UserOutlined class="login-card__input-icon" />
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
              <LockOutlined class="login-card__input-icon" />
            </template>
          </a-input-password>
        </a-form-item>

        <div class="login-card__tips">
          没有账号？
          <RouterLink to="/user/register">去注册</RouterLink>
        </div>

        <a-form-item class="login-card__submit">
          <a-button type="primary" size="large" html-type="submit" :loading="submitting">
            登录
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<style scoped>
/* 页面背景：浅灰底 + 顶部柔光 */
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 160px);
  padding: 24px 16px 48px;
  background:
    radial-gradient(720px 320px at 50% -10%, rgba(0, 0, 0, 0.04) 0%, transparent 60%),
    linear-gradient(180deg, #f7f8fa 0%, #eceef1 100%);
}

/* 登录卡片 */
.login-card {
  width: 100%;
  max-width: 400px;
  padding: 36px 34px 30px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 18px;
  box-shadow:
    0 12px 32px rgba(15, 23, 42, 0.08),
    0 2px 6px rgba(15, 23, 42, 0.04);
}

.login-card__brand {
  display: flex;
  justify-content: center;
  margin-bottom: 18px;
}

.login-card__logo {
  width: 52px;
  height: 52px;
  object-fit: contain;
  filter: drop-shadow(0 4px 10px rgba(99, 102, 241, 0.25));
}

.login-card__title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  text-align: center;
}

.login-card__subtitle {
  margin: 6px 0 24px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  text-align: center;
}

.login-card__input-icon {
  color: rgba(0, 0, 0, 0.35);
}

.login-card__tips {
  margin-bottom: 18px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  text-align: right;
}

.login-card__tips a {
  color: #1677ff;
}

.login-card__tips a:hover {
  color: #4096ff;
}

/* 提交按钮独占一行 */
.login-card__submit :deep(.ant-btn) {
  width: 100%;
  height: 42px;
  font-size: 15px;
  font-weight: 500;
  background: linear-gradient(135deg, #22d3ee 0%, #6366f1 100%);
  border-color: transparent;
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.35);
}

.login-card__submit :deep(.ant-btn:hover) {
  background: linear-gradient(135deg, #38bdf8 0%, #818cf8 100%);
  border-color: transparent;
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.45);
}

@media (max-width: 480px) {
  .login-card {
    padding: 28px 22px 24px;
  }
}
</style>
