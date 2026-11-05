<template>
  <div class="about">
    <a-card title="关于" style="max-width: 640px; margin: 0 auto">
      <p class="about__text">
        AI 应用生成平台：输入一句话描述你想要的网站或应用，AI 会自动生成前端代码，
        预览效果满意后可以直接部署上线。
      </p>
      <a-descriptions :column="1" bordered size="small" class="about__desc">
        <a-descriptions-item label="技术栈">Vue 3 + TypeScript + Vite + Ant Design Vue</a-descriptions-item>
        <a-descriptions-item label="后端">Spring Boot + MyBatis + MySQL</a-descriptions-item>
        <a-descriptions-item label="作者">leon</a-descriptions-item>
      </a-descriptions>
    </a-card>
  </div>
</template>

<style scoped>
#aboutPage,
.about {
  padding: 8px 0;
}
.about__text {
  margin-top: 0;
  line-height: 1.8;
  color: rgba(0, 0, 0, 0.65);
}

.about__desc {
  margin-top: 16px;
}
</style>
