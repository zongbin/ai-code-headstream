/**
 * Markdown 渲染工具
 *
 * 使用主流的 markdown-it 解析 Markdown，配合 highlight.js 对代码块做语法高亮，
 * 最后用 DOMPurify 过滤危险 HTML，保证渲染结果安全。
 */

import MarkdownIt from 'markdown-it'
import type { Token } from 'markdown-it'
import hljs from 'highlight.js/lib/common'
import DOMPurify from 'dompurify'

/** 代码块语言归一化（把常见的别名映射到 highlight.js 支持的语言名） */
const LANG_ALIAS: Record<string, string> = {
  html: 'xml',
  htm: 'xml',
  vue: 'xml',
  svg: 'xml',
  js: 'javascript',
  jsx: 'javascript',
  mjs: 'javascript',
  cjs: 'javascript',
  ts: 'typescript',
  tsx: 'typescript',
  sh: 'bash',
  shell: 'bash',
  zsh: 'bash',
  yml: 'yaml',
  md: 'markdown',
  py: 'python',
  rb: 'ruby',
  kt: 'kotlin',
  golang: 'go',
}

/** 根据语言推断默认文件名（与后端保存的文件名保持一致） */
const LANG_FILE_NAME: Record<string, string> = {
  html: 'index.html',
  css: 'style.css',
  js: 'script.js',
  javascript: 'script.js',
}

/** 从文本中提取文件名（如 index.html / src/App.vue） */
const FILE_NAME_REGEX = /([\w./-]+\.(?:html?|css|jsx?|tsx?|vue|json|md|ya?ml|txt))/i

/** 语言与文件扩展名的对应关系，用于校验推断出来的文件名是否匹配 */
const LANG_EXTENSIONS: Record<string, string[]> = {
  html: ['html', 'htm'],
  css: ['css'],
  js: ['js', 'jsx', 'mjs', 'cjs'],
  javascript: ['js', 'jsx', 'mjs', 'cjs'],
  ts: ['ts', 'tsx'],
  typescript: ['ts', 'tsx'],
  vue: ['vue'],
}

/**
 * 校验文件名是否与代码块语言匹配
 * @param filename 文件名
 * @param lang 代码块语言
 */
function matchLanguage(filename: string, lang: string): boolean {
  const extensions = LANG_EXTENSIONS[lang.toLowerCase()]
  if (!extensions) {
    return true
  }
  const matched = filename.toLowerCase().match(/\.([\w]+)$/)?.[1]
  return matched ? extensions.includes(matched) : false
}

/** 转义 HTML 特殊字符 */
const escapeHtml = (text: string) =>
  text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')

/**
 * 对代码做语法高亮
 * @param code 代码内容
 * @param lang 代码语言（可为空）
 */
function highlightCode(code: string, lang: string): string {
  const normalized = LANG_ALIAS[lang.toLowerCase()] ?? lang.toLowerCase()
  if (normalized && hljs.getLanguage(normalized)) {
    try {
      return hljs.highlight(code, { language: normalized, ignoreIllegals: true }).value
    } catch {
      // 高亮失败时退回纯文本
    }
  }
  return escapeHtml(code)
}

/**
 * 从代码块前面的文本里推断文件名
 *
 * markdown-it 的 block token 是扁平的，一个段落由 paragraph_open / inline / paragraph_close
 * 三个同级 token 组成，其中 inline token 里带原始文本。
 *
 * @param tokens markdown-it 解析出的 token 列表
 * @param index 当前代码块 token 的下标
 */
function guessFileName(tokens: Token[], index: number): string {
  // 向前查找最近的文本内容
  for (let i = index - 1; i >= 0; i--) {
    const token = tokens[i]
    if (!token) {
      continue
    }
    if (token.type === 'fence') {
      break
    }
    if (token.type === 'inline') {
      const matched = token.content.match(FILE_NAME_REGEX)
      if (matched?.[1]) {
        return matched[1]
      }
    }
  }
  return ''
}

const md = new MarkdownIt({
  // 不解析 Markdown 中的裸 HTML，避免 AI 输出里的原始标签破坏聊天区排版；
  // 需要展示的 HTML/CSS/JS 都放在代码块里，由 highlight.js 高亮。
  html: false,
  linkify: true,
  breaks: true,
})

/** 每次渲染的代码块序号，用于生成稳定的 data-key */
let codeBlockSeq = 0

/**
 * 自定义代码块渲染：
 * 输出带文件名、语言标签、复制按钮的代码块容器（details/summary 实现展开收起）。
 */
md.renderer.rules.fence = (tokens, index) => {
  const token = tokens[index]
  if (!token) {
    return ''
  }
  const info = token.info.trim()
  const parts = info.split(/\s+/)
  const lang = parts[0] ?? ''
  const content = token.content
  // 文件名优先级：代码块标注 > 前面文本里推断（且需与语言匹配）> 按语言取默认名
  const guessed = guessFileName(tokens, index)
  const filename =
    parts[1] ||
    (guessed && matchLanguage(guessed, lang) ? guessed : '') ||
    LANG_FILE_NAME[lang.toLowerCase()] ||
    '代码文件'
  const code = highlightCode(content, lang)
  const normalizedLang = LANG_ALIAS[lang.toLowerCase()] ?? lang.toLowerCase()
  const key = `code-${codeBlockSeq++}`

  return [
    `<details class="md-code" data-key="${key}" open>`,
    `<summary class="md-code__header">`,
    `<span class="md-code__arrow"></span>`,
    `<span class="md-code__name">${escapeHtml(filename)}</span>`,
    lang ? `<span class="md-code__lang">${escapeHtml(lang)}</span>` : '',
    `<button type="button" class="md-code__copy" aria-label="复制代码" title="复制代码">`,
    `<svg class="md-code__copy-icon" viewBox="64 64 896 896" width="14" height="14" aria-hidden="true" focusable="false"><path fill="currentColor" d="M832 64H296c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h496v688c0 4.4 3.6 8 8 8h56c4.4 0 8-3.6 8-8V96c0-17.7-14.3-32-32-32zM704 192H192c-17.7 0-32 14.3-32 32v530.7c0 8.5 3.4 16.6 9.4 22.6l173.3 173.3c2.2 2.2 4.7 4 7.4 5.5v1.9h4.2c3.5 1.3 7.2 2 11 2H704c17.7 0 32-14.3 32-32V224c0-17.7-14.3-32-32-32zM350 856.2L263.9 770H350v86.2zM664 888H414V746c0-22.1-17.9-40-40-40H232V264h432v624z"/></svg>`,
    `<span class="md-code__copy-label">复制</span>`,
    `</button>`,
    `</summary>`,
    `<pre class="md-code__body"><code class="hljs language-${escapeHtml(
      normalizedLang || 'plaintext',
    )}">${code}</code></pre>`,
    `</details>`,
  ].join('')
}

/**
 * DOMPurify 过滤配置
 *
 * 默认配置已经保留了 class（highlight.js 高亮依赖它）与 button/details/summary 等标签，
 * 这里只额外放开链接的 target 属性，并禁止脚本、样式表等危险标签。
 */
const PURIFY_CONFIG = {
  ADD_ATTR: ['target', 'rel'],
  FORBID_TAGS: ['script', 'style', 'iframe', 'object', 'embed', 'form', 'input'],
}

/**
 * 把 Markdown 文本渲染成安全的 HTML
 * @param source Markdown 原文
 */
export function renderMarkdown(source: string): string {
  if (!source) {
    return ''
  }
  codeBlockSeq = 0
  const html = md.render(source)
  return DOMPurify.sanitize(html, PURIFY_CONFIG)
}
