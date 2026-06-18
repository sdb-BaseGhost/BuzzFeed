import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, register as apiRegister, getCurrentUser } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const currentUser = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  async function login(username, password) {
    const res = await apiLogin({ username, password })
    token.value = res.data.token
    localStorage.setItem('token', res.data.token)
    await fetchCurrentUser()
  }

  async function register(username, password, email, displayName) {
    await apiRegister({ username, password, email, displayName })
  }

  async function fetchCurrentUser() {
    if (!token.value) {
      console.log('[Auth] fetchCurrentUser: no token, skip')
      return
    }
    try {
      console.log('[Auth] fetchCurrentUser: token exists, calling /api/auth/me ...')
      const res = await getCurrentUser()
      console.log('[Auth] fetchCurrentUser: success', res.data)
      currentUser.value = res.data
    } catch (e) {
      console.error('[Auth] fetchCurrentUser: failed, will logout', e)
      logout()
    }
  }

  function logout() {
    token.value = null
    currentUser.value = null
    localStorage.removeItem('token')
  }

  // 页面刷新后，如果有 token 则自动恢复用户信息
  if (token.value) {
    fetchCurrentUser()
  }

  return { token, currentUser, isLoggedIn, login, register, fetchCurrentUser, logout }
})
