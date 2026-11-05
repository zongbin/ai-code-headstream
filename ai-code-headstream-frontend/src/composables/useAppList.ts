import { ref } from 'vue'
import { message } from 'ant-design-vue'
import type { ApiResponse, PageResult } from '@/types/api.ts'

/**
 * 应用列表（首页「我的作品」「精选案例」通用）
 *
 * 封装「列表 + 关键字搜索 + 分页」的通用逻辑，
 * 传入不同的请求函数即可复用于不同的应用列表。
 */
export function useAppList(
  fetcher: (params: API.AppQueryRequest) => Promise<ApiResponse<PageResult<API.AppVO>>>,
  options: { errorTip: string; pageSize?: number },
) {
  // 列表数据
  const apps = ref<API.AppVO[]>([])
  const total = ref(0)
  // 搜索关键字
  const searchName = ref('')
  // 分页
  const pageNum = ref(1)
  const pageSize = options.pageSize ?? 6

  /**
   * 获取应用列表
   */
  const fetchList = async () => {
    try {
      const res = await fetcher({
        pageNum: pageNum.value,
        pageSize,
        appName: searchName.value || undefined,
      })
      if (res.data.code === 0 && res.data.data) {
        apps.value = res.data.data.records ?? []
        total.value = res.data.data.totalRow ?? 0
      } else {
        message.error(`${options.errorTip}，` + res.data.message)
      }
    } catch (error) {
      console.error(options.errorTip, error)
    }
  }

  /**
   * 搜索（回到第一页）
   */
  const search = () => {
    pageNum.value = 1
    fetchList()
  }

  /**
   * 翻页
   */
  const changePage = (page: number) => {
    pageNum.value = page
    fetchList()
  }

  /**
   * 删除后刷新（删掉当前页最后一条时自动回到上一页）
   */
  const refreshAfterDelete = () => {
    if (apps.value.length === 1 && pageNum.value > 1) {
      pageNum.value -= 1
    }
    fetchList()
  }

  return {
    apps,
    total,
    searchName,
    pageNum,
    pageSize,
    fetchList,
    search,
    changePage,
    refreshAfterDelete,
  }
}
