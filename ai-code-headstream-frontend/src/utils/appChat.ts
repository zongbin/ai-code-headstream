import { API_BASE_URL } from '@/request'

/**
 * 应用聊天生成代码（流式 SSE）
 *
 * 后端以 text/event-stream 返回，每个事件的 data 为 `{"t": "content"|"error", "d": "..."}`：
 * - t 为 content 时，d 是增量文本
 * - t 为 error 时，d 是错误信息，需要展示给用户
 *
 * 该接口需要边读边处理，axios 无法流式读取，因此使用 fetch + ReadableStream 实现。
 *
 * 注意：src/api 目录下的文件由 openapi2ts 自动生成，请勿在该目录下新增手写文件。
 *
 * @param params 应用 ID（按字符串传递，避免 Long 精度丢失）与用户消息
 * @param onMessage 每收到一段增量文本时的回调
 * @param signal 中断信号
 */
export async function chatToGenCodeStream(
  params: { appId: string | number; message: string },
  onMessage: (chunk: string) => void,
  signal?: AbortSignal,
) {
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
  if (!response.ok || !response.body) {
    let detail = `状态码：${response.status}`
    try {
      const text = await response.text()
      const json = JSON.parse(text) as { message?: string }
      if (json?.message) {
        detail = json.message
      }
    } catch {
      // 响应体不是 JSON 时保留状态码
    }
    throw new Error(`生成代码失败，${detail}`)
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  // 后端推送的错误信息，流结束后统一抛出
  let serverError = ''

  /**
   * 解析一个完整的 SSE 事件
   */
  const readEvent = (raw: string) => {
    const dataLines = raw
      .split('\n')
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trim())
    if (dataLines.length === 0) {
      return
    }
    const payload = dataLines.join('\n')
    try {
      const parsed = JSON.parse(payload) as { t?: string; d?: string }
      const data = parsed?.d
      if (typeof data !== 'string' || data.length === 0) {
        return
      }
      if (parsed?.t === 'error') {
        // 后端错误：记录下来，等流结束后抛出，避免和已收到的内容混在一起
        serverError = data
        return
      }
      onMessage(data)
    } catch (error) {
      console.error('解析 SSE 数据失败', payload, error)
    }
  }

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
  // 后端在流中推送了错误，抛出给调用方展示
  if (serverError) {
    throw new Error(serverError)
  }
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
