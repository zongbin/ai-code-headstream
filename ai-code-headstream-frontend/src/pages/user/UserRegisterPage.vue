<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

// 提交中状态，防止重复点击
const submitting = ref(false)

const router = useRouter()

/**
 * 验证确认密码
 * @param rule
 * @param value
 * @param callback
 */
const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  submitting.value = true
  try {
    const res = await userRegister(values)
    // 注册成功，跳转到登录页面
    if (res.data.code === 0) {
      message.success('注册成功')
      router.push({
        path: '/user/login',
        replace: true,
      })
    } else {
      message.error('注册失败，' + res.data.message)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-card__brand">
        <img class="register-card__logo" src="@/assets/logo.png" alt="logo" />
      </div>

      <h2 class="register-card__title">用户注册</h2>
      <p class="register-card__subtitle">创建一个账号，开启你的 AI 应用之旅</p>

      <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
        <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input
            v-model:value="formState.userAccount"
            size="large"
            placeholder="请输入账号"
            allow-clear
          >
            <template #prefix>
              <UserOutlined class="register-card__input-icon" />
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
              <LockOutlined class="register-card__input-icon" />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item
          name="checkPassword"
          :rules="[
            { required: true, message: '请确认密码' },
            { min: 8, message: '密码不能小于 8 位' },
            { validator: validateCheckPassword },
          ]"
        >
          <a-input-password
            v-model:value="formState.checkPassword"
            size="large"
            placeholder="请再次输入密码"
          >
            <template #prefix>
              <LockOutlined class="register-card__input-icon" />
            </template>
          </a-input-password>
        </a-form-item>

        <div class="register-card__tips">
          已有账号？
          <RouterLink to="/user/login">去登录</RouterLink>
        </div>

        <a-form-item class="register-card__submit">
          <a-button type="primary" size="large" html-type="submit" :loading="submitting">
            注册
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<style scoped>
/* 页面背景：浅灰底 + 顶部柔光 */
.register-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 160px);
  padding: 24px 16px 48px;
  background:
    radial-gradient(720px 320px at 50% -10%, rgba(0, 0, 0, 0.04) 0%, transparent 60%),
    linear-gradient(180deg, #f7f8fa 0%, #eceef1 100%);
}

/* 注册卡片 */
.register-card {
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

.register-card__brand {
  display: flex;
  justify-content: center;
  margin-bottom: 18px;
}

.register-card__logo {
  width: 52px;
  height: 52px;
  object-fit: contain;
  filter: drop-shadow(0 4px 10px rgba(99, 102, 241, 0.25));
}

.register-card__title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  text-align: center;
}

.register-card__subtitle {
  margin: 6px 0 24px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  text-align: center;
}

.register-card__input-icon {
  color: rgba(0, 0, 0, 0.35);
}

.register-card__tips {
  margin-bottom: 18px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  text-align: right;
}

.register-card__tips a {
  color: #1677ff;
}

.register-card__tips a:hover {
  color: #4096ff;
}

/* 提交按钮独占一行 */
.register-card__submit :deep(.ant-btn) {
  width: 100%;
  height: 42px;
  font-size: 15px;
  font-weight: 500;
  background: linear-gradient(135deg, #22d3ee 0%, #6366f1 100%);
  border-color: transparent;
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.35);
}

.register-card__submit :deep(.ant-btn:hover) {
  background: linear-gradient(135deg, #38bdf8 0%, #818cf8 100%);
  border-color: transparent;
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.45);
}

@media (max-width: 480px) {
  .register-card {
    padding: 28px 22px 24px;
  }
}
</style>
