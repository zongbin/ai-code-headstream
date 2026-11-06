/**
 * 对话历史相关的常量与类型
 */

/** 消息类型：用户消息（与后端 MessageTypeEnum 保持一致） */
export const MESSAGE_TYPE_USER = 'user'

/** 消息类型：AI 回复（与后端 MessageTypeEnum 保持一致） */
export const MESSAGE_TYPE_AI = 'ai'

/** 对话历史每页条数（一页 10 条） */
export const CHAT_HISTORY_PAGE_SIZE = 10

/**
 * 对话历史游标分页结果
 *
 * 后端在分页结果上额外返回了游标信息（hasMore、nextCursor），
 * openapi 生成的类型里没有体现这两个字段，这里补充上。
 */
export interface ChatHistoryCursorPage {
  /** 当前页的消息列表（按消息创建时间升序） */
  records?: API.ChatHistoryVO[]
  /** 当前页的条数 */
  totalRow?: number
  /** 是否还有更早的历史消息 */
  hasMore?: boolean
  /** 下一次向前加载的游标（本次结果中最早的创建时间），没有更多时为空 */
  nextCursor?: string
}
