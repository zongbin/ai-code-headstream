import type { Component } from 'vue'

/**
 * 全局导航菜单配置
 * 通过该文件即可扩展/调整顶部导航栏的菜单项，无需修改组件本身。
 */
export interface MenuItemConfig {
  /** 唯一标识，同时作为路由路径（子菜单时为 key） */
  key: string
  /** 显示的标题 */
  title: string
  /** 路由路径，点击时跳转 */
  path?: string
  /** 图标，传入图标组件（如 @ant-design/icons-vue 中的组件） */
  icon?: Component
  /** 子菜单 */
  children?: MenuItemConfig[]
}

export const menuItems: MenuItemConfig[] = [
  {
    key: '/',
    title: '首页',
    path: '/',
  },
  {
    key: '/about',
    title: '关于',
    path: '/about',
  },
]
