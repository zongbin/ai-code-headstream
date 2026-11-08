import { computed, onBeforeUnmount, onMounted, ref, watch, type Ref } from 'vue'
import { message } from 'ant-design-vue'

/**
 * 可视化编辑：在预览 iframe 里选中元素，再把元素信息交给对话页。
 *
 * 预览网站和主网站同域，因此可以在预览文档里注入一段编辑脚本。
 * 脚本只负责悬浮边框、选中边框，以及通过 postMessage 把元素信息发给父页面。
 * 父页面发给预览页的开关指令也走 postMessage，对话页不必关心 iframe 内部实现。
 */

/** 父页面发出的消息来源 */
const PARENT_SOURCE = 'ai-code-parent'

/** 预览页面发出的消息来源 */
const PREVIEW_SOURCE = 'ai-code-preview'

/** 消息类型，避免和页面自己的 postMessage 混淆 */
const MESSAGE_TYPE = 'visual-edit'

/** 悬浮时的边框类名 */
const HOVER_CLASS = 'ai-code-visual-hover'

/** 选中后的边框类名 */
const SELECTED_CLASS = 'ai-code-visual-selected'

/** 编辑模式标记，加在预览页的 html 上 */
const EDITING_CLASS = 'ai-code-visual-editing'

/** 注入到预览页的样式 id */
const STYLE_ID = 'ai-code-visual-edit-style'

/**
 * 用户在预览页选中的元素
 */
export interface VisualElement {
  /** 标签名，小写 */
  tagName: string
  /** id，没有则为空字符串 */
  id: string
  /** class，没有则为空字符串 */
  className: string
  /** 可见文本，过长会截断 */
  text: string
  /** 能定位到该元素的 CSS 选择器 */
  selector: string
  /** 元素所在页面路径 */
  pagePath: string
}

/** 父页面 -> 预览页面 */
interface ParentEditMessage {
  source: typeof PARENT_SOURCE
  type: typeof MESSAGE_TYPE
  action: 'enable' | 'disable' | 'clear'
}

/** 预览页面 -> 父页面 */
interface PreviewEditMessage {
  source: typeof PREVIEW_SOURCE
  type: typeof MESSAGE_TYPE
  action: 'select'
  element?: Partial<VisualElement>
}

type PreviewWindow = Window & {
  __aiCodeVisualEditInstalled?: boolean
}

/**
 * 预览页里的编辑脚本。
 *
 * 悬浮时画浅色虚线边框，点击后改成更深的实线边框，并把元素信息 postMessage 给父页面。
 * 常量从父页面注入，避免和上面的协议字符串写两份后对不上。
 */
const PREVIEW_EDITOR_SCRIPT = `
(function () {
  if (window.__aiCodeVisualEditInstalled) {
    return
  }
  window.__aiCodeVisualEditInstalled = true

  var PARENT_SOURCE = ${JSON.stringify(PARENT_SOURCE)}
  var PREVIEW_SOURCE = ${JSON.stringify(PREVIEW_SOURCE)}
  var MESSAGE_TYPE = ${JSON.stringify(MESSAGE_TYPE)}
  var HOVER_CLASS = ${JSON.stringify(HOVER_CLASS)}
  var SELECTED_CLASS = ${JSON.stringify(SELECTED_CLASS)}
  var EDITING_CLASS = ${JSON.stringify(EDITING_CLASS)}
  var IGNORED = { HTML: 1, HEAD: 1, SCRIPT: 1, STYLE: 1, LINK: 1, META: 1, NOSCRIPT: 1 }

  var editing = false
  var hovered = null
  var selected = null

  function isElement(node) {
    return !!node && node.nodeType === 1
  }

  function isIgnored(el) {
    return !!IGNORED[el.tagName]
  }

  function eventElement(target) {
    var node = target
    if (node && node.nodeType === 3) {
      node = node.parentElement
    }
    if (!isElement(node) || isIgnored(node)) {
      return null
    }
    return node
  }

  function escapeCss(value) {
    if (window.CSS && typeof window.CSS.escape === 'function') {
      return window.CSS.escape(value)
    }
    return String(value).replace(/[^a-zA-Z0-9_-]/g, function (char) {
      return '\\\\' + char
    })
  }

  function readClassName(el) {
    var names = (el.getAttribute('class') || '').replace(/\\s+/g, ' ').trim().split(' ')
    var kept = []
    for (var i = 0; i < names.length; i++) {
      var name = names[i]
      if (!name || name === HOVER_CLASS || name === SELECTED_CLASS || name === EDITING_CLASS) {
        continue
      }
      kept.push(name)
    }
    return kept.join(' ')
  }

  function readText(el) {
    var text = ''
    if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
      text = el.value || el.getAttribute('placeholder') || ''
    } else if (el.tagName === 'IMG') {
      text = el.getAttribute('alt') || ''
    } else {
      text = el.innerText || el.textContent || ''
    }
    text = String(text).replace(/\\s+/g, ' ').trim()
    if (text.length > 80) {
      text = text.slice(0, 80) + '…'
    }
    return text
  }

  function buildSelector(el) {
    if (el.id) {
      var idSelector = '#' + escapeCss(el.id)
      try {
        if (document.querySelectorAll(idSelector).length === 1) {
          return idSelector
        }
      } catch (error) {
        // id 不能直接当选择器时，继续拼路径
      }
    }
    var parts = []
    var node = el
    while (node && node.nodeType === 1 && node.tagName !== 'HTML') {
      var part = node.tagName.toLowerCase()
      if (node.id) {
        part += '#' + escapeCss(node.id)
        parts.unshift(part)
        break
      }
      var parent = node.parentElement
      if (parent) {
        var sameTag = []
        var children = parent.children
        for (var i = 0; i < children.length; i++) {
          if (children[i].tagName === node.tagName) {
            sameTag.push(children[i])
          }
        }
        if (sameTag.length > 1) {
          part += ':nth-of-type(' + (sameTag.indexOf(node) + 1) + ')'
        }
      }
      parts.unshift(part)
      node = parent
    }
    var selector = parts.join(' > ')
    return selector.length > 500 ? selector.slice(0, 500) : selector
  }

  function describe(el) {
    return {
      tagName: el.tagName.toLowerCase(),
      id: el.id || '',
      className: readClassName(el),
      text: readText(el),
      selector: buildSelector(el),
      pagePath: window.location.pathname + window.location.hash,
    }
  }

  function clearHover() {
    if (hovered) {
      hovered.classList.remove(HOVER_CLASS)
      hovered = null
    }
  }

  function clearSelected() {
    if (selected) {
      selected.classList.remove(SELECTED_CLASS)
      selected = null
    }
  }

  function setEditing(next) {
    editing = !!next
    document.documentElement.classList.toggle(EDITING_CLASS, editing)
    if (!editing) {
      clearHover()
      clearSelected()
    }
  }

  document.addEventListener('mouseover', function (event) {
    if (!editing) {
      return
    }
    var target = eventElement(event.target)
    if (!target) {
      return
    }
    if (hovered === target) {
      return
    }
    clearHover()
    hovered = target
    if (target !== selected) {
      target.classList.add(HOVER_CLASS)
    }
  }, true)

  document.addEventListener('mouseout', function (event) {
    if (!editing || !hovered) {
      return
    }
    if (!event.relatedTarget) {
      clearHover()
    }
  }, true)

  function selectTarget(target) {
    if (!target) {
      return
    }
    clearHover()
    if (selected && selected !== target) {
      selected.classList.remove(SELECTED_CLASS)
    }
    selected = target
    selected.classList.add(SELECTED_CLASS)
    window.parent.postMessage({
      source: PREVIEW_SOURCE,
      type: MESSAGE_TYPE,
      action: 'select',
      element: describe(target),
    }, window.location.origin)
  }

  function blockPageAction(event) {
    if (!editing) {
      return
    }
    event.preventDefault()
    event.stopPropagation()
  }

  document.addEventListener('click', function (event) {
    if (!editing) {
      return
    }
    blockPageAction(event)
    selectTarget(eventElement(event.target))
  }, true)

  document.addEventListener('auxclick', blockPageAction, true)
  document.addEventListener('submit', blockPageAction, true)

  window.addEventListener('message', function (event) {
    if (event.source !== window.parent) {
      return
    }
    var data = event.data
    if (!data || data.source !== PARENT_SOURCE || data.type !== MESSAGE_TYPE) {
      return
    }
    if (data.action === 'enable') {
      setEditing(true)
      return
    }
    if (data.action === 'disable') {
      setEditing(false)
      return
    }
    if (data.action === 'clear') {
      clearSelected()
      clearHover()
    }
  })
})()
`

/** 注入到预览页的边框样式。悬浮是浅色虚线，选中后换成更深的实线。 */
const EDITOR_STYLE = `
html.${EDITING_CLASS},
html.${EDITING_CLASS} * {
  cursor: pointer !important;
}
.${HOVER_CLASS} {
  outline: 2px dashed #1677ff !important;
  outline-offset: 1px !important;
}
.${SELECTED_CLASS} {
  outline: 3px solid #0958d9 !important;
  outline-offset: 1px !important;
}
`

/**
 * 读取预览文档。跨域时浏览器会拒绝访问，这里转成明确的失败原因。
 */
const readPreviewDocument = (iframe: HTMLIFrameElement) => {
  try {
    const doc = iframe.contentDocument
    if (!doc) {
      throw new Error('无法编辑该预览页面，请确认预览网站与当前网站同域')
    }
    return doc
  } catch {
    throw new Error('无法编辑该预览页面，请确认预览网站与当前网站同域')
  }
}

/**
 * 向预览页注入编辑脚本。已经注入过则直接复用。
 */
const injectPreviewEditor = (iframe: HTMLIFrameElement) => {
  const doc = readPreviewDocument(iframe)
  if (!doc.getElementById(STYLE_ID)) {
    const style = doc.createElement('style')
    style.id = STYLE_ID
    style.textContent = EDITOR_STYLE
    ;(doc.head ?? doc.documentElement).appendChild(style)
  }
  const win = iframe.contentWindow as PreviewWindow | null
  if (!win) {
    throw new Error('请等待网页预览加载完成')
  }
  if (win.__aiCodeVisualEditInstalled) {
    return
  }
  const script = doc.createElement('script')
  script.textContent = PREVIEW_EDITOR_SCRIPT
  doc.documentElement.appendChild(script)
  if (!win.__aiCodeVisualEditInstalled) {
    throw new Error('编辑脚本注入失败')
  }
}

/**
 * 通知预览页进入、退出编辑，或清掉选中边框
 */
const postToPreview = (iframe: HTMLIFrameElement | null, action: ParentEditMessage['action']) => {
  const win = iframe?.contentWindow
  if (!win) {
    return
  }
  const payload: ParentEditMessage = {
    source: PARENT_SOURCE,
    type: MESSAGE_TYPE,
    action,
  }
  try {
    win.postMessage(payload, window.location.origin)
  } catch (error) {
    console.error('向预览页面发送编辑指令失败', error)
  }
}

/**
 * 把 postMessage 里的元素整理成可展示、可放进提示词的结构
 */
const normalizeElement = (value: unknown): VisualElement | null => {
  if (!value || typeof value !== 'object') {
    return null
  }
  const raw = value as Partial<VisualElement>
  const clip = (text: unknown, max: number) => {
    if (typeof text !== 'string') {
      return ''
    }
    const normalized = text.replace(/\s+/g, ' ').trim()
    return normalized.length > max ? normalized.slice(0, max) : normalized
  }
  const tagName = clip(raw.tagName, 40).toLowerCase()
  if (!tagName) {
    return null
  }
  return {
    tagName,
    id: clip(raw.id, 120),
    className: clip(raw.className, 200),
    text: clip(raw.text, 80),
    selector: clip(raw.selector, 500),
    pagePath: clip(raw.pagePath, 300),
  }
}

/**
 * 输入框上方的选中提示标题
 */
export const formatElementTitle = (element: VisualElement) => {
  let name = element.tagName || '元素'
  if (element.id) {
    name += `#${element.id}`
  } else if (element.className) {
    const first = element.className.split(/\s+/).find(Boolean)
    if (first) {
      name += `.${first}`
    }
  }
  return `已选中 ${name}`
}

/**
 * 输入框上方的选中提示详情
 */
export const formatElementDescription = (element: VisualElement) => {
  const lines: string[] = []
  if (element.selector) {
    lines.push(`选择器：${element.selector}`)
  }
  if (element.text) {
    lines.push(`文本：${element.text}`)
  }
  if (element.pagePath) {
    lines.push(`页面：${element.pagePath}`)
  }
  return lines.join('\n')
}

/**
 * 把选中元素写进发给后端的提示词。没有选中元素时原样返回用户输入。
 */
export const buildMessageWithElement = (userMessage: string, element: VisualElement | null) => {
  const content = userMessage.trim()
  if (!element) {
    return content
  }
  const lines = ['【用户选中的页面元素】', `标签：${element.tagName}`]
  if (element.selector) {
    lines.push(`选择器：${element.selector}`)
  }
  if (element.id) {
    lines.push(`id：${element.id}`)
  }
  if (element.className) {
    lines.push(`class：${element.className}`)
  }
  if (element.text) {
    lines.push(`文本：${element.text}`)
  }
  if (element.pagePath) {
    lines.push(`页面路径：${element.pagePath}`)
  }
  lines.push('')
  lines.push(content)
  return lines.join('\n')
}

/**
 * 可视化编辑状态。
 *
 * 对话页只切换编辑模式、展示选中元素、发送时拼进提示词。
 * iframe 注入和 postMessage 都留在这里。
 *
 * @param previewReady 预览区域是否已经可以展示网页
 */
export function useVisualEdit(previewReady: Ref<boolean>) {
  /** 是否处于编辑模式 */
  const editing = ref(false)
  /** 当前选中的元素，没有选中时为空 */
  const selectedElement = ref<VisualElement | null>(null)
  /** 预览文档是否已经加载完成，加载完成前不能进入编辑 */
  const previewLoaded = ref(false)
  /** 当前预览 iframe */
  let currentIframe: HTMLIFrameElement | null = null

  const selectionTitle = computed(() =>
    selectedElement.value ? formatElementTitle(selectedElement.value) : '',
  )
  const selectionDescription = computed(() =>
    selectedElement.value ? formatElementDescription(selectedElement.value) : '',
  )

  /**
   * 预览被收起时，编辑状态一并清掉
   */
  watch(previewReady, (ready) => {
    if (ready) {
      return
    }
    postToPreview(currentIframe, 'disable')
    currentIframe = null
    previewLoaded.value = false
    editing.value = false
    selectedElement.value = null
  })

  /**
   * 预览 iframe 即将重新加载（刷新或生成了新页面）
   * 旧文档上的选中状态不再有效，编辑模式保留到新页面加载完成后再恢复。
   */
  const markPreviewLoading = () => {
    currentIframe = null
    previewLoaded.value = false
    selectedElement.value = null
  }

  /**
   * 预览 iframe 加载完成
   */
  const handlePreviewLoad = (frame: HTMLIFrameElement) => {
    if (!frame.isConnected) {
      return
    }
    let sameOrigin = true
    try {
      const href = frame.contentWindow?.location.href ?? ''
      if (!href || href === 'about:blank') {
        return
      }
    } catch (error) {
      console.error('读取预览页面地址失败', error)
      sameOrigin = false
    }
    currentIframe = frame
    previewLoaded.value = true
    selectedElement.value = null
    if (!editing.value) {
      return
    }
    if (!sameOrigin) {
      editing.value = false
      message.error('无法编辑该预览页面，请确认预览网站与当前网站同域')
      return
    }
    try {
      injectPreviewEditor(frame)
      postToPreview(frame, 'enable')
    } catch (error) {
      console.error('恢复编辑模式失败', error)
      editing.value = false
      message.error('无法编辑该预览页面，请确认预览网站与当前网站同域')
    }
  }

  /**
   * 进入编辑模式
   */
  const enterEdit = () => {
    if (!currentIframe || !previewLoaded.value) {
      message.warning('请等待网页预览加载完成')
      return
    }
    try {
      injectPreviewEditor(currentIframe)
      postToPreview(currentIframe, 'enable')
      editing.value = true
    } catch (error) {
      console.error('进入编辑模式失败', error)
      editing.value = false
      const detail = error instanceof Error ? error.message : ''
      message.error(detail || '无法编辑该预览页面，请确认预览网站与当前网站同域')
    }
  }

  /**
   * 退出编辑模式，并清掉选中元素
   */
  const exitEdit = () => {
    editing.value = false
    selectedElement.value = null
    postToPreview(currentIframe, 'disable')
  }

  /**
   * 只取消选中，继续留在编辑模式
   */
  const clearSelection = () => {
    selectedElement.value = null
    postToPreview(currentIframe, 'clear')
  }

  /**
   * 切换编辑模式
   */
  const toggleEdit = () => {
    if (editing.value) {
      exitEdit()
      return
    }
    enterEdit()
  }

  /**
   * 接收预览页 postMessage 过来的选中元素
   */
  const handleMessage = (event: MessageEvent) => {
    if (!currentIframe || event.source !== currentIframe.contentWindow) {
      return
    }
    if (event.origin !== window.location.origin) {
      return
    }
    const data = event.data as Partial<PreviewEditMessage> | null
    if (!data || data.source !== PREVIEW_SOURCE || data.type !== MESSAGE_TYPE) {
      return
    }
    if (data.action !== 'select' || !editing.value) {
      return
    }
    const element = normalizeElement(data.element)
    if (!element) {
      return
    }
    selectedElement.value = element
  }

  /**
   * 按当前选中元素生成发给后端的提示词
   */
  const composeMessage = (userMessage: string) => {
    return buildMessageWithElement(userMessage, selectedElement.value)
  }

  onMounted(() => {
    window.addEventListener('message', handleMessage)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('message', handleMessage)
    exitEdit()
  })

  return {
    editing,
    selectedElement,
    previewLoaded,
    selectionTitle,
    selectionDescription,
    toggleEdit,
    clearSelection,
    exitEdit,
    handlePreviewLoad,
    markPreviewLoading,
    composeMessage,
  }
}
