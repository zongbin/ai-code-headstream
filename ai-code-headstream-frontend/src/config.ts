/**
 * 前端访问后端的地址。
 *
 * 开发时读取 .env.development，打包部署时读取 .env.production。
 * 业务代码只从这里取值，不要直接写死地址。
 */

function readUrl(name: string, value: string | undefined) {
  const url = value?.trim().replace(/\/+$/, '')
  if (!url) {
    throw new Error(`缺少环境变量 ${name}，请检查 .env.development 或 .env.production`)
  }
  return url
}

/** 后端接口根地址。普通请求、流式生成、代码预览都走这里。 */
export const API_BASE_URL = readUrl('VITE_API_BASE_URL', import.meta.env.VITE_API_BASE_URL)

/**
 * 已部署应用的访问根地址。
 * 后端把作品复制到部署目录后，通过 {DEPLOY_BASE_URL}/{deployKey}/ 打开。
 */
export const DEPLOY_BASE_URL = readUrl('VITE_DEPLOY_BASE_URL', import.meta.env.VITE_DEPLOY_BASE_URL)
