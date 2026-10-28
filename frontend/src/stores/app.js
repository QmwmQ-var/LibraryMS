import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getNotifications } from '@/api/stats'

const COLLAPSE_KEY = 'libraryms_sidebar_collapsed'
const READ_KEY = 'libraryms_read_notifications'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(localStorage.getItem(COLLAPSE_KEY) === 'true')
  const mobileSidebarOpen = ref(false)
  const notifications = ref([])
  const notificationsLoading = ref(false)
  const readIds = ref(JSON.parse(localStorage.getItem(READ_KEY) || '[]'))

  const unreadCount = computed(
    () => notifications.value.filter((item) => !readIds.value.includes(item.id)).length,
  )

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem(COLLAPSE_KEY, String(sidebarCollapsed.value))
  }

  const closeMobileSidebar = () => {
    mobileSidebarOpen.value = false
  }

  const fetchNotifications = async () => {
    notificationsLoading.value = true
    try {
      const data = await getNotifications()
      notifications.value = data.items || []
    } catch (error) {
      console.warn('[LibraryMS] 通知加载失败', error)
    } finally {
      notificationsLoading.value = false
    }
  }

  const markAllRead = () => {
    readIds.value = notifications.value.map((item) => item.id)
    localStorage.setItem(READ_KEY, JSON.stringify(readIds.value))
  }

  const markRead = (id) => {
    if (!readIds.value.includes(id)) {
      readIds.value = [...readIds.value, id]
      localStorage.setItem(READ_KEY, JSON.stringify(readIds.value))
    }
  }

  return {
    sidebarCollapsed,
    mobileSidebarOpen,
    notifications,
    notificationsLoading,
    unreadCount,
    toggleSidebar,
    closeMobileSidebar,
    fetchNotifications,
    markAllRead,
    markRead,
  }
})
