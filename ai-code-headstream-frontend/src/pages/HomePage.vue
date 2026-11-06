<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PaperClipOutlined } from '@ant-design/icons-vue'
import { addApp, deleteApp, listMyAppVoByPage, listSquareAppVoByPage } from '@/api/appController.ts'
import { getAppDeployUrl, APP_LABEL_OPTIONS } from '@/utils/app.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useAppList } from '@/composables/useAppList.ts'
import AppListSection from '@/components/AppListSection.vue'
import SendButton from '@/components/SendButton.vue'
import ToolButton from '@/components/ToolButton.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 提示词
const promptText = ref('')
const creating = ref(false)

// 快捷示例（常见网站生成需求）
const quickPrompts = [
  {
    title: '音乐节宣传页',
    prompt:
      '帮我创建一个夏季音乐节活动宣传页面，包含活动倒计时、演出阵容介绍与嘉宾照片墙、购票按钮及票档价格、场地位置与交通指引、往届回顾图集和常见问题模块，整体采用深色霓虹科技风配色，突出音乐氛围，并适配手机端浏览。',
  },
  {
    title: '科技公司官网',
    prompt:
      '帮我创建一个科技公司企业官网，包含公司介绍、核心产品与服务展示、解决方案分栏介绍、团队成员头像墙、客户案例与合作品牌标识、新闻动态列表以及联系我们表单，整体采用简洁的蓝白科技风配色，导航栏固定在顶部并带滚动高亮效果。',
  },
  {
    title: '预约商城网站',
    prompt:
      '帮我创建一个在线预约与商城下单网站，包含商品分类筛选、商品详情图与规格选择、购物车与结算流程、在线预约时间选择、订单状态查询和会员积分展示，首页有轮播图与热销榜单，支持移动端自适应布局。',
  },
  {
    title: '运营数据后台',
    prompt:
      '帮我创建一个运营数据管理后台，包含左侧可折叠菜单、顶部搜索与消息通知、核心指标统计卡片、趋势折线图与占比饼图、明细数据表格支持搜索和分页、角色权限配置页面，整体采用深色大屏可视化风格。',
  },
]

// 未登录跳转登录页时，暂存用户填写的创建想法
const PROMPT_CACHE_KEY = 'home:initPrompt'

// 我的应用（仅登录后可见）
const isLogin = computed(() => !!loginUserStore.loginUser?.id)
const {
  apps: myApps,
  total: myTotal,
  pageNum: myPageNum,
  pageSize: myPageSize,
  fetchList: fetchMyApps,
  changePage: changeMyPage,
  refreshAfterDelete: refreshMyApps,
} = useAppList(listMyAppVoByPage, { errorTip: '获取我的应用失败', pageSize: 8 })

// 案例广场（所有用户公开的应用，精选应用排在最前面）
const {
  apps: squareApps,
  total: squareTotal,
  label: squareLabel,
  pageNum: squarePageNum,
  pageSize: squarePageSize,
  fetchList: fetchSquareApps,
  changeLabel: changeSquareLabel,
  changePage: changeSquarePage,
} = useAppList(listSquareAppVoByPage, { errorTip: '获取案例广场应用失败', pageSize: 8 })

onMounted(() => {
  // 登录跳转回来后，恢复之前填写的创建想法
  const cachedPrompt = sessionStorage.getItem(PROMPT_CACHE_KEY)
  if (cachedPrompt) {
    promptText.value = cachedPrompt
    sessionStorage.removeItem(PROMPT_CACHE_KEY)
  }
  fetchSquareApps()
  // 已登录时加载我的作品
  if (isLogin.value) {
    fetchMyApps()
  }
})

// 登录状态变化时（比如刚登录成功），加载我的作品
watch(isLogin, (val) => {
  if (val) {
    fetchMyApps()
  } else {
    myApps.value = []
    myTotal.value = 0
  }
})

/**
 * 校验是否已登录，未登录时返回 false
 */
const checkLogin = async () => {
  if (loginUserStore.loginUser?.id) {
    return true
  }
  try {
    await loginUserStore.fetchLoginUser()
  } catch (error) {
    console.error('获取登录用户信息失败', error)
  }
  return !!loginUserStore.loginUser?.id
}

/**
 * 创建应用，并跳转到应用生成对话页
 */
const doCreate = async () => {
  const initPrompt = promptText.value.trim()
  if (!initPrompt) {
    message.warning('请输入你的想法')
    return
  }
  // 创建应用前先校验登录状态，未登录跳转登录页
  if (!(await checkLogin())) {
    message.warning('请先登录后再创建应用')
    sessionStorage.setItem(PROMPT_CACHE_KEY, initPrompt)
    await router.push({ path: '/user/login', query: { redirect: '/' } })
    return
  }
  creating.value = true
  try {
    const res = await addApp({
      initPrompt,
    })
    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功，正在生成中...')
      await router.push(`/app/chat/${res.data.data}`)
    } else {
      message.error('创建失败，' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败', error)
    message.error('创建失败，请检查后端服务是否已启动')
  } finally {
    creating.value = false
  }
}

/**
 * 使用快捷示例
 */
const useQuickPrompt = (prompt: string) => {
  promptText.value = prompt
}

/**
 * 打开应用对话页（查看详情）
 * 页面会自行判断是否需要自动发送初始提示词，这里不需要额外传参
 */
const openAppChat = (app: API.AppVO) => {
  if (!app.id) {
    return
  }
  router.push(`/app/chat/${app.id}`)
}

/**
 * 打开部署后的作品（新标签页）
 */
const openDeployedApp = (app: API.AppVO) => {
  const url = getAppDeployUrl(app.deployKey)
  if (!url) {
    message.warning('该应用暂未部署')
    return
  }
  window.open(url, '_blank')
}

/**
 * 重命名我自己的应用（接口调用在卡片内完成，这里同步列表数据）
 */
const renameMyApp = (app: API.AppVO, appName: string) => {
  app.appName = appName
}

/**
 * 删除我自己的应用
 */
const deleteMyApp = async (app: API.AppVO) => {
  if (!app.id) {
    return
  }
  const res = await deleteApp({ id: app.id as unknown as number })
  if (res.data.code === 0) {
    message.success('删除成功')
    refreshMyApps()
  } else {
    message.error('删除失败，' + res.data.message)
  }
}
</script>

<template>
  <main class="home-page">
    <!-- 首屏：标语 + 提示词输入框 -->
    <section class="hero">
      <div class="hero__inner">
        <h1 class="hero__title">AI 应用生成平台</h1>
        <p class="hero__subtitle">与 AI 对话轻松创建应用和网站</p>

        <div class="hero__editor">
          <a-textarea
            v-model:value="promptText"
            class="hero__textarea"
            :auto-size="{ minRows: 3, maxRows: 10 }"
            placeholder="帮我创建一个活动宣传页面"
            @press-enter="doCreate"
          />
          <div class="hero__toolbar">
            <tool-button :icon="PaperClipOutlined" label="上传" filled />
            <a-tooltip :title="promptText.trim() ? '开始生成' : '请先输入你的想法'">
              <span class="hero__send-wrap">
                <send-button
                  gradient
                  :size="42"
                  :loading="creating"
                  :disabled="creating || !promptText.trim()"
                  @click="doCreate"
                />
              </span>
            </a-tooltip>
          </div>
        </div>

        <div class="hero__quick">
          <div
            v-for="item in quickPrompts"
            :key="item.title"
            class="hero__quick-card"
            @click="useQuickPrompt(item.prompt)"
          >
            <div class="hero__quick-title">{{ item.title }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 我的作品（仅登录后可见） -->
    <app-list-section
      v-if="isLogin"
      v-model:pageNum="myPageNum"
      title="我的作品"
      :apps="myApps"
      :total="myTotal"
      :page-size="myPageSize"
      empty-text="还没有应用，快去创建一个吧"
      author-fallback="我"
      editable
      @page-change="changeMyPage"
      @view-chat="openAppChat"
      @view-deploy="openDeployedApp"
      @rename="renameMyApp"
      @delete="deleteMyApp"
    />

    <!-- 案例广场（用户公开的应用，精选应用排在最前面，可按标签筛选） -->
    <app-list-section
      v-model:pageNum="squarePageNum"
      v-model:label="squareLabel"
      title="案例广场"
      :apps="squareApps"
      :total="squareTotal"
      :page-size="squarePageSize"
      :label-options="APP_LABEL_OPTIONS"
      empty-text="暂无公开应用"
      author-fallback="官方"
      @label-change="changeSquareLabel"
      @page-change="changeSquarePage"
      @view-chat="openAppChat"
      @view-deploy="openDeployedApp"
    />
  </main>
</template>

<style scoped>
/* 主页内容（灰色背景由 BasicLayout 提供） */
.home-page {
  position: relative;
  z-index: 1;
  padding-bottom: 40px;
}

.hero {
  position: relative;
  padding: 64px 0 32px;
  text-align: center;
}

.hero__inner {
  max-width: 1120px;
  margin: 0 auto;
}

.hero__title {
  margin: 0;
  font-size: 52px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #262626;
}

.hero__subtitle {
  margin: 14px 0 36px;
  font-size: 17px;
  letter-spacing: 1px;
  color: rgba(0, 0, 0, 0.45);
}

.hero__editor {
  padding: 14px 16px 12px;
  text-align: left;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

/* hero__textarea 类直接挂在 textarea 元素上 */
.hero__textarea.ant-input {
  padding: 6px 4px;
  font-size: 16px;
  color: rgba(0, 0, 0, 0.88);
  background: transparent;
  border: none;
  box-shadow: none;
  resize: none;
}

.hero__textarea.ant-input::placeholder {
  color: rgba(0, 0, 0, 0.35);
}

.hero__textarea.ant-input:hover,
.hero__textarea.ant-input:focus,
.hero__textarea.ant-input-focused {
  background: transparent;
  border: none;
  box-shadow: none;
}

.hero__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.hero__send-wrap {
  display: inline-flex;
  line-height: 0;
}

.hero__quick {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  width: 50%;
  margin: 28px auto 0;
}

.hero__quick-card {
  padding: 14px 16px;
  cursor: pointer;
  text-align: left;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  transition:
    background 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease;
}

.hero__quick-card:hover {
  background: #fafafa;
  border-color: #d4d4d8;
  transform: translateY(-3px);
}

.hero__quick-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

@media (max-width: 1024px) {
  .hero__quick {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero {
    padding: 40px 0 24px;
  }

  .hero__title {
    font-size: 32px;
    letter-spacing: 1px;
  }

  .hero__subtitle {
    margin: 10px 0 24px;
    font-size: 14px;
  }

  .hero__quick {
    grid-template-columns: 1fr;
    width: 100%;
  }
}
</style>
