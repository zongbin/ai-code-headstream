import { API_BASE_URL } from '@/request'

/**
 * 应用主键
 *
 * 后端为了避免 Long 精度丢失，会把主键序列化成字符串，
 * 所以主键一律按字符串处理，不要转成 number（会丢失精度）。
 */
export type AppId = string

/**
 * 应用静态资源（预览页面）地址
 * 后端会把生成的代码保存到 {codeGenType}_{appId} 目录下，通过 /static 暴露
 */
export function getAppPreviewUrl(codeGenType?: string, appId?: AppId | number) {
  if (!codeGenType || !appId) {
    return ''
  }
  if(codeGenType === 'vue_project'){
    return `${API_BASE_URL}/static/${codeGenType}_${appId}/dist/index.html`
  }
  return `${API_BASE_URL}/static/${codeGenType}_${appId}/`
}

/**
 * 应用部署地址（查看作品）
 * 后端部署后会把代码复制到部署目录，并通过 http://localhost/{deployKey}/ 访问。
 * 注意：部署地址与生成网站的预览地址（getAppPreviewUrl）不同，不要混用。
 */
export function getAppDeployUrl(deployKey?: string) {
  if (!deployKey) {
    return ''
  }
  return `http://localhost/${deployKey}/`
}

/**
 * 精选应用的优先级，与后端 AppConstant.GOOD_APP_PRIORITY 保持一致。
 */
export const GOOD_APP_PRIORITY = 99

/**
 * 置顶应用的优先级，与后端 AppConstant.TOP_APP_PRIORITY 保持一致。
 * 置顶通过把优先级标记为该值实现，与精选（99）互斥。
 */
export const PINNED_APP_PRIORITY = 999

/**
 * 默认应用优先级，与后端 AppConstant.DEFAULT_APP_PRIORITY 保持一致
 */
export const DEFAULT_APP_PRIORITY = 0

/**
 * 应用是否为精选（优先级为 99）
 */
export function isGoodApp(app?: API.AppVO) {
  return app?.priority === GOOD_APP_PRIORITY
}

/**
 * 应用是否为置顶（优先级为 999）
 */
export function isPinnedApp(app?: API.AppVO) {
  return app?.priority === PINNED_APP_PRIORITY
}

/**
 * 应用可见状态：私有，与后端 AppConstant.APP_VISIBLE_PRIVATE 保持一致
 */
export const APP_VISIBLE_PRIVATE = 0

/**
 * 应用可见状态：公开，与后端 AppConstant.APP_VISIBLE_PUBLIC 保持一致
 */
export const APP_VISIBLE_PUBLIC = 1

/**
 * 应用是否公开
 */
export function isPublicApp(app?: API.AppVO) {
  return app?.visible === APP_VISIBLE_PUBLIC
}

/**
 * 应用可见状态文案
 */
export function getAppVisibleText(visible?: number) {
  return visible === APP_VISIBLE_PUBLIC ? '公开' : '私有'
}

/**
 * 代码生成类型，与后端 CodeGenTypeEnum 保持一致
 */
export const CODE_GEN_TYPE_OPTIONS = [
  { value: 'html', text: '原生 HTML 模式', color: 'green' },
  { value: 'multi_file', text: '原生多文件模式', color: 'blue' },
  { value: 'vue_project', text: 'Vue 工程模式', color: 'purple' },
] as const

/**
 * 代码生成类型文案
 */
export function getCodeGenTypeText(codeGenType?: string) {
  if (!codeGenType) {
    return '-'
  }
  return CODE_GEN_TYPE_OPTIONS.find((item) => item.value === codeGenType)?.text ?? codeGenType
}

/**
 * 代码生成类型标签颜色
 */
export function getCodeGenTypeColor(codeGenType?: string) {
  return CODE_GEN_TYPE_OPTIONS.find((item) => item.value === codeGenType)?.color ?? 'default'
}

/**
 * 应用标签枚举值，与后端 AppLabelEnum 保持一致。
 */
export const APP_LABELS = [
  '工具',
  '网站',
  '数据分析',
  '活动页面',
  '管理平台',
  '用户应用',
  '个人管理',
  '游戏',
] as const

/** 标签筛选项 */
export interface AppLabelOption {
  /** 传给后端的标签值，空字符串表示全部 */
  value: string
  /** 页面展示文案 */
  text: string
}

/**
 * 标签筛选项（首项为「全部」，展示所有内容）
 */
export const APP_LABEL_OPTIONS: AppLabelOption[] = [
  { value: '', text: '全部' },
  ...APP_LABELS.map((label) => ({ value: label, text: label })),
]

/**
 * 解析后端时间。兼容 "yyyy-mm-dd HH:mm:ss" 和 "yyyy-mm-ddTHH:mm:ss"
 */
function parseTime(time: string) {
  const normalized = time.trim().replace('T', ' ').replace(/-/g, '/').replace(/\.\d+/, '')
  return new Date(normalized)
}

/** 后端未返回封面时使用的占位图 */
const PLACEHOLDER_COVER = 'https://picsum.photos/320/180'

/**
 * 应用封面地址
 *
 * 有封面用接口返回的地址，没有则用占位图。
 * 占位图按应用 id 区分，避免同一地址被浏览器缓存成同一张图。
 */
export function getAppCoverUrl(app: API.AppVO) {
  const cover = app.cover?.trim()
  if (cover) {
    return cover
  }
  return `${PLACEHOLDER_COVER}?random=${encodeURIComponent(String(app.id ?? 'app'))}`
}

/**
 * 格式化时间：日期（yyyy-mm-dd）
 */
export function formatDate(time?: string) {
  if (!time) {
    return ''
  }
  const date = parseTime(time)
  if (Number.isNaN(date.getTime())) {
    return time
  }
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/**
 * 格式化时间：日期时间
 */
export function formatDateTime(time?: string) {
  if (!time) {
    return '-'
  }
  const date = parseTime(time)
  if (Number.isNaN(date.getTime())) {
    return time
  }
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
