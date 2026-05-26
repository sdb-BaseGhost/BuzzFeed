import { ref } from 'vue'
import { createPost } from '@/api/post'
import { useFeedStore } from '@/stores/feed'

export function usePost() {
  const feedStore = useFeedStore()
  const loading = ref(false)
  const error = ref('')

  async function publishPost(data) {
    error.value = ''
    loading.value = true
    try {
      const res = await createPost(data)
      feedStore.addPost(res.data)
      return res.data
    } catch (e) {
      error.value = e.message || '发布失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  return { loading, error, publishPost }
}
