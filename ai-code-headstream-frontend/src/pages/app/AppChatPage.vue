<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  BulbOutlined,
  CheckOutlined,
  CopyOutlined,
  EditOutlined,
  InfoCircleOutlined,
  PaperClipOutlined,
  RocketOutlined,
} from '@ant-design/icons-vue'
import { chatToGenCodeStream, createTypewriter } from '@/utils/appChat.ts'
import {
  deleteApp,
  deleteAppByAdmin,
  deployApp,
  getAppVoById,
  listMyAppVoByPage,
} from '@/api/appController.ts'
import { getAppPreviewUrl } from '@/utils/app.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import ACCESS_ENUM from '@/access/accessEnum.ts'
import ChatMessageContent from '@/components/ChatMessageContent.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import AppPreview from '@/components/AppPreview.vue'
import SendButton from '@/components/SendButton.vue'
import ToolButton from '@/components/ToolButton.vue'
import siteLogo from '@/assets/logo.png'

interface ChatMessage {
  id: number
  role: 'user' | 'ai'
  content: string
  streaming?: boolean
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 当前应用 ID
// 后端主键是雪花 ID，会以字符串返回（避免 Long 精度丢失），
// 所以这里全程按字符串传递，openapi 生成的类型定义仍是 number，调用时需要转换一下。
const appId = computed(() => String(route.params.id))

// 应用信息
const appInfo = ref<API.AppVO>({})
const loading = ref(false)

// 是否是自己的作品（只有作者本人才能继续对话）
const isOwner = computed(
  () => !!appInfo.value.id && appInfo.value.userId === loginUserStore.loginUser.id,
)

// 是否为管理员
const isAdmin = computed(() => loginUserStore.loginUser.userRole === ACCESS_ENUM.ADMIN)

// 是否可以管理（修改/删除）该应用：仅本人或管理员
const canManage = computed(() => isOwner.value || isAdmin.value)

// 应用详情悬浮窗
const detailVisible = ref(false)
const deleting = ref(false)

// 从应用卡片进入时带 view=1，只查看对话，不自动发送初始提示词
const isViewMode = computed(() => route.query.view === '1')

// 我的应用列表（用于顶部切换应用）
const myApps = ref<API.AppVO[]>([])

// 顶部下拉选项：确保当前应用始终存在
const appOptions = computed(() => {
  const options = [...myApps.value]
  const current = appInfo.value
  if (current.id && !options.some((item) => item.id === current.id)) {
    options.unshift(current)
  }
  return options
})

// 消息列表
const messages = ref<ChatMessage[]>([])
let messageId = 0

// 输入框内容
const inputText = ref('')

// 是否正在生成（含打字机还没打完的时间）
const streaming = ref(false)
let abortController: AbortController | null = null
let typewriter: ReturnType<typeof createTypewriter> | null = null
// 切换应用或离开页面时递增，丢弃已经过期的生成请求
let requestSeq = 0

// 预览区域
const previewReady = ref(false)
const previewUrl = ref('')
const messagesRef = ref<HTMLElement | null>(null)

// 部署结果
const deployUrl = ref('')
const deployVisible = ref(false)
const deploying = ref(false)

/**
 * 滚动到消息底部
 */
const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

/** 打字过程中合并滚动，避免每个字都排一次 nextTick */
let scrollScheduled = false
const scheduleScroll = () => {
  if (scrollScheduled) {
    return
  }
  scrollScheduled = true
  void nextTick(() => {
    scrollScheduled = false
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

/**
 * 中断进行中的生成，并让尚未结束的请求回调失效
 */
const cancelStreaming = () => {
  requestSeq++
  abortController?.abort()
  abortController = null
  typewriter?.stop()
  typewriter = null
}

/**
 * 获取当前应用详情
 */
const loadAppInfo = async () => {
  if (!appId.value) {
    return
  }
  loading.value = true
  try {
    const res = await getAppVoById({ id: appId.value as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
    } else {
      message.error('获取应用详情失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取应用详情失败', error)
    message.error('获取应用详情失败，请检查后端服务是否已启动')
  } finally {
    loading.value = false
  }
}

/**
 * 获取我的应用列表（顶部下拉切换）
 */
const loadMyApps = async () => {
  try {
    const res = await listMyAppVoByPage({
      pageNum: 1,
      pageSize: 20,
    })
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records ?? []
    }
  } catch (error) {
    console.error('获取应用列表失败', error)
  }
}

/**
 * 在新窗口打开预览页面
 */
const openPreview = () => {
  if (!previewUrl.value) {
    return
  }
  window.open(previewUrl.value, '_blank')
}


/**
 * 把异常转换成给用户看的错误信息
 *
 * appChat 里已经把后端的业务失败原因（如“无权限访问该应用”）转成了可读文案，
 * 直接展示即可；流进行中失败时后端会断开连接，浏览器抛出的信息没有业务含义，
 * 统一提示重试。
 */
const getErrorDetail = (error: unknown) => {
  const detail = (error as Error)?.message || ''
  if (!detail || /network error|failed to fetch|load failed|terminated/i.test(detail)) {
    return '连接中断，请稍后重试'
  }
  return detail
}

/**
 * 发送消息并接收 AI 流式回复
 * @param text 用户消息内容
 */
const sendMessage = async (text: string) => {
  const content = text.trim()
  if (!content || streaming.value || !isOwner.value) {
    return
  }
  const seq = ++requestSeq
  // 用户消息
  messages.value.push({
    id: ++messageId,
    role: 'user',
    content,
  })
  // AI 消息占位。必须通过 messages 里的响应式代理改内容，
  // 直接改 push 之前的普通对象不会触发视图更新，整段会等到结束才一次性出现。
  messages.value.push({
    id: ++messageId,
    role: 'ai',
    content: '',
    streaming: true,
  })
  const aiMessage = messages.value[messages.value.length - 1]
  inputText.value = ''
  streaming.value = true
  await scrollToBottom()
  if (seq !== requestSeq) {
    return
  }

  const printer = createTypewriter((typed) => {
    if (seq !== requestSeq || !aiMessage) {
      return
    }
    aiMessage.content = typed
    scheduleScroll()
  })
  typewriter = printer
  abortController = new AbortController()
  let succeeded = false
  try {
    await chatToGenCodeStream(
      {
        appId: appId.value,
        message: content,
      },
      (chunk) => printer.append(chunk),
      abortController.signal,
    )
    if (seq !== requestSeq) {
      return
    }
    await printer.finish()
    succeeded = true
  } catch (error) {
    if (seq !== requestSeq) {
      return
    }
    console.error('生成代码失败', error)
    if ((error as Error)?.name === 'AbortError') {
      printer.stop()
    } else {
      // 后端返回的具体原因（如无权限、应用不存在）直接展示给用户
      const detail = getErrorDetail(error)
      const tip = aiMessage?.content
        ? `\n\n> 生成失败：${detail}`
        : `> 生成失败：${detail}`
      printer.append(tip)
      await printer.finish()
      message.error(`生成代码失败，${detail}`)
    }
  } finally {
    // 请求已过期（切换应用等）时不做清理，状态已由对应流程重置
    if (seq === requestSeq) {
      if (aiMessage) {
        aiMessage.streaming = false
      }
      streaming.value = false
      abortController = null
      typewriter = null
      if (succeeded) {
        // 打字机打完后再展示网页，避免预览先于对话出现
        showPreview()
      }
      await scrollToBottom()
    }
  }
}

/**
 * 展示生成后的网页效果
 *
 * 后端在流式响应结束后才把代码写入磁盘，这里稍微延迟一下再加载预览，避免文件还没落盘。
 */
const showPreview = () => {
  const seq = requestSeq
  const url = getAppPreviewUrl(appInfo.value.codeGenType, appId.value)
  if (!url) {
    return
  }
  window.setTimeout(() => {
    if (seq !== requestSeq) {
      return
    }
    previewUrl.value = url
    previewReady.value = true
  }, 800)
}

/**
 * 发送按钮
 */
const doSend = async () => {
  await sendMessage(inputText.value)
}

/**
 * 部署应用
 */
const doDeploy = async () => {
  if (!appId.value) {
    return
  }
  deploying.value = true
  try {
    const res = await deployApp({ appId: appId.value as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployVisible.value = true
      message.success('部署成功')
    } else {
      message.error('部署失败，' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败', error)
    message.error('部署失败，请检查后端服务是否已启动')
  } finally {
    deploying.value = false
  }
}

/**
 * 跳转到应用编辑页
 */
const goEdit = () => {
  if (!appId.value) {
    return
  }
  router.push(`/app/edit/${appId.value}`)
}

/**
 * 删除当前应用（管理员走管理端接口，普通用户走本人接口）
 */
const doDelete = async () => {
  if (!appId.value) {
    return
  }
  deleting.value = true
  try {
    const res = isAdmin.value
      ? await deleteAppByAdmin({ id: appId.value as unknown as number })
      : await deleteApp({ id: appId.value as unknown as number })
    if (res.data.code === 0) {
      message.success('删除成功')
      detailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败，' + res.data.message)
    }
  } catch (error) {
    console.error('删除应用失败', error)
    message.error('删除失败，请检查后端服务是否已启动')
  } finally {
    deleting.value = false
  }
}

/**
 * 复制部署地址
 */
const copyDeployUrl = async () => {
  try {
    await navigator.clipboard.writeText(deployUrl.value)
    message.success('已复制部署地址')
  } catch (error) {
    console.error('复制失败', error)
    message.error('复制失败，请手动复制')
  }
}

/**
 * 切换应用
 */
const onAppChange = (value: number | string) => {
  router.push(`/app/chat/${value}`)
}

/**
 * 打开部署地址
 */
const openDeployUrl = () => {
  if (!deployUrl.value) {
    return
  }
  window.open(deployUrl.value, '_blank')
}

/**
 * 自动把初始提示词作为第一条消息发送给 AI。
 * 从应用卡片进入（带 view=1）时只查看对话，不自动发送。
 */
const autoSendInitPrompt = async () => {
  if (isViewMode.value) {
    return
  }
  const initPrompt = appInfo.value.initPrompt
  if (initPrompt) {
    await sendMessage(initPrompt)
  }
}

watch(appId, async () => {
  // 切换应用时重置状态
  cancelStreaming()
  messages.value = []
  inputText.value = ''
  streaming.value = false
  previewReady.value = false
  previewUrl.value = ''
  await loadAppInfo()
  // 自动把初始提示词作为第一条消息发送给 AI
  await autoSendInitPrompt()
})

onMounted(async () => {
  await loadAppInfo()
  await loadMyApps()
  await autoSendInitPrompt()
})

onBeforeUnmount(() => {
  cancelStreaming()
})
</script>

<template>
  <div class="chat-page">
    <!-- 顶部栏 -->
    <div class="chat-page__top">
      <div class="chat-page__top-left">
        <a-select
          class="chat-page__app-select"
          :value="appId"
          :loading="loading"
          @change="onAppChange"
        >
          <a-select-option v-for="item in appOptions" :key="item.id" :value="item.id">
            {{ item.appName }}
          </a-select-option>
        </a-select>
        <span class="chat-page__app-name">{{ appInfo.appName }}</span>
        <a-tag v-if="appInfo.deployedTime" color="green">已部署</a-tag>
      </div>
      <div class="chat-page__top-right">
        <a-button @click="detailVisible = true">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-button :loading="deploying" @click="doDeploy">
          <template #icon>
            <RocketOutlined />
          </template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 核心内容区域 -->
    <div class="chat-page__body">
      <!-- 左侧：对话区域 -->
      <div class="chat-page__chat">
        <div ref="messagesRef" class="chat-page__messages">
          <a-empty v-if="messages.length === 0" description="描述你的想法，开始生成网站" />
          <div
            v-for="item in messages"
            :key="item.id"
            class="chat-message"
            :class="item.role === 'user' ? 'chat-message--user' : 'chat-message--ai'"
          >
            <a-avatar
              v-if="item.role === 'ai'"
              class="chat-message__avatar"
              :size="32"
              shape="circle"
              :src="siteLogo"
            />
            <div class="chat-message__bubble">
              <p v-if="item.role === 'user'" class="chat-message__text">{{ item.content }}</p>
              <template v-else>
                <chat-message-content
                  v-if="item.content"
                  :content="item.content"
                  :streaming="item.streaming"
                />
                <span v-else class="chat-message__typing">
                  <a-spin size="small" />
                  <span>正在生成代码...</span>
                </span>
                <span v-if="item.streaming && !item.content" class="chat-message__cursor" />
              </template>
            </div>
          </div>
        </div>

        <!-- 用户消息输入框 -->
        <div class="chat-page__input">
          <a-tooltip :title="isOwner ? '' : '无法在别人的作品下对话哦~'">
            <div class="chat-page__textarea-wrap">
              <a-textarea
                v-model:value="inputText"
                :auto-size="{ minRows: 3, maxRows: 8 }"
                :disabled="streaming || !isOwner"
                :placeholder="
                  isOwner ? '描述越详细，页面越接近你的想法' : '无法在别人的作品下对话哦~'
                "
                @press-enter="doSend"
              />
            </div>
          </a-tooltip>
          <div class="chat-page__input-toolbar">
            <a-space :size="4">
              <tool-button :icon="PaperClipOutlined" label="上传" tooltip="上传附件功能暂未开放" />
              <tool-button :icon="EditOutlined" label="编辑" />
              <tool-button :icon="BulbOutlined" label="优化" tooltip="提示词优化功能暂未开放" />
            </a-space>
            <a-tooltip
              :title="
                !isOwner ? '无法在别人的作品下对话哦~' : inputText.trim() ? '发送' : '请先输入内容'
              "
            >
              <span class="chat-page__tip">
                <send-button
                  :loading="streaming"
                  :disabled="streaming || !inputText.trim() || !isOwner"
                  @click="doSend"
                />
              </span>
            </a-tooltip>
          </div>
        </div>
      </div>

      <!-- 右侧：网页展示区域 -->
      <app-preview :url="previewUrl" :ready="previewReady" @open="openPreview" />
    </div>

    <!-- 应用详情悬浮窗 -->
    <app-detail-modal
      v-model:open="detailVisible"
      :app="appInfo"
      :can-manage="canManage"
      :deleting="deleting"
      @edit="goEdit"
      @delete="doDelete"
    />

    <!-- 部署结果 -->
    <a-modal v-model:open="deployVisible" title="部署成功" :footer="null" :width="460" centered>
      <div class="deploy-result">
        <div class="deploy-result__icon">
          <CheckOutlined />
        </div>
        <div class="deploy-result__title">网站部署成功！</div>
        <div class="deploy-result__desc">你的网站已经成功部署，可以通过以下链接访问：</div>
        <a-input :value="deployUrl" readonly class="deploy-result__input">
          <template #suffix>
            <a-tooltip title="复制链接">
              <span class="deploy-result__copy" @click="copyDeployUrl">
                <CopyOutlined />
              </span>
            </a-tooltip>
          </template>
        </a-input>
        <div class="deploy-result__actions">
          <a-button type="primary" @click="openDeployUrl">访问网站</a-button>
          <a-button @click="deployVisible = false">关闭</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 140px);
  min-height: 520px;
  overflow: hidden;
  background: #fff;
  border-radius: 12px;
}

.chat-page__top {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.deploy-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.deploy-result__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: 16px;
  font-size: 28px;
  color: #52c41a;
  background: #f6ffed;
  border: 2px solid #52c41a;
  border-radius: 50%;
}

.deploy-result__title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.deploy-result__desc {
  margin: 8px 0 16px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.deploy-result__input {
  width: 100%;
}

.deploy-result__copy {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(0, 0, 0, 0.45);
  cursor: pointer;
}

.deploy-result__copy:hover {
  color: #1677ff;
}

.deploy-result__actions {
  display: flex;
  gap: 8px;
  margin-top: 20px;
}

.chat-page__top-left {
  display: flex;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.chat-page__top-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.chat-page__app-select {
  width: 180px;
}

.chat-page__app-name {
  overflow: hidden;
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-page__tip {
  display: inline-flex;
  line-height: 0;
}

.chat-page__body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.chat-page__chat {
  display: flex;
  flex-direction: column;
  width: 42%;
  min-width: 320px;
  border-right: 1px solid #f0f0f0;
}

.chat-page__messages {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.chat-message {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.chat-message--user {
  justify-content: flex-end;
}

.chat-message__avatar {
  flex-shrink: 0;
  overflow: hidden;
  background: #f0f0f0;
}

.chat-message__avatar :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.chat-message__bubble {
  max-width: 88%;
  padding: 10px 14px;
  background: #f7f8fa;
  border-radius: 10px;
}

.chat-message--user .chat-message__bubble {
  background: #e6f4ff;
}

.chat-message__text {
  margin: 0;
  line-height: 1.7;
  white-space: pre-wrap;
}

.chat-message__typing {
  display: flex;
  gap: 8px;
  align-items: center;
  color: rgba(0, 0, 0, 0.45);
}

.chat-message__cursor {
  display: inline-block;
  width: 8px;
  height: 1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: #1f1f1f;
  animation: chat-cursor-blink 0.9s steps(1) infinite;
}

@keyframes chat-cursor-blink {
  50% {
    opacity: 0;
  }
}

.chat-page__input {
  padding: 12px 16px 16px;
  border-top: 1px solid #f0f0f0;
}

.chat-page__textarea-wrap {
  display: block;
}

.chat-page__input :deep(textarea) {
  resize: none;
}

.chat-page__input-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

@media (max-width: 992px) {
  .chat-page {
    height: auto;
  }

  .chat-page__body {
    flex-direction: column;
  }

  .chat-page__chat {
    width: 100%;
    min-width: 0;
    border-right: none;
    border-bottom: 1px solid #f0f0f0;
  }

  .chat-page__messages {
    max-height: 420px;
  }
}
</style>
