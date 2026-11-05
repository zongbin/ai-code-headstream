/**
 * 后端接口的通用响应类型
 *
 * 后端统一返回 { code, data, message } 结构，字段均为可选，
 * 这里只声明列表/分页场景用到的部分，避免在业务代码里大量使用 any。
 */

/** 统一响应结构 */
export interface ApiResult<T> {
  code?: number
  data?: T
  message?: string
}

/** 分页结果 */
export interface PageResult<T> {
  records?: T[]
  totalRow?: number
}

/** axios 响应（只关心 data 字段） */
export interface ApiResponse<T> {
  data: ApiResult<T>
}
