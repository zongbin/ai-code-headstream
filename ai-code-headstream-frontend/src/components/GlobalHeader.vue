<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MenuOutlined, UserOutlined } from '@ant-design/icons-vue'
import { Grid, type ItemType } from 'ant-design-vue'
import type { MenuItemType, SubMenuType } from 'ant-design-vue/es/menu/src/hooks/useItems'
import { menuItems, type MenuItemConfig } from '../layouts/menu.config.ts'
import { siteConfig } from '../layouts/site.config.ts'

/** 站点标题 */
const siteTitle = siteConfig.title

/** Logo 图片地址 */
const logoSrc = siteConfig.logo

const route = useRoute()
const router = useRouter()

/** 当前登录用户信息（暂时用 null 表示未登录，后续接入真实用户信息） */
const user = ref<{ name: string; avatar?: string } | null>(null)

const screens = Grid.useBreakpoint()
const isMobile = computed(() => screens.value.md === false)

/** 移动端抽屉菜单展开状态 */
const drawerOpen = ref(false)

/** 依据当前路由高亮菜单项 */
const selectedKeys = computed(() => {
  const path = route.path
  const matched = menuItems.filter((item) => item.path).find((item) => item.path === path)
  return matched ? [matched.key] : []
})

/** 转换为 ant-design-vue Menu 所需的 items 结构 */
const flatItems = computed<ItemType[]>(() => menuItems.map(toMenuItem))

function toMenuItem(item: MenuItemConfig): ItemType {
  if (item.children?.length) {
    const subMenu: SubMenuType = {
      key: item.key,
      label: item.title,
      children: item.children.map(toMenuItem),
    }
    if (item.icon) {
      subMenu.icon = h(item.icon)
    }
    return subMenu
  }

  const menuItem: MenuItemType = {
    key: item.key,
    label: item.title,
  }
  if (item.icon) {
    menuItem.icon = h(item.icon)
  }
  return menuItem
}

function handleMenuClick(info: { key: string | number }) {
  const key = String(info.key)
  const item = findItem(menuItems, key)
  if (item?.path) {
    router.push(item.path)
  }
  drawerOpen.value = false
}

function findItem(items: MenuItemConfig[], key: string): MenuItemConfig | undefined {
  for (const item of items) {
    if (item.key === key) return item
    if (item.children) {
      const found = findItem(item.children, key)
      if (found) return found
    }
  }
  return undefined
}
</script>

<template>
  <a-layout-header class="global-header">
    <div class="global-header__inner">
      <!-- 左侧：Logo + 站点标题 -->
      <div class="global-header__brand">
        <img class="global-header__logo" :src="logoSrc" :alt="siteTitle" />
        <span class="global-header__title">{{ siteTitle }}</span>
      </div>

      <!-- 中间：菜单（桌面端） -->
      <nav v-if="!isMobile" class="global-header__nav">
        <a-menu
          mode="horizontal"
          :items="flatItems"
          :selected-keys="selectedKeys"
          @click="handleMenuClick"
        />
      </nav>

      <div class="global-header__extra">
        <!-- 移动端菜单按钮 -->
        <a-button
          v-if="isMobile"
          type="text"
          class="global-header__menu-btn"
          @click="drawerOpen = true"
        >
          <template #icon><MenuOutlined /></template>
        </a-button>

        <!-- 右侧：登录用户信息 / 登录按钮 -->
        <div v-if="user" class="global-header__user">
          <a-avatar :src="user.avatar" :size="28">
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <span class="global-header__nickname">{{ user.name }}</span>
        </div>
        <a-button v-else type="primary" size="small">登录</a-button>
      </div>
    </div>

    <!-- 移动端：抽屉式菜单 -->
    <a-drawer v-model:open="drawerOpen" placement="left" :width="240" :closable="true">
      <template #title>{{ siteTitle }}</template>
      <a-menu
        mode="inline"
        :items="flatItems"
        :selected-keys="selectedKeys"
        @click="handleMenuClick"
      />
    </a-drawer>
  </a-layout-header>
</template>

<style scoped>
.global-header {
  padding: 0;
  height: 64px;
  line-height: normal;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.global-header__inner {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 24px;
  gap: 16px;
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

.global-header__nav {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.global-header__extra {
  margin-left: auto;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.global-header__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.global-header__nickname {
  color: rgba(0, 0, 0, 0.88);
  font-size: 14px;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .global-header__inner {
    padding: 0 12px;
    gap: 8px;
  }

  .global-header__title {
    font-size: 16px;
  }
}
</style>
