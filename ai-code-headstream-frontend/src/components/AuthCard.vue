<script setup lang="ts">
/**
 * 登录 / 注册页共用的卡片布局
 * 品牌区、标题、副标题与外层样式统一，内容通过默认插槽传入。
 */
defineProps<{
  /** 卡片标题 */
  title: string
  /** 卡片副标题 */
  subtitle: string
}>()
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-card__brand">
        <img class="auth-card__logo" src="@/assets/logo.png" alt="logo" />
      </div>

      <h2 class="auth-card__title">{{ title }}</h2>
      <p class="auth-card__subtitle">{{ subtitle }}</p>

      <slot />
    </div>
  </div>
</template>

<style scoped>
/* 页面背景：浅灰底 + 顶部柔光 */
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 160px);
  padding: 24px 16px 48px;
  background:
    radial-gradient(720px 320px at 50% -10%, rgba(0, 0, 0, 0.04) 0%, transparent 60%),
    linear-gradient(180deg, #f7f8fa 0%, #eceef1 100%);
}

/* 卡片 */
.auth-card {
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

.auth-card__brand {
  display: flex;
  justify-content: center;
  margin-bottom: 18px;
}

.auth-card__logo {
  width: 52px;
  height: 52px;
  object-fit: contain;
  filter: drop-shadow(0 4px 10px rgba(99, 102, 241, 0.25));
}

.auth-card__title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  text-align: center;
}

.auth-card__subtitle {
  margin: 6px 0 24px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  text-align: center;
}

/* 输入框前置图标 */
.auth-card :deep(.auth-card__input-icon) {
  color: rgba(0, 0, 0, 0.35);
}

/* 底部提示（如「没有账号？去注册」） */
.auth-card :deep(.auth-card__tips) {
  margin-bottom: 18px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  text-align: right;
}

.auth-card :deep(.auth-card__tips a) {
  color: #1677ff;
}

.auth-card :deep(.auth-card__tips a:hover) {
  color: #4096ff;
}

/* 提交按钮独占一行 */
.auth-card :deep(.auth-card__submit .ant-btn) {
  width: 100%;
  height: 42px;
  font-size: 15px;
  font-weight: 500;
  background: linear-gradient(135deg, #22d3ee 0%, #6366f1 100%);
  border-color: transparent;
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.35);
}

.auth-card :deep(.auth-card__submit .ant-btn:hover) {
  background: linear-gradient(135deg, #38bdf8 0%, #818cf8 100%);
  border-color: transparent;
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.45);
}

@media (max-width: 480px) {
  .auth-card {
    padding: 28px 22px 24px;
  }
}
</style>
