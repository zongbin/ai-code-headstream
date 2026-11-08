import { API_BASE_URL } from '@/config'

/**
 * 后端是否会发送结束事件（done）。
 *
 * 新版后端在代码生成完毕后，会在流末尾追加一个 done 事件；旧版后端没有。
 * 这里记录下来，用来判断"流没收到 done 就断开了"到底是正常结束还是中途被掐断：
 * 只要这个后端发过 done，之后再遇到缺少 done 的流，就说明生成确实没完成。
 */
let backendSendsDone = false

/**
 * 构造流式接口的失败原因
 *
 * 正常情况下后端返回的是 SSE；流开始前就失败时，响应体可能是 JSON 错误信息，
 * 也可能是空的（接口声明了 produces = text/event-stream，错误响应写不出 JSON）。
 * 能拿到具体原因就用具体原因，拿不到就给通用提示。
 */
const buildErrorDetail = async (response: Response) => {
  try {
    const text = await response.text()
    if (text) {
      const json = JSON.parse(text) as { code?: number; message?: string }
      if (json?.message) {
        return json.message
      }
    }
  } catch {
    // 响应体为空或不是 JSON
  }
  if (response.status === 401) {
    return '登录状态已失效，请重新登录'
  }
  if (response.status === 403) {
    return '无权限访问该应用'
  }
  return '生成失败，请稍后重试'
}

/**
 * 应用聊天生成代码（流式 SSE）
 *
 * 后端以 text/event-stream 返回，有两种事件：
 * - 内容事件（未指定事件名，按 SSE 规范默认为 message）：data 形如 `{"d": "增量文本"}`，
 *   d 就是本次推送出来的代码片段。
 * - 结束事件（事件名 done）：data 为空，标识后端已经正常生成完毕。
 *   后端是在流结束的 doOnComplete 里把代码写入磁盘的，done 事件在它之后才发出，
 *   所以收到 done 就说明代码已经落盘，可以直接加载预览。
 *
 * 异常分两种情况：
 * - 流开始前就失败（如无权限、应用不存在）：接口声明了 produces = text/event-stream，
 *   全局异常处理返回的 JSON 写不出来，浏览器最终收到的是一个没有响应体的错误响应。
 *   这里按 Content-Type 判断不是 SSE，并尽量给出可读的失败原因。
 * - 流进行中失败（AI 调用异常等）：后端直接断开连接，客户端只能感知为流提前结束。
 *   此时若一段内容都没收到，或者这个后端发过 done 却没等到，就按失败处理。
 *
 * 该接口需要边读边处理，axios 无法流式读取，因此使用 fetch + ReadableStream 实现。
 *
 * 注意：src/api 目录下的文件由 openapi2ts 自动生成，请勿在该目录下新增手写文件。
 *
 * @param params 应用 ID（按字符串传递，避免 Long 精度丢失）与用户消息
 * @param onMessage 每收到一段增量文本时的回调
 * @param signal 中断信号
 * @returns 是否收到了后端的结束事件（done）。收到说明后端已正常生成完毕并保存了代码。
 */
export async function chatToGenCodeStream(
  params: { appId: string | number; message: string },
  onMessage: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<boolean> {
  const url = `${API_BASE_URL}/app/chat/gen/code?appId=${encodeURIComponent(
    String(params.appId),
  )}&message=${encodeURIComponent(params.message)}`
  const response = await fetch(url, {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'text/event-stream',
    },
    signal,
  })
  // 流开始前就失败时，返回的不是 SSE，这里取出失败原因
  const contentType = response.headers.get('content-type') || ''
  if (!contentType.includes('text/event-stream')) {
    throw new Error(await buildErrorDetail(response))
  }
  if (!response.body) {
    throw new Error('生成失败，请稍后重试')
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  // 是否已经收到过内容，用来判断流是否被提前掐断
  let received = false
  // 是否已经收到后端的结束事件（done）
  let finished = false

  /**
   * 解析一个完整的 SSE 事件
   */
  const readEvent = (raw: string) => {
    const lines = raw.split('\n')
    // 事件名：没有 event 字段时按 SSE 规范默认为 message
    let eventName = 'message'
    const dataLines: string[] = []
    for (const line of lines) {
      // 忽略注释行和空行
      if (!line || line.startsWith(':')) {
        continue
      }
      const colon = line.indexOf(':')
      const field = colon === -1 ? line : line.slice(0, colon)
      const value = colon === -1 ? '' : line.slice(colon + 1).replace(/^ /, '')
      if (field === 'event') {
        eventName = value
      } else if (field === 'data') {
        dataLines.push(value)
      }
    }
    // 结束事件：后端正常生成完毕
    if (eventName === 'done') {
      finished = true
      return
    }
    if (dataLines.length === 0) {
      return
    }
    const payload = dataLines.join('\n')
    // 结束事件的 data 为空，这里直接跳过，避免解析空 JSON 报错
    if (!payload.trim()) {
      return
    }
    try {
      // 后端的内容事件只有内容字段 d
      const parsed = JSON.parse(payload) as { d?: string }
      const data = parsed?.d
      if (typeof data !== 'string' || data.length === 0) {
        return
      }
      received = true
      onMessage(data)
    } catch (error) {
      console.error('解析 SSE 数据失败', payload, error)
    }
  }

  // 一直读到服务端关闭连接为止。
  // 收到 done 事件说明后端已经生成完毕，但代码是在流结束前的 doOnComplete 里落盘的，
  // 所以这里不主动 cancel 连接，让服务端正常收尾，避免被当成"流被取消"而误判未保存。
  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    // Spring SSE 使用 \n 分隔，兼容 \r\n
    buffer = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
    let index = buffer.indexOf('\n\n')
    while (index !== -1) {
      readEvent(buffer.slice(0, index))
      buffer = buffer.slice(index + 2)
      index = buffer.indexOf('\n\n')
    }
  }
  // 处理最后一段没有以空行结尾的数据
  if (buffer.trim().length > 0) {
    readEvent(buffer)
  }
  if (finished) {
    // 这个后端发过 done，说明后面缺少 done 就确实没生成完
    backendSendsDone = true
    // 收到了结束事件却没有任何内容，按生成失败处理
    if (!received) {
      throw new Error('本次未生成任何内容，请重试')
    }
    return true
  }
  // 没等到结束事件：一段内容都没收到，或者这个后端本来就会发 done 却没等到
  if (!received || backendSendsDone) {
    throw new Error('生成中断，请稍后重试')
  }
  // 旧版后端（不发 done）：连接关闭即视为生成结束
  return false
}

/**
 * 打字机：把已经到达的文本按节奏写到界面上。
 *
 * 流式接口一次可能推来一整段，直接拼接会一下子铺满。这里先攒在缓冲区，
 * 再按积压量决定每拍输出几个字：刚开始一字一字打，积压变多就加快，避免长代码打不完。
 */
export function createTypewriter(onText: (text: string) => void) {
  let source = ''
  let shown = 0
  let timer: number | null = null
  let stopped = false
  let closed = false
  let pendingDone: (() => void) | null = null

  const clearTimer = () => {
    if (timer !== null) {
      window.clearTimeout(timer)
      timer = null
    }
  }

  const settle = () => {
    const done = pendingDone
    pendingDone = null
    done?.()
  }

  const schedule = (delay: number) => {
    if (timer !== null || stopped) {
      return
    }
    timer = window.setTimeout(tick, delay)
  }

  const tick = () => {
    timer = null
    if (stopped) {
      return
    }
    const pending = source.length - shown
    if (pending > 0) {
      const step =
        pending > 800 ? 36 : pending > 300 ? 14 : pending > 80 ? 5 : pending > 20 ? 2 : 1
      shown = Math.min(source.length, shown + step)
      onText(source.slice(0, shown))
      schedule(pending > 300 ? 16 : 32)
      return
    }
    if (closed) {
      settle()
      return
    }
    schedule(40)
  }

  return {
    append(chunk: string) {
      if (stopped || !chunk) {
        return
      }
      source += chunk
      if (timer === null) {
        tick()
      }
    },
    /** 上游流已结束，打完缓冲区后 resolve */
    finish() {
      closed = true
      if (stopped) {
        return Promise.resolve()
      }
      if (shown >= source.length) {
        clearTimer()
        return Promise.resolve()
      }
      return new Promise<void>((resolve) => {
        pendingDone = resolve
        if (timer === null) {
          tick()
        }
      })
    },
    stop() {
      stopped = true
      closed = true
      clearTimer()
      settle()
    },
  }
}
