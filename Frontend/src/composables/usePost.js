import { ref } from 'vue'
import { createPost } from '@/api/post'

export function usePost() {
  const loading = ref(false)
  const error = ref('')

  async function publishPost(data) {
    error.value = ''
    loading.value = true
    try {
      const res = await createPost(data)
      return res.data  // 返回 contentId
    } catch (e) {
      error.value = e.message || '发布失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  return { loading, error, publishPost }
}
