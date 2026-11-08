/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 后端接口根地址 */
  readonly VITE_API_BASE_URL: string
  /** 已部署应用的访问根地址 */
  readonly VITE_DEPLOY_BASE_URL: string
}
