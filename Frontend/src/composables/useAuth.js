import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ref } from 'vue'

export function useAuth() {
  const authStore = useAuthStore()
  const router = useRouter()
  const loading = ref(false)
  const error = ref('')

  async function login(username, password) {
    error.value = ''
    loading.value = true
    try {
      await authStore.login(username, password)
      router.push('/')
    } catch (e) {
      error.value = e.message || '登录失败'
    } finally {
      loading.value = false
    }
  }

  async function register(username, password, email, displayName) {
    error.value = ''
    loading.value = true
    try {
      await authStore.register(username, password, email, displayName)
      router.push('/login')
    } catch (e) {
      error.value = e.message || '注册失败'
    } finally {
      loading.value = false
    }
  }

  function logout() {
    authStore.logout()
    router.push('/login')
  }

  return { loading, error, login, register, logout }
}
