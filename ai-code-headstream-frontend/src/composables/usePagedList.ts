import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { ApiResponse, PageResult } from '@/types/api.ts'

/** 分页查询参数 */
export interface PagedQuery {
  pageNum?: number
  pageSize?: number
}

/**
 * 管理端表格分页列表
 *
 * 封装「列表数据 + 搜索条件 + 分页」的通用逻辑，管理页只需要：
 * 1. 传入请求函数
 * 2. 传入初始搜索条件（重置时恢复为该值）
 */
export function usePagedList<T, Q extends PagedQuery = PagedQuery>(
  fetcher: (params: Q) => Promise<ApiResponse<PageResult<T>>>,
  initialQuery: Partial<Q> = {},
  options: { errorTip?: string; pageSize?: number } = {},
) {
  const errorTip = options.errorTip ?? '获取数据失败'
  // 初始条件快照，重置时恢复
  const initial = {
    pageNum: 1,
    pageSize: options.pageSize ?? 10,
    ...initialQuery,
  } as Q

  // 搜索条件
  const searchParams = reactive({ ...initial }) as unknown as Q
  // 列表数据
  const data = ref<T[]>([])
  const total = ref(0)

  /**
   * 获取数据
   */
  const fetchData = async () => {
    try {
      const res = await fetcher({ ...searchParams })
      if (res.data.code === 0 && res.data.data) {
        data.value = res.data.data.records ?? []
        total.value = res.data.data.totalRow ?? 0
      } else {
        message.error(`${errorTip}，` + res.data.message)
      }
    } catch (error) {
      console.error(errorTip, error)
      message.error(`${errorTip}，请检查后端服务是否已启动`)
    }
  }

  // 分页参数
  const pagination = computed(() => ({
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (count: number) => `共 ${count} 条`,
  }))

  /**
   * 表格分页/排序变化
   */
  const doTableChange = (page: { current?: number; pageSize?: number }) => {
    searchParams.pageNum = page.current
    searchParams.pageSize = page.pageSize
    fetchData()
  }

  /**
   * 搜索（回到第一页）
   */
  const doSearch = () => {
    searchParams.pageNum = 1
    fetchData()
  }

  /**
   * 重置搜索条件
   */
  const doReset = () => {
    Object.assign(searchParams, initial)
    fetchData()
  }

  return { data, total, searchParams, pagination, fetchData, doSearch, doReset, doTableChange }
}
