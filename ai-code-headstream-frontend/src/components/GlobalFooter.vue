<script setup lang="ts">
/**
 * 全局底部版权信息
 * 通过 position: sticky 始终固定在页面底部（内容不足一屏时也不会上浮）
 */
</script>

<template>
  <div class="global-footer">
    <span class="global-footer__text">created by leon. email:chen_6089@163.com</span>
  </div>
</template>

<style scoped>
.global-footer {
  width: 100%;
  padding: 16px 24px;
  text-align: center;
  background-color: #fff;
  border-top: 1px solid #f0f0f0;
}

.global-footer__text {
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}

@media (max-width: 768px) {
  .global-footer {
    padding: 12px 12px;
  }

  .global-footer__text {
    font-size: 12px;
  }
}
</style>
