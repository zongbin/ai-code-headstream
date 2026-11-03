<script setup lang="ts">
import { computed, ref } from 'vue'
import { type MenuProps } from 'ant-design-vue'
import { LogoutOutlined } from '@ant-design/icons-vue'
import { userLogout } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRouter } from 'vue-router'

const loginUserStore = useLoginUserStore()
const router = useRouter()

// 监听路由变化，更新当前选中的菜单
const selectedKeys = ref<String[]>(['/'])
router.afterEach((to, from, next) => {
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
    key: '/about',
    label: '关于',
    title: '关于',
  },
]

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
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
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
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
