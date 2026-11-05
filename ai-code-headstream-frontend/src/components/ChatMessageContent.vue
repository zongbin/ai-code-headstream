<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { renderMarkdown } from '@/utils/markdown.ts'

const props = defineProps<{
  content: string
  streaming?: boolean
}>()

/**
 * 渲染后的 Markdown HTML
 *
 * 内容来自 AI 输出，经过 markdown-it 解析 + DOMPurify 过滤后使用 v-html 渲染。
 * 流式输出时内容变化频繁，这里做了节流，避免每来一个字就重新解析、重新高亮。
 */
const html = ref('')
let renderTimer: number | null = null

/** 已被用户手动收起的代码块 */
const collapsedKeys = ref<string[]>([])

/**
 * 记录当前 DOM 中处于收起状态的代码块
 * 在 DOM 被替换之前调用（watch 回调默认在组件更新前触发）
 */
const captureCollapsed = () => {
  const root = rootRef.value
  if (!root) {
    return
  }
  const keys: string[] = []
  root.querySelectorAll<HTMLDetailsElement>('details.md-code').forEach((item, index) => {
    if (!item.open) {
      keys.push(item.dataset.key ?? String(index))
    }
  })
  collapsedKeys.value = keys
}

/**
 * 重新渲染后，恢复用户收起状态
 */
const restoreCollapsed = async () => {
  await nextTick()
  const root = rootRef.value
  if (!root) {
    return
  }
  root.querySelectorAll<HTMLDetailsElement>('details.md-code').forEach((item, index) => {
    const key = item.dataset.key ?? String(index)
    item.open = !collapsedKeys.value.includes(key)
  })
  // 流式输出时在最后一个文字后面放光标；光标落在代码块里时，把该代码块滚到底部
  if (props.streaming) {
    const voidTags = new Set(['BR', 'HR', 'IMG', 'INPUT'])
    let caretHost: HTMLElement = root
    while (caretHost.lastElementChild && !voidTags.has(caretHost.lastElementChild.tagName)) {
      caretHost = caretHost.lastElementChild as HTMLElement
    }
    const caret = document.createElement('span')
    caret.className = 'chat-caret'
    caret.setAttribute('aria-hidden', 'true')
    caretHost.appendChild(caret)
    const codeBody = caretHost.closest<HTMLElement>('.md-code__body')
    if (codeBody) {
      codeBody.scrollTop = codeBody.scrollHeight
    }
  }
}

watch(
  () => props.streaming,
  (value) => {
    if (!value) {
      rootRef.value?.querySelectorAll('.chat-caret').forEach((el) => el.remove())
    }
  },
)

const doRender = () => {
  renderTimer = null
  html.value = renderMarkdown(props.content)
  void restoreCollapsed()
}

watch(
  () => props.content,
  () => {
    captureCollapsed()
    if (renderTimer === null) {
      renderTimer = window.setTimeout(doRender, props.streaming ? 48 : 0)
    }
  },
)

/**
 * 复制按钮的事件委托（v-html 渲染的内容无法直接绑定事件）
 */
const rootRef = ref<HTMLElement | null>(null)

const onClick = async (event: MouseEvent) => {
  const target = event.target as HTMLElement | null
  const button = target?.closest('.md-code__copy') as HTMLElement | null
  if (!button) {
    return
  }
  // 阻止冒泡，避免触发展开/收起
  event.preventDefault()
  event.stopPropagation()
  const code = button.closest('.md-code')?.querySelector('code')?.textContent ?? ''
  try {
    await navigator.clipboard.writeText(code)
    const label = button.querySelector('.md-code__copy-label')
    if (label) {
      label.textContent = '已复制'
    }
    button.classList.add('is-copied')
    window.setTimeout(() => {
      if (label) {
        label.textContent = '复制'
      }
      button.classList.remove('is-copied')
    }, 1500)
  } catch (error) {
    console.error('复制失败', error)
    message.error('复制失败，请手动复制')
  }
}

onMounted(() => {
  rootRef.value?.addEventListener('click', onClick)
  doRender()
})

onBeforeUnmount(() => {
  rootRef.value?.removeEventListener('click', onClick)
  if (renderTimer !== null) {
    window.clearTimeout(renderTimer)
    renderTimer = null
  }
})
</script>

<template>
  <div ref="rootRef" class="chat-content" v-html="html"></div>
</template>

<style scoped>
.chat-content {
  word-break: break-word;
}

.chat-content :deep(.chat-caret) {
  display: inline-block;
  width: 8px;
  height: 1em;
  margin-left: 1px;
  vertical-align: text-bottom;
  background: #1f1f1f;
  animation: chat-caret-blink 0.9s steps(1) infinite;
}

@keyframes chat-caret-blink {
  50% {
    opacity: 0;
  }
}

.chat-content :deep(h1),
.chat-content :deep(h2),
.chat-content :deep(h3),
.chat-content :deep(h4),
.chat-content :deep(h5),
.chat-content :deep(h6) {
  margin: 12px 0 6px;
  font-weight: 600;
  line-height: 1.4;
}

.chat-content :deep(h1) {
  font-size: 20px;
}

.chat-content :deep(h2) {
  font-size: 18px;
}

.chat-content :deep(h3) {
  font-size: 16px;
}

.chat-content :deep(h4),
.chat-content :deep(h5),
.chat-content :deep(h6) {
  font-size: 15px;
}

.chat-content :deep(p) {
  margin: 6px 0;
  line-height: 1.7;
}

.chat-content :deep(ul),
.chat-content :deep(ol) {
  margin: 6px 0;
  padding-left: 22px;
  line-height: 1.7;
}

.chat-content :deep(li) {
  margin: 2px 0;
}

.chat-content :deep(blockquote) {
  margin: 8px 0;
  padding: 4px 12px;
  color: rgba(0, 0, 0, 0.55);
  background: rgba(0, 0, 0, 0.03);
  border-left: 3px solid rgba(0, 0, 0, 0.15);
}

.chat-content :deep(a) {
  color: #1677ff;
  text-decoration: none;
}

.chat-content :deep(a:hover) {
  text-decoration: underline;
}

.chat-content :deep(hr) {
  margin: 12px 0;
  border: none;
  border-top: 1px solid #f0f0f0;
}

.chat-content :deep(table) {
  width: 100%;
  margin: 8px 0;
  border-collapse: collapse;
  font-size: 13px;
}

.chat-content :deep(th),
.chat-content :deep(td) {
  padding: 6px 10px;
  border: 1px solid #f0f0f0;
  text-align: left;
}

.chat-content :deep(th) {
  background: #fafafa;
  font-weight: 600;
}

.chat-content :deep(code) {
  padding: 1px 5px;
  margin: 0 2px;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 4px;
}

/* 代码块容器 */
.chat-content :deep(.md-code) {
  margin: 10px 0;
  overflow: hidden;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.chat-content :deep(.md-code__header) {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
  list-style: none;
  background: #f5f5f5;
  border-bottom: 1px solid #f0f0f0;
}

.chat-content :deep(.md-code__header::-webkit-details-marker) {
  display: none;
}

.chat-content :deep(.md-code__arrow) {
  width: 0;
  height: 0;
  border-top: 4px solid transparent;
  border-bottom: 4px solid transparent;
  border-left: 6px solid rgba(0, 0, 0, 0.45);
  transition: transform 0.2s;
}

.chat-content :deep(.md-code[open] > .md-code__header .md-code__arrow) {
  transform: rotate(90deg);
}

.chat-content :deep(.md-code__name) {
  overflow: hidden;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.75);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-content :deep(.md-code__lang) {
  padding: 0 6px;
  font-size: 11px;
  line-height: 18px;
  color: #1677ff;
  text-transform: uppercase;
  background: rgba(22, 119, 255, 0.1);
  border-radius: 4px;
}

.chat-content :deep(.md-code__copy) {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  margin-left: auto;
  font-size: 12px;
  line-height: 1;
  color: rgba(0, 0, 0, 0.45);
  cursor: pointer;
  background: transparent;
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-radius: 4px;
  transition:
    color 0.2s,
    border-color 0.2s,
    background 0.2s;
}

.chat-content :deep(.md-code__copy-icon) {
  display: block;
  flex-shrink: 0;
}

.chat-content :deep(.md-code__copy:hover),
.chat-content :deep(.md-code__copy.is-copied) {
  color: #1677ff;
  background: rgba(22, 119, 255, 0.06);
  border-color: #1677ff;
}

.chat-content :deep(.md-code__copy:focus-visible) {
  outline: 2px solid #1677ff;
  outline-offset: 2px;
}

.chat-content :deep(.md-code__body) {
  max-height: 400px;
  padding: 12px;
  margin: 0;
  overflow: auto;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  background: #fff;
  white-space: pre;
}

/* 代码块内的行内代码不加背景色，避免与高亮样式冲突 */
.chat-content :deep(.md-code__body code) {
  padding: 0;
  margin: 0;
  font-size: inherit;
  background: transparent;
  border-radius: 0;
}
</style>
