import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import UserProfilePage from '@/pages/user/UserProfilePage.vue'
import AccessEnum from '@/access/accessEnum.ts'
import NoAuthPage from '@/pages/NoAuthPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '主页',
      component: HomePage,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: UserManagePage,
      meta: {
        access: AccessEnum.ADMIN,
      },
    },
    {
      path: '/user/profile',
      name: '个人设置',
      component: UserProfilePage,
      meta: {
        access: AccessEnum.USER,
      },
    },
    {
      path: '/noAuth',
      name: '无权限页面',
      component: NoAuthPage
    },
    {
      path: '/about',
      name: '关于',
      component: () => import('@/pages/AboutView.vue'),
    },
  ],
})

export default router
