import { useFeedStore } from '@/stores/feed'
import { ref } from 'vue'

export function useFeed() {
  const feedStore = useFeedStore()
  const loading = ref(false)

  async function loadFeed(type = 'recommend') {
    loading.value = true
    try {
      await feedStore.fetchFeed(type)
    } finally {
      loading.value = false
    }
  }

  async function loadMore(type = 'recommend') {
    await feedStore.loadMore(type)
  }

  return { ...feedStore, loading, loadFeed, loadMore }
}
