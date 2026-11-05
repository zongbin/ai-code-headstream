<script setup lang="ts">
/**
 * 表单底部操作按钮（保存 / 取消）
 */
withDefaults(
  defineProps<{
    /** 保存中 */
    saving?: boolean
    /** 禁用保存按钮 */
    disabled?: boolean
  }>(),
  {
    saving: false,
    disabled: false,
  },
)

const emit = defineEmits<{
  /** 取消 */
  (e: 'cancel'): void
}>()
</script>

<template>
  <a-form-item>
    <a-space>
      <a-button type="primary" html-type="submit" :loading="saving" :disabled="disabled">
        保存
      </a-button>
      <a-button html-type="button" @click="emit('cancel')">取消</a-button>
    </a-space>
  </a-form-item>
</template>
