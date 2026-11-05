<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowUpOutlined,
  DeleteOutlined,
  EditOutlined,
  EllipsisOutlined,
  PaperClipOutlined,
} from '@ant-design/icons-vue'
import { addApp, deleteApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController.ts'
import { formatDate, getAppDeployUrl } from '@/utils/app.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'

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
const myApps = ref<API.AppVO[]>([])
const myTotal = ref(0)
const mySearchName = ref('')
const mySearch = reactive({
  pageNum: 1,
  pageSize: 6,
})

// 精选应用
const goodApps = ref<API.AppVO[]>([])
const goodTotal = ref(0)
const goodSearchName = ref('')
const goodSearch = reactive({
  pageNum: 1,
  pageSize: 6,
})

/**
 * 获取我的应用列表
 */
const fetchMyApps = async () => {
  try {
    const res = await listMyAppVoByPage({
      pageNum: mySearch.pageNum,
      pageSize: mySearch.pageSize,
      appName: mySearchName.value || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records ?? []
      myTotal.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取我的应用失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取我的应用失败', error)
  }
}

/**
 * 获取精选应用列表
 */
const fetchGoodApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: goodSearch.pageNum,
      pageSize: goodSearch.pageSize,
      appName: goodSearchName.value || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      goodApps.value = res.data.data.records ?? []
      goodTotal.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取精选应用失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取精选应用失败', error)
  }
}

onMounted(() => {
  // 登录跳转回来后，恢复之前填写的创建想法
  const cachedPrompt = sessionStorage.getItem(PROMPT_CACHE_KEY)
  if (cachedPrompt) {
    promptText.value = cachedPrompt
    sessionStorage.removeItem(PROMPT_CACHE_KEY)
  }
  fetchGoodApps()
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
      message.error('创建应用失败，' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败', error)
    message.error('创建应用失败，请检查后端服务是否已启动')
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
 * 搜索我的应用
 */
const doSearchMyApps = () => {
  mySearch.pageNum = 1
  fetchMyApps()
}

/**
 * 我的应用翻页
 */
const onMyPageChange = (page: number) => {
  mySearch.pageNum = page
  fetchMyApps()
}

/**
 * 打开应用对话页（查看详情）
 * 带 view=1 表示只查看，不自动发送初始提示词
 */
const openAppChat = (app: API.AppVO) => {
  if (!app.id) {
    return
  }
  router.push(`/app/chat/${app.id}?view=1`)
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
 * 编辑我自己的应用
 */
const editMyApp = (app: API.AppVO) => {
  if (!app.id) {
    return
  }
  router.push(`/app/edit/${app.id}`)
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
    // 删掉当前页最后一条时，自动回到上一页
    if (myApps.value.length === 1 && mySearch.pageNum > 1) {
      mySearch.pageNum -= 1
    }
    fetchMyApps()
  } else {
    message.error('删除失败，' + res.data.message)
  }
}

/**
 * 搜索精选应用
 */
const doSearchGoodApps = () => {
  goodSearch.pageNum = 1
  fetchGoodApps()
}

/**
 * 精选应用翻页
 */
const onGoodPageChange = (page: number) => {
  goodSearch.pageNum = page
  fetchGoodApps()
}

/** 后端未返回封面时使用的占位图 */
const PLACEHOLDER_COVER = 'https://picsum.photos/320/180'

/**
 * 卡片封面地址。有封面用接口返回的地址，没有则用占位图。
 * 占位图按应用 id 区分，避免同一地址被浏览器缓存成同一张图。
 */
const coverUrl = (app: API.AppVO) => {
  const cover = app.cover?.trim()
  if (cover) {
    return cover
  }
  return `${PLACEHOLDER_COVER}?random=${encodeURIComponent(String(app.id ?? 'app'))}`
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
            <a-tooltip title="上传附件功能暂未开放">
              <span class="hero__tool-wrap">
                <a-button type="text" class="hero__tool-btn" disabled aria-label="上传附件">
                  <PaperClipOutlined />
                  <span>上传</span>
                </a-button>
              </span>
            </a-tooltip>
            <a-tooltip :title="promptText.trim() ? '开始生成' : '请先输入你的想法'">
              <span class="hero__send-wrap">
                <a-button
                  type="primary"
                  shape="circle"
                  class="hero__send"
                  :loading="creating"
                  :disabled="creating || !promptText.trim()"
                  aria-label="开始生成"
                  @click="doCreate"
                >
                  <template #icon>
                    <ArrowUpOutlined />
                  </template>
                </a-button>
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
    <section v-if="isLogin" class="section">
      <div class="section__header">
        <h2 class="section__title">我的作品</h2>
        <a-input-search
          v-model:value="mySearchName"
          class="section__search"
          placeholder="搜索应用名称"
          allow-clear
          @search="doSearchMyApps"
        />
      </div>

      <a-empty v-if="myApps.length === 0" description="还没有应用，快去创建一个吧" />
      <div v-else class="app-grid">
        <div v-for="app in myApps" :key="app.id" class="app-card">
          <div class="app-card__cover">
            <img class="app-card__cover-img" :src="coverUrl(app)" :alt="app.appName" />
            <div class="app-card__cover-actions" @click.stop>
              <a-button size="small" class="app-card__cover-btn" @click="openAppChat(app)">
                查看对话
              </a-button>
              <a-button
                v-if="app.deployKey"
                size="small"
                class="app-card__cover-btn"
                @click="openDeployedApp(app)"
              >
                查看作品
              </a-button>
            </div>
          </div>
          <div class="app-card__body">
            <a-avatar
              class="app-card__avatar"
              :size="40"
              :src="app.user?.userAvatar"
              :style="{ backgroundColor: '#40a9ff' }"
            >
              {{ app.user?.userName?.slice(0, 1) ?? 'U' }}
            </a-avatar>
            <div class="app-card__info">
              <div class="app-card__name">{{ app.appName }}</div>
              <div class="app-card__meta">
                <span class="app-card__author-name">{{ app.user?.userName ?? '我' }}</span>
                <span v-if="app.createTime" class="app-card__time">
                  {{ formatDate(app.createTime) }}
                </span>
                <span class="app-card__more" @click.stop>
                  <a-dropdown :trigger="['click']">
                    <a-button type="text" class="app-card__more-btn" aria-label="更多操作">
                      <EllipsisOutlined />
                    </a-button>
                    <template #overlay>
                      <a-menu class="app-card__more-menu">
                        <a-menu-item @click="editMyApp(app)">
                          <EditOutlined />
                          编辑
                        </a-menu-item>
                        <a-menu-item class="app-card__more-menu-item--danger">
                          <a-popconfirm
                            title="确定删除该应用？"
                            ok-text="确定"
                            cancel-text="取消"
                            @confirm="deleteMyApp(app)"
                          >
                            <span class="app-card__more-menu-trigger">
                              <DeleteOutlined />
                              删除
                            </span>
                          </a-popconfirm>
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="myTotal > mySearch.pageSize" class="section__pagination">
        <a-pagination
          :current="mySearch.pageNum"
          :page-size="mySearch.pageSize"
          :total="myTotal"
          show-less-items
          @change="onMyPageChange"
        />
      </div>
    </section>

    <!-- 精选案例 -->
    <section class="section">
      <div class="section__header">
        <h2 class="section__title">精选案例</h2>
        <a-input-search
          v-model:value="goodSearchName"
          class="section__search"
          placeholder="搜索应用名称"
          allow-clear
          @search="doSearchGoodApps"
        />
      </div>

      <a-empty v-if="goodApps.length === 0" description="暂无精选应用" />
      <div v-else class="app-grid">
        <div
          v-for="app in goodApps"
          :key="app.id"
          class="app-card app-card--good"
        >
          <div class="app-card__cover">
            <img class="app-card__cover-img" :src="coverUrl(app)" :alt="app.appName" />
            <div class="app-card__cover-actions" @click.stop>
              <a-button size="small" class="app-card__cover-btn" @click="openAppChat(app)">
                查看对话
              </a-button>
              <a-button
                v-if="app.deployKey"
                size="small"
                class="app-card__cover-btn"
                @click="openDeployedApp(app)"
              >
                查看作品
              </a-button>
            </div>
          </div>
          <div class="app-card__body">
            <a-avatar
              class="app-card__avatar"
              :size="40"
              :src="app.user?.userAvatar"
              :style="{ backgroundColor: '#40a9ff' }"
            >
              {{ app.user?.userName?.slice(0, 1) ?? 'U' }}
            </a-avatar>
            <div class="app-card__info">
              <div class="app-card__name">{{ app.appName }}</div>
              <div class="app-card__meta">
                <span class="app-card__author-name">{{ app.user?.userName ?? '官方' }}</span>
                <span v-if="app.createTime" class="app-card__time">
                  {{ formatDate(app.createTime) }}
                </span>
              </div>
            </div>
            <a-tag color="blue" class="app-card__tag">精选</a-tag>
          </div>
        </div>
      </div>

      <div v-if="goodTotal > goodSearch.pageSize" class="section__pagination">
        <a-pagination
          :current="goodSearch.pageNum"
          :page-size="goodSearch.pageSize"
          :total="goodTotal"
          show-less-items
          @change="onGoodPageChange"
        />
      </div>
    </section>
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

.hero__tool-wrap,
.hero__send-wrap {
  display: inline-flex;
  line-height: 0;
}

.hero__tool-wrap {
  cursor: not-allowed;
}

.hero__tool-btn {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  background: #f5f5f5;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition:
    color 0.2s ease,
    background 0.2s ease,
    border-color 0.2s ease;
}

.hero__tool-btn :deep(.anticon) {
  font-size: 14px;
}

.hero__tool-btn:not(:disabled):hover {
  color: rgba(0, 0, 0, 0.88) !important;
  background: #eee !important;
  border-color: #d4d4d8 !important;
}

.hero__tool-btn:disabled,
.hero__tool-btn.ant-btn-disabled {
  color: rgba(0, 0, 0, 0.25) !important;
  pointer-events: none;
  background: #fafafa !important;
  border-color: #f0f0f0 !important;
}

.hero__tool-btn:focus-visible,
.hero__send:focus-visible,
.app-card__more-btn:focus-visible {
  outline: 2px solid #1677ff;
  outline-offset: 2px;
}

.hero__send {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  color: #fff;
  background: linear-gradient(135deg, #22d3ee 0%, #6366f1 100%);
  border-color: transparent;
  box-shadow:
    0 6px 20px rgba(99, 102, 241, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
  transition:
    background 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.hero__send:hover {
  background: linear-gradient(135deg, #38bdf8 0%, #818cf8 100%) !important;
  box-shadow:
    0 8px 26px rgba(99, 102, 241, 0.7),
    inset 0 1px 0 rgba(255, 255, 255, 0.3) !important;
  transform: translateY(-1px);
}

.hero__send:active {
  transform: translateY(0);
}

.hero__send :deep(.anticon) {
  font-size: 18px;
}

.hero__send:disabled,
.hero__send.ant-btn-disabled {
  color: rgba(0, 0, 0, 0.25) !important;
  pointer-events: none;
  background: #f0f0f0 !important;
  border-color: transparent !important;
  box-shadow: none !important;
  transform: none;
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

.section {
  position: relative;
  z-index: 1;
  max-width: 1240px;
  margin: 24px auto 0;
  padding: 24px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 18px;
}

.section__header {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section__title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.section__search {
  max-width: 260px;
}

.section__pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.app-card {
  overflow: hidden;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  transition:
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.app-card:hover {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-4px);
}

.app-card__cover {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 160px;
  overflow: hidden;
  background-size: cover;
  background-position: center;
}

/* 悬停图片时显示遮罩与操作按钮 */
.app-card__cover::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.32);
  opacity: 0;
  transition: opacity 0.2s ease;
  pointer-events: none;
}

.app-card__cover:hover::after {
  opacity: 1;
}

.app-card__cover-actions {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 2;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  transform: translate(-50%, -50%);
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.2s ease;
}

.app-card__cover:hover .app-card__cover-actions,
.app-card__cover-actions:focus-within {
  opacity: 1;
  visibility: visible;
}

.app-card__cover-btn {
  color: rgba(0, 0, 0, 0.88);
  background: rgba(255, 255, 255, 0.95);
  border-color: transparent;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.app-card__cover-btn:hover,
.app-card__cover-btn:focus {
  color: #1677ff !important;
  background: #fff !important;
}

.app-card__cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-card__body {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 14px 16px 8px;
}

.app-card__more {
  display: inline-flex;
  flex-shrink: 0;
  margin-left: auto;
}

.app-card__more-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  color: rgba(0, 0, 0, 0.45);
  border-radius: 4px;
}

.app-card__more-btn .anticon {
  font-size: 16px;
}

.app-card__more-btn:hover {
  color: #1677ff !important;
  background: rgba(22, 119, 255, 0.08) !important;
}

.app-card__more-menu {
  min-width: 110px;
}

.app-card__more-menu-item--danger {
  color: #ff4d4f;
}

.app-card__more-menu-item--danger:hover {
  background: rgba(255, 77, 79, 0.08) !important;
}

.app-card__more-menu-trigger {
  display: flex;
  gap: 6px;
  align-items: center;
  width: 100%;
}

.app-card__avatar {
  flex-shrink: 0;
}

.app-card__info {
  flex: 1;
  min-width: 0;
}

.app-card__name {
  overflow: hidden;
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-card__meta {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
  margin-top: 4px;
}

.app-card__author-name {
  overflow: hidden;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-card__time {
  flex-shrink: 0;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.app-card__tag {
  flex-shrink: 0;
  margin-right: 0;
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

  .section {
    padding: 16px;
  }
}
</style>
