<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const route = useRoute()
// 主页使用灰色背景
const isHome = computed(() => route.path === '/')
</script>

<template>
  <a-layout class="basic-layout" :class="{ 'basic-layout--home': isHome }">
    <!-- 顶部导航栏 -->
    <global-header />

    <!-- 中间内容区域：根据路由切换页面 -->
    <a-layout-content class="basic-layout__content">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </a-layout-content>

    <!-- 底部版权信息：始终固定在底部 -->
    <a-layout-footer class="basic-layout__footer">
      <global-footer />
    </a-layout-footer>
  </a-layout>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
}

/* 主页：灰色背景，导航栏保持组件自身的白色 */
.basic-layout--home {
  background:
    radial-gradient(900px 420px at 50% -8%, rgba(0, 0, 0, 0.04) 0%, transparent 62%),
    linear-gradient(180deg, #f7f8fa 0%, #e6e8eb 100%);
}

.basic-layout--home .basic-layout__content {
  background-color: transparent;
}

.basic-layout__content {
  flex: 1;
  padding: 16px 24px;
  background-color: #f5f5f5;
}

.basic-layout__footer {
  padding: 0;
  background-color: transparent;
}

/* 路由切换过渡动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 768px) {
  .basic-layout__content {
    padding: 12px;
  }
}
</style>
