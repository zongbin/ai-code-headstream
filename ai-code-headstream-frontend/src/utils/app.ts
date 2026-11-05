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
 * 精选应用的优先级，与后端 AppConstant.GOOD_APP_PRIORITY 保持一致
 */
export const GOOD_APP_PRIORITY = 99

/**
 * 默认应用优先级，与后端 AppConstant.DEFAULT_APP_PRIORITY 保持一致
 */
export const DEFAULT_APP_PRIORITY = 0

/**
 * 解析后端时间。兼容 "yyyy-mm-dd HH:mm:ss" 和 "yyyy-mm-ddTHH:mm:ss"
 */
function parseTime(time: string) {
  const normalized = time.trim().replace('T', ' ').replace(/-/g, '/').replace(/\.\d+/, '')
  return new Date(normalized)
}

/**
 * 格式化时间：相对时间（如：5 小时前）
 */
export function formatFromNow(time?: string) {
  if (!time) {
    return '未知时间'
  }
  const date = parseTime(time)
  if (Number.isNaN(date.getTime())) {
    return time
  }
  const diff = Date.now() - date.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) {
    return '刚刚'
  }
  if (diff < hour) {
    return `${Math.floor(diff / minute)} 分钟前`
  }
  if (diff < day) {
    return `${Math.floor(diff / hour)} 小时前`
  }
  if (diff < 30 * day) {
    return `${Math.floor(diff / day)} 天前`
  }
  return date.toLocaleDateString()
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
