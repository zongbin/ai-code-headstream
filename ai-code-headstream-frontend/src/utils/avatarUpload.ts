import request from '@/request'

/**
 * 上传用户头像
 *
 * 注意：src/api 目录下的文件由 openapi2ts 自动生成，请勿在该目录下新增手写文件。
 * 自动生成的 uploadAvatar 会固定带上 `Content-Type: application/json`，
 * 而 axios 在该 Content-Type 下会把 FormData 序列化成 JSON（JSON.stringify(formDataToJSON(data))），
 * 导致后端收不到 multipart 请求（Current request is not a multipart request）。
 * 因此这里直接使用 request 实例发起 multipart/form-data 请求，由浏览器自动补充 boundary。
 *
 * @param file 头像文件
 */
export async function uploadAvatarFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request<API.BaseResponseString>('/user/avatar/upload', {
    method: 'POST',
    data: formData,
  })
}
