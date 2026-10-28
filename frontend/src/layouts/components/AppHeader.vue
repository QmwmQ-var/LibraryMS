<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { flatMenuItems } from '@/router/menu'
import { ROLE_LABELS } from '@/utils/constants'
import { initials } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const auth = useAuthStore()
const searchKeyword = ref('')

const menuTitle = computed(
  () => flatMenuItems.find((item) => item.name === route.name)?.label || route.meta.title || '',
)

const roleLabel = computed(() => ROLE_LABELS[auth.role] || '访客')

const onSearch = () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return
  router.push({ name: auth.isAdmin ? 'books-manage' : 'books', query: { keyword } })
}

const openNotification = (item) => {
  appStore.markRead(item.id)
  if (item.path) router.push(item.path)
}

const onUserCommand = async (command) => {
  if (command === 'profile') {
    router.push({ name: 'profile' })
    return
  }
  if (command === 'logout') {
    await auth.logout()
    ElMessage.success('已安全退出')
    router.replace({ name: 'login' })
  }
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__left">
      <button class="header-icon mobile-menu" @click="appStore.mobileSidebarOpen = true">
        <el-icon :size="18"><Menu /></el-icon>
      </button>
      <button class="header-icon desktop-menu" @click="appStore.toggleSidebar()">
        <el-icon :size="17">
          <Expand v-if="appStore.sidebarCollapsed" />
          <Fold v-else />
        </el-icon>
      </button>

      <div class="breadcrumb">
        <span class="breadcrumb__root" @click="router.push({ name: 'dashboard' })">知墨</span>
        <el-icon class="breadcrumb__sep"><ArrowRight /></el-icon>
        <span class="breadcrumb__current">{{ menuTitle }}</span>
      </div>
    </div>

    <div class="app-header__right">
      <el-input
        v-model="searchKeyword"
        class="header-search"
        placeholder="搜索书名、作者或 ISBN"
        clearable
        @keyup.enter="onSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <div class="header-divider" />

      <el-popover
        placement="bottom-end"
        :width="372"
        trigger="click"
        popper-class="notification-popper"
        @show="appStore.fetchNotifications()"
      >
        <template #reference>
          <button class="header-icon bell" aria-label="消息通知">
            <el-icon :size="18"><Bell /></el-icon>
            <span v-if="appStore.unreadCount" class="bell__dot" />
          </button>
        </template>

        <div class="notice-panel">
          <div class="notice-panel__head">
            <div>
              <strong>消息提醒</strong>
              <span>{{ appStore.notifications.length }} 条动态</span>
            </div>
            <button class="text-button" @click="appStore.markAllRead()">全部已读</button>
          </div>

          <div v-if="appStore.notificationsLoading" class="notice-panel__loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            正在获取提醒…
          </div>

          <div v-else class="notice-list">
            <button
              v-for="item in appStore.notifications"
              :key="item.id"
              class="notice-item"
              :class="{ 'is-unread': !appStore.unreadCount || !auth.initialized }"
              @click="openNotification(item)"
            >
              <span class="notice-item__icon" :class="`is-${item.tone}`">
                <el-icon>
                  <WarningFilled v-if="item.tone === 'danger'" />
                  <Clock v-else-if="item.tone === 'warning'" />
                  <CircleCheck v-else-if="item.tone === 'success'" />
                  <InfoFilled v-else />
                </el-icon>
              </span>
              <span class="notice-item__copy">
                <strong>{{ item.title }}</strong>
                <span>{{ item.description }}</span>
              </span>
              <el-icon class="notice-item__arrow"><ArrowRight /></el-icon>
            </button>
          </div>
        </div>
      </el-popover>

      <el-dropdown trigger="click" @command="onUserCommand">
        <button class="user-menu">
          <span class="user-menu__avatar" :style="{ background: auth.user?.avatarColor }">
            {{ initials(auth.displayName) }}
          </span>
          <span class="user-menu__copy">
            <strong>{{ auth.displayName }}</strong>
            <small>{{ roleLabel }}</small>
          </span>
          <el-icon class="user-menu__arrow"><ArrowDown /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              个人中心
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--header-height);
  padding: 0 34px;
  border-bottom: 1px solid rgba(226, 215, 200, 0.64);
  background: rgba(248, 243, 234, 0.82);
  backdrop-filter: blur(16px);
}

.app-header__left,
.app-header__right {
  display: flex;
  align-items: center;
}

.app-header__left {
  gap: 14px;
}

.app-header__right {
  gap: 15px;
}

.header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid transparent;
  border-radius: 50%;
  color: var(--text-500);
  background: transparent;
  cursor: pointer;
  transition: all 0.18s ease;
}

.header-icon:hover {
  color: var(--ink-700);
  border-color: var(--line);
  background: var(--paper-50);
}

.mobile-menu {
  display: none;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
}

.breadcrumb__root {
  color: var(--text-400);
  cursor: pointer;
  transition: color 0.18s ease;
}

.breadcrumb__root:hover {
  color: var(--copper-600);
}

.breadcrumb__sep {
  color: var(--paper-300);
  font-size: 10px;
}

.breadcrumb__current {
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.header-search {
  width: 238px;
}

.header-search :deep(.el-input__wrapper) {
  height: 36px;
  border-radius: 99px;
  background: rgba(253, 251, 247, 0.76);
  box-shadow: 0 0 0 1px rgba(221, 209, 193, 0.75) inset;
}

.header-search :deep(.el-input__inner) {
  font-size: 12px;
}

.header-search :deep(.el-input__prefix) {
  color: var(--text-400);
}

.header-divider {
  width: 1px;
  height: 22px;
  background: var(--line);
}

.bell {
  position: relative;
}

.bell__dot {
  position: absolute;
  top: 7px;
  right: 8px;
  width: 7px;
  height: 7px;
  border: 2px solid #f8f3ea;
  border-radius: 50%;
  background: var(--danger);
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 3px 4px 3px 3px;
  border: 0;
  border-radius: 22px;
  background: transparent;
  cursor: pointer;
}

.user-menu__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: #fff8ee;
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.7);
}

.user-menu__copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.35;
}

.user-menu__copy strong {
  color: var(--text-700);
  font-size: 12px;
  font-weight: 650;
}

.user-menu__copy small {
  color: var(--text-400);
  font-size: 9px;
}

.user-menu__arrow {
  margin-left: 2px;
  color: var(--text-400);
  font-size: 10px;
}

.notice-panel {
  padding: 4px 2px;
}

.notice-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 5px 5px 13px;
  border-bottom: 1px solid var(--line);
}

.notice-panel__head > div {
  display: flex;
  flex-direction: column;
}

.notice-panel__head strong {
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 16px;
}

.notice-panel__head span {
  margin-top: 2px;
  color: var(--text-400);
  font-size: 10px;
}

.text-button {
  padding: 3px 0;
  border: 0;
  color: var(--copper-600);
  background: transparent;
  font-size: 10.5px;
  cursor: pointer;
}

.text-button:hover {
  color: var(--copper-500);
}

.notice-panel__loading {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 26px 8px;
  color: var(--text-400);
  font-size: 12px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding-top: 8px;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  padding: 10px 8px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.18s ease;
}

.notice-item:hover {
  background: var(--paper-100);
}

.notice-item__icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 14px;
}

.notice-item__icon.is-danger {
  color: var(--danger);
  background: var(--danger-bg);
}

.notice-item__icon.is-warning {
  color: var(--warning);
  background: var(--warning-bg);
}

.notice-item__icon.is-info {
  color: var(--info);
  background: var(--info-bg);
}

.notice-item__icon.is-success {
  color: var(--success);
  background: var(--success-bg);
}

.notice-item__copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.notice-item__copy strong {
  color: var(--text-700);
  font-size: 11.5px;
  font-weight: 650;
}

.notice-item__copy span {
  margin-top: 2px;
  color: var(--text-400);
  font-size: 10px;
  line-height: 1.55;
}

.notice-item__arrow {
  margin-top: 5px;
  color: var(--line-strong);
  font-size: 10px;
}

@media (max-width: 1180px) {
  .app-header {
    padding: 0 24px;
  }

  .header-search {
    width: 200px;
  }
}

@media (max-width: 960px) {
  .app-header {
    padding: 0 16px;
    height: 66px;
  }

  .desktop-menu {
    display: none;
  }

  .mobile-menu {
    display: flex;
  }

  .header-search,
  .header-divider {
    display: none;
  }

  .user-menu__copy,
  .user-menu__arrow {
    display: none;
  }

  .app-header__right {
    gap: 7px;
  }
}
</style>

<style>
.notification-popper.el-popover {
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--paper-50);
  box-shadow: var(--shadow-lg);
}
</style>
