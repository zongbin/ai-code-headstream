import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { listAppChatHistory } from '@/api/chatHistoryController.ts'
import { CHAT_HISTORY_PAGE_SIZE, type ChatHistoryCursorPage } from '@/utils/chatHistory.ts'

/**
 * 应用对话历史（游标分页）
 *
 * 后端按游标返回消息：首次查询不带游标，返回最新的一批消息（按创建时间升序）；
 * 需要更多历史记录时带上上一次返回的 nextCursor，即可向前加载更早的消息。
 *
 * 这里维护已加载的消息列表：加载更多时把更早的消息插到列表前面，
 * 保证列表始终按消息创建时间升序。
 *
 * @param getAppId 获取当前应用 id 的函数（应用切换时返回新的 id）
 * @param options 提示文案配置
 */
export function useChatHistory(getAppId: () => string, options: { errorTip?: string } = {}) {
  const errorTip = options.errorTip ?? '获取对话历史失败'
  // 已加载的消息（按消息创建时间升序）
  const records = ref<API.ChatHistoryVO[]>([])
  // 是否还有更早的历史消息
  const hasMore = ref(false)
  // 下一次向前加载的游标
  const nextCursor = ref('')
  // 首次加载中
  const loading = ref(false)
  // 加载更多中
  const loadingMore = ref(false)

  /**
   * 请求一页对话历史
   *
   * @param cursor 游标，为空表示从最新开始
   */
  const fetchPage = async (cursor?: string) => {
    const res = await listAppChatHistory({
      chatHistoryAppQueryRequest: {
        // 后端主键是雪花 ID，前端全程按字符串传递，调用时转换一下类型
        appId: getAppId() as unknown as number,
        pageSize: CHAT_HISTORY_PAGE_SIZE,
        cursor,
      },
    })
    if (res.data.code !== 0) {
      throw new Error(res.data.message || errorTip)
    }
    const data = (res.data.data ?? {}) as unknown as ChatHistoryCursorPage
    return {
      records: data.records ?? [],
      hasMore: !!data.hasMore,
      nextCursor: data.nextCursor ?? '',
    }
  }

  /**
   * 加载最新一页对话历史（覆盖已加载的消息）
   */
  const loadHistory = async () => {
    if (!getAppId()) {
      return
    }
    loading.value = true
    try {
      const page = await fetchPage()
      records.value = page.records
      hasMore.value = page.hasMore
      nextCursor.value = page.nextCursor
    } catch (error) {
      console.error(errorTip, error)
      message.error(`${errorTip}，请稍后重试`)
    } finally {
      loading.value = false
    }
  }

  /**
   * 加载更早的对话历史（追加到已加载消息的前面）
   */
  const loadMoreHistory = async () => {
    if (loading.value || loadingMore.value || !hasMore.value) {
      return
    }
    loadingMore.value = true
    try {
      const page = await fetchPage(nextCursor.value)
      records.value = [...page.records, ...records.value]
      hasMore.value = page.hasMore
      nextCursor.value = page.nextCursor
    } catch (error) {
      console.error(errorTip, error)
      message.error(`${errorTip}，请稍后重试`)
    } finally {
      loadingMore.value = false
    }
  }

  /**
   * 清空已加载的消息（切换应用时调用）
   */
  const resetHistory = () => {
    records.value = []
    hasMore.value = false
    nextCursor.value = ''
  }

  return {
    records,
    hasMore,
    loading,
    loadingMore,
    loadHistory,
    loadMoreHistory,
    resetHistory,
  }
}
