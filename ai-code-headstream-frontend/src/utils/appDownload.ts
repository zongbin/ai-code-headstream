import axios, { type AxiosResponse } from 'axios'
import request from '@/request'

/**
 * 下载应用代码（ZIP）
 *
 * 后端成功时直接把压缩包写入响应流：
 * - Content-Type: application/zip
 * - Content-Disposition: attachment; filename="{appId}.zip"
 *
 * 失败时全局异常处理返回的是 JSON（code / message），响应头不是 zip。
 * 这里按响应头区分，避免把错误信息保存成压缩包。
 *
 * 注意：src/api 目录下的文件由 openapi2ts 自动生成，请勿在该目录下新增手写文件。
 * 该接口的响应体是二进制流，生成代码按 JSON 处理，因此在这里单独适配响应头。
 *
 * @param appId 应用 ID（按字符串传递，避免 Long 精度丢失）
 */
export async function downloadAppCodeZip(appId: string) {
  let res: AxiosResponse<Blob>
  try {
    res = await request<Blob>(`/app/download/${encodeURIComponent(appId)}`, {
      method: 'GET',
      responseType: 'blob',
    })
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.data instanceof Blob) {
      throw new Error(await readErrorMessage(error.response.data))
    }
    throw new Error('请检查后端服务是否已启动')
  }
  const blob = res.data
  const contentType = readHeader(res, 'content-type') || blob.type || ''
  if (contentType.includes('application/json') || contentType.includes('text/')) {
    throw new Error(await readErrorMessage(blob))
  }
  const fileName = parseFileName(readHeader(res, 'content-disposition')) || `${appId}.zip`
  saveBlob(blob, fileName)
}

/**
 * 读取响应头。axios 会把头名称转成小写。
 */
const readHeader = (res: AxiosResponse, name: string) => {
  const value = res.headers?.[name]
  return typeof value === 'string' ? value : ''
}

/**
 * 从 Content-Disposition 中取出文件名。
 * 后端格式为：attachment; filename="123.zip"
 */
const parseFileName = (contentDisposition: string) => {
  if (!contentDisposition) {
    return ''
  }
  const quoted = /filename="([^"]+)"/i.exec(contentDisposition)
  const plain = /filename=([^;]+)/i.exec(contentDisposition)
  const raw = (quoted?.[1] ?? plain?.[1] ?? '').trim()
  if (!raw) {
    return ''
  }
  try {
    return decodeURIComponent(raw)
  } catch {
    return raw
  }
}

/**
 * 把错误响应体里的 message 取出来。
 */
const readErrorMessage = async (blob: Blob) => {
  try {
    const text = await blob.text()
    if (!text) {
      return '请稍后重试'
    }
    const json = JSON.parse(text) as { message?: string }
    return json.message || '请稍后重试'
  } catch {
    return '请稍后重试'
  }
}

/**
 * 触发浏览器下载
 */
const saveBlob = (blob: Blob, fileName: string) => {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
