<script setup>
import { onBeforeUnmount, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
import AppSidebar from './components/AppSidebar.vue'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const appStore = useAppStore()
let timer = null

onMounted(() => {
  appStore.fetchNotifications()
  timer = window.setInterval(() => appStore.fetchNotifications(), 120000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})

watch(
  () => route.fullPath,
  () => {
    appStore.closeMobileSidebar()
    appStore.fetchNotifications()
  },
)
</script>

<template>
  <div class="app-shell" :class="{ 'is-collapsed': appStore.sidebarCollapsed }">
    <div
      v-if="appStore.mobileSidebarOpen"
      class="mobile-backdrop"
      @click="appStore.closeMobileSidebar()"
    />
    <AppSidebar />
    <div class="app-main">
      <AppHeader />
      <main class="app-content">
        <router-view />
      </main>
      <footer class="app-footer">
        <span>知墨图书馆 · 高校图书管理系统</span>
        <span class="app-footer__meta">
          <span class="app-footer__dot" />
          演示环境 · Mock API
        </span>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
  background:
    linear-gradient(135deg, rgba(248, 243, 234, 0.82), rgba(245, 239, 229, 0.85)),
    repeating-linear-gradient(
      90deg,
      transparent,
      transparent 79px,
      rgba(135, 115, 88, 0.035) 80px
    );
}

.app-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  margin-left: var(--sidebar-width);
  transition: margin-left 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}

.app-shell.is-collapsed .app-main {
  margin-left: var(--sidebar-collapsed);
}

.app-content {
  flex: 1;
  padding: 28px 34px 36px;
}

.app-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 34px 22px;
  color: var(--text-400);
  font-size: 11px;
  letter-spacing: 0.03em;
}

.app-footer__meta {
  display: flex;
  align-items: center;
  gap: 7px;
}

.app-footer__dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--success);
  box-shadow: 0 0 0 3px rgba(62, 130, 101, 0.12);
}

.mobile-backdrop {
  position: fixed;
  inset: 0;
  z-index: 90;
  background: rgba(12, 39, 40, 0.42);
  backdrop-filter: blur(2px);
}

@media (max-width: 1180px) {
  .app-content {
    padding: 24px 24px 30px;
  }
}

@media (max-width: 960px) {
  .app-main,
  .app-shell.is-collapsed .app-main {
    margin-left: 0;
  }

  .app-content {
    padding: 18px 16px 24px;
  }

  .app-footer {
    padding: 0 16px 18px;
  }

  .app-footer__meta {
    display: none;
  }
}
</style>
