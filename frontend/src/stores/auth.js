import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import { TOKEN_KEY } from '@/api/http'

const USER_KEY = 'libraryms_user'

const readUser = () => {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(readUser())
  const loading = ref(false)
  const initialized = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value && user.value))
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const role = computed(() => user.value?.role || '')
  const displayName = computed(() => user.value?.name || '读者')
  const reader = computed(() => user.value?.reader || null)

  const persist = () => {
    if (token.value) localStorage.setItem(TOKEN_KEY, token.value)
    else localStorage.removeItem(TOKEN_KEY)
    if (user.value) localStorage.setItem(USER_KEY, JSON.stringify(user.value))
    else localStorage.removeItem(USER_KEY)
  }

  const applyUser = (payload) => {
    user.value = payload
    persist()
  }

  const login = async (credentials) => {
    loading.value = true
    try {
      const result = await authApi.login(credentials)
      token.value = result.token
      user.value = result.user
      initialized.value = true
      persist()
      return result
    } finally {
      loading.value = false
    }
  }

  const fetchProfile = async () => {
    if (!token.value) return null
    loading.value = true
    try {
      const profile = await authApi.fetchMe()
      applyUser(profile)
      initialized.value = true
      return profile
    } finally {
      loading.value = false
    }
  }

  const logout = async () => {
    try {
      if (token.value) await authApi.logout()
    } catch {
      // 退出登录时即使接口不可用也应清理本地状态。
    } finally {
      token.value = ''
      user.value = null
      initialized.value = false
      persist()
    }
  }

  const changePassword = async (payload) => authApi.changePassword(payload)

  const hasRole = (roles) => {
    if (!roles) return true
    return [].concat(roles).includes(role.value)
  }

  return {
    token,
    user,
    loading,
    initialized,
    isAuthenticated,
    isAdmin,
    role,
    displayName,
    reader,
    login,
    fetchProfile,
    logout,
    changePassword,
    hasRole,
  }
})
