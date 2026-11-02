<script setup lang="ts">
import { RouterView } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<template>
  <a-layout class="basic-layout">
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
