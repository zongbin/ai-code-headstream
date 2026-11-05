<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { type MenuProps } from 'ant-design-vue'
import { LogoutOutlined, SettingOutlined } from '@ant-design/icons-vue'
import { userLogout } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRouter } from 'vue-router'
import router from '@/router/index.ts'
import checkAccess from '@/access/checkAccess.ts'

const loginUserStore = useLoginUserStore()
const routerUse = useRouter()

// 监听路由变化，更新当前选中的菜单
const selectedKeys = ref<String[]>(['/'])
routerUse.afterEach((to, from, next) => {
  {
    selectedKeys.value = [to.path]
  }
})

// 菜单配置项
const originItems = [
  {
    key: '/',
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: '/about',
    label: '关于',
    title: '关于',
  },
]
type MenuItem = NonNullable<MenuProps['items']>[number]

function menuToRouteItem(menu: MenuItem) {
  // 排除分割线、分组等没有 key 的项
  if (!menu || !('key' in menu) || menu.key == null) {
    return { path: '', meta: {} as Record<string, any> }
  }
  const path = String(menu.key)
  const route = router.getRoutes().find((r) => r.path === path)

  if (!route) {
    console.warn(`[menu] 菜单项 ${path} 未匹配到路由`)
    return { path, meta: {} as Record<string, any> }
  }
  return route
}

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const item = menuToRouteItem(menu)
    // if (item.meta?.hideInMenu) {
    //   return false
    // }
    // 根据权限过滤菜单，有权限则返回 true，则保留该菜单
    return checkAccess(loginUserStore.loginUser, item.meta?.access as string)
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 菜单项的 key 即路由路径，点击后跳转
  router.push(key)
}

// 个人设置
const goToProfile = () => {
  routerUse.push('/user/profile')
}

// 用户头像地址，没有头像（或加载失败）时展示橙色默认头像
const avatarFailed = ref(false)

const userAvatar = computed(() => (loginUserStore.loginUser.userAvatar ?? '').trim())

// 是否展示真实头像图片
const showAvatarImage = computed(() => !!userAvatar.value && !avatarFailed.value)

const avatarSrc = computed(() => (showAvatarImage.value ? userAvatar.value : undefined))

// 有图片时用默认底色，没有图片时用橙色底色
const avatarStyle = computed(() =>
  showAvatarImage.value
    ? { backgroundColor: '#f0f0f0' }
    : { backgroundColor: '#fa8c16', color: '#fff' },
)

// 默认头像上展示的用户名首字母
const avatarInitial = computed(() => (loginUserStore.loginUser.userName ?? 'U').slice(0, 1))

// 头像地址变化时重置加载状态
watch(
  () => loginUserStore.loginUser.userAvatar,
  () => {
    avatarFailed.value = false
  },
)

// 头像加载失败时回退到默认头像
const handleAvatarLoadError = () => {
  avatarFailed.value = true
}

// 用户注销
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<template>
  <a-layout-header class="global-header">
    <a-row :wrap="false">
      <!-- 左侧：Logo + 站点标题 -->
      <a-col flex="300px">
        <RouterLink to="/">
          <div class="global-header__brand">
            <img class="global-header__logo" src="@/assets/logo.png" alt="logo" />
            <span class="global-header__title">AI 零代码应用生成平台</span>
          </div>
        </RouterLink>
      </a-col>

      <!-- 中间：菜单 -->
      <a-col flex="auto">
        <a-menu
          mode="horizontal"
          :items="menuItems"
          :selected-keys="selectedKeys"
          @click="handleMenuClick"
        />
      </a-col>
      <a-col>
        <div class="global-header__extra">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar
                  :src="avatarSrc"
                  :style="avatarStyle"
                  :load-error="handleAvatarLoadError"
                >
                  {{ avatarInitial }}
                </a-avatar>
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="goToProfile">
                    <SettingOutlined />
                    个人设置
                  </a-menu-item>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<style scoped>
.global-header {
  padding: 0 24px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.global-header__brand {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 10px;
}

.global-header__logo {
  height: 32px;
  width: 32px;
  object-fit: contain;
}

.global-header__title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.global-header__extra {
  margin-left: auto;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
</style>
