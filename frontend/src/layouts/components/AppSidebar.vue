<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { menuGroups } from '@/router/menu'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { ROLE_LABELS } from '@/utils/constants'
import { initials } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const auth = useAuthStore()

const groups = computed(() =>
  menuGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => auth.hasRole(item.roles)),
    }))
    .filter((group) => group.items.length),
)

const isActive = (item) => route.name === item.name

const navigate = (item) => {
  if (!isActive(item)) router.push({ name: item.name })
  appStore.closeMobileSidebar()
}

const onLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '继续阅读',
      type: 'warning',
      customClass: 'soft-message-box',
    })
    await auth.logout()
    router.replace({ name: 'login' })
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <aside
    class="sidebar"
    :class="{
      'is-collapsed': appStore.sidebarCollapsed,
      'is-open': appStore.mobileSidebarOpen,
    }"
  >
    <div class="brand" @click="router.push({ name: 'dashboard' })">
      <div class="brand__seal">
        <span>知</span>
        <i class="brand__seal-line" />
      </div>
      <div v-if="!appStore.sidebarCollapsed" class="brand__copy">
        <strong>知墨图书馆</strong>
        <span>ZHIMO LIBRARY</span>
      </div>
    </div>

    <div v-if="!appStore.sidebarCollapsed" class="sidebar__mode">
      <span class="sidebar__mode-dot" />
      {{ auth.isAdmin ? '馆员工作台' : '读者服务中心' }}
      <span class="sidebar__mode-role">{{ auth.isAdmin ? 'ADMIN' : 'READER' }}</span>
    </div>

    <nav class="nav">
      <div v-for="group in groups" :key="group.title" class="nav-group">
        <div v-if="!appStore.sidebarCollapsed" class="nav-group__title">{{ group.title }}</div>
        <div v-else class="nav-group__divider" />
        <el-tooltip
          v-for="item in group.items"
          :key="item.name"
          :content="item.label"
          placement="right"
          :disabled="!appStore.sidebarCollapsed"
        >
          <button
            class="nav-item"
            :class="{ 'is-active': isActive(item) }"
            @click="navigate(item)"
          >
            <span class="nav-item__icon">
              <el-icon :size="18"><component :is="item.icon" /></el-icon>
            </span>
            <span v-if="!appStore.sidebarCollapsed" class="nav-item__label">
              {{ item.label }}
            </span>
            <span
              v-if="!appStore.sidebarCollapsed && item.name === 'penalties' && auth.reader?.unpaidAmount"
              class="nav-item__badge"
            >
              !
            </span>
          </button>
        </el-tooltip>
      </div>
    </nav>

    <div class="sidebar__footer">
      <div v-if="!appStore.sidebarCollapsed" class="sidebar__quote">
        <span class="sidebar__quote-mark">“</span>
        <p>吹灭读书灯，一身都是月。</p>
      </div>

      <div class="sidebar__account">
        <div class="avatar" :style="{ background: auth.user?.avatarColor }">
          {{ initials(auth.displayName) }}
        </div>
        <div v-if="!appStore.sidebarCollapsed" class="sidebar__account-copy">
          <strong>{{ auth.displayName }}</strong>
          <span>{{ ROLE_LABELS[auth.role] }} · {{ auth.user?.username }}</span>
        </div>
        <el-tooltip content="退出登录" placement="top">
          <button v-if="!appStore.sidebarCollapsed" class="logout" @click="onLogout">
            <el-icon><SwitchButton /></el-icon>
          </button>
        </el-tooltip>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  width: var(--sidebar-width);
  padding: 24px 16px 18px;
  overflow: hidden;
  color: rgba(244, 234, 219, 0.86);
  background:
    radial-gradient(circle at 12% 8%, rgba(201, 146, 100, 0.16), transparent 28%),
    radial-gradient(circle at 88% 88%, rgba(88, 143, 127, 0.15), transparent 34%),
    linear-gradient(168deg, #143b38 0%, #0d2b2c 60%, #0b2628 100%);
  box-shadow: 12px 0 40px rgba(19, 42, 37, 0.12);
  transition:
    width 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.28s ease;
}

.sidebar::after {
  position: absolute;
  top: 0;
  right: 0;
  width: 1px;
  height: 100%;
  background: linear-gradient(transparent, rgba(234, 211, 189, 0.2), transparent);
  content: '';
}

.sidebar.is-collapsed {
  width: var(--sidebar-collapsed);
  padding-right: 13px;
  padding-left: 13px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 2px 6px 22px;
  cursor: pointer;
}

.brand__seal {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border: 1px solid rgba(229, 193, 156, 0.5);
  border-radius: 11px;
  color: #f4e6d4;
  background:
    linear-gradient(145deg, rgba(201, 146, 100, 0.28), rgba(201, 146, 100, 0.04)),
    rgba(255, 255, 255, 0.03);
  font-family: var(--font-display);
  font-size: 21px;
  font-weight: 700;
  position: relative;
}

.brand__seal-line {
  position: absolute;
  right: 7px;
  bottom: 8px;
  width: 12px;
  height: 1px;
  background: rgba(229, 193, 156, 0.65);
}

.brand__copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.2;
}

.brand__copy strong {
  color: #f6ecdf;
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.brand__copy span {
  margin-top: 4px;
  color: rgba(193, 212, 203, 0.55);
  font-size: 8px;
  font-weight: 600;
  letter-spacing: 0.22em;
}

.sidebar__mode {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 6px 18px;
  padding: 8px 10px;
  border: 1px solid rgba(214, 230, 220, 0.1);
  border-radius: 8px;
  color: rgba(234, 242, 235, 0.74);
  background: rgba(255, 255, 255, 0.035);
  font-size: 11px;
  white-space: nowrap;
}

.sidebar__mode-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #d8a16f;
  box-shadow: 0 0 0 3px rgba(216, 161, 111, 0.12);
}

.sidebar__mode-role {
  margin-left: auto;
  color: rgba(216, 161, 111, 0.75);
  font-family: var(--font-mono);
  font-size: 8px;
  letter-spacing: 0.08em;
}

.nav {
  flex: 1;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.nav::-webkit-scrollbar {
  width: 3px;
}

.nav-group {
  margin-bottom: 18px;
}

.nav-group__title {
  padding: 0 12px 7px;
  color: rgba(176, 199, 191, 0.4);
  font-size: 9px;
  font-weight: 650;
  letter-spacing: 0.18em;
}

.nav-group__divider {
  width: 22px;
  height: 1px;
  margin: 4px auto 10px;
  background: rgba(199, 219, 210, 0.15);
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 3px;
  border: 0;
  border-radius: 9px;
  color: rgba(231, 239, 233, 0.67);
  background: transparent;
  font-size: 13px;
  text-align: left;
  white-space: nowrap;
  cursor: pointer;
  transition:
    color 0.18s ease,
    background-color 0.18s ease;
}

.is-collapsed .nav-item {
  justify-content: center;
  padding: 11px 0;
}

.nav-item:hover {
  color: #f8efe3;
  background: rgba(255, 255, 255, 0.055);
}

.nav-item.is-active {
  color: #fcf4e8;
  background: linear-gradient(90deg, rgba(196, 143, 100, 0.22), rgba(117, 159, 144, 0.1));
}

.nav-item.is-active::before {
  position: absolute;
  top: 9px;
  bottom: 9px;
  left: 0;
  width: 2px;
  border-radius: 2px;
  background: #d8a16f;
  content: '';
}

.nav-item__icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 20px;
}

.nav-item.is-active .nav-item__icon {
  color: #e2b183;
}

.nav-item__label {
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-item__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-left: auto;
  border-radius: 50%;
  color: #fdf7ec;
  background: #b55b52;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
}

.sidebar__footer {
  padding-top: 12px;
  border-top: 1px solid rgba(211, 227, 219, 0.1);
}

.sidebar__quote {
  padding: 12px 12px 14px;
  color: rgba(231, 239, 233, 0.48);
  font-family: var(--font-display);
  font-size: 11px;
  line-height: 1.7;
}

.sidebar__quote-mark {
  color: #d8a16f;
  font-size: 22px;
  line-height: 0;
  vertical-align: -5px;
}

.sidebar__account {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.045);
}

.is-collapsed .sidebar__account {
  justify-content: center;
  padding: 8px 0;
  background: transparent;
}

.avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(250, 239, 225, 0.25);
  border-radius: 50%;
  color: #fff8ed;
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 700;
}

.sidebar__account-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  line-height: 1.35;
}

.sidebar__account-copy strong {
  overflow: hidden;
  color: #f3e8d8;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar__account-copy span {
  overflow: hidden;
  margin-top: 2px;
  color: rgba(192, 211, 202, 0.48);
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 0;
  border-radius: 6px;
  color: rgba(220, 234, 225, 0.48);
  background: transparent;
  cursor: pointer;
  transition: all 0.18s ease;
}

.logout:hover {
  color: #f3e8d8;
  background: rgba(255, 255, 255, 0.08);
}

@media (max-width: 960px) {
  .sidebar {
    width: var(--sidebar-width) !important;
    padding: 24px 16px 18px;
    transform: translateX(-102%);
  }

  .sidebar.is-open {
    transform: translateX(0);
    box-shadow: 20px 0 60px rgba(0, 0, 0, 0.24);
  }

  .sidebar .brand__copy,
  .sidebar .sidebar__mode,
  .sidebar .nav-group__title,
  .sidebar .nav-item__label,
  .sidebar .nav-item__badge,
  .sidebar .sidebar__quote,
  .sidebar .sidebar__account-copy,
  .sidebar .logout {
    display: flex !important;
  }

  .sidebar .sidebar__mode {
    display: flex !important;
  }

  .sidebar .nav-group__divider {
    display: none;
  }

  .sidebar .nav-item {
    justify-content: flex-start;
    padding: 10px 12px;
  }

  .sidebar .sidebar__account {
    justify-content: flex-start;
    padding: 9px 8px;
    background: rgba(255, 255, 255, 0.045);
  }
}
</style>
