import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getFeed } from '@/api/feed'
import { useAuthStore } from '@/stores/auth'

const INITIAL_SIZE = 15  // 首次加载量，足够填满大屏
const PAGE_SIZE = 5       // 后续每次滚动加载量

export const useFeedStore = defineStore('feed', () => {
  const posts = ref([])
  const lastTime = ref(null)       // 双游标：最后一条的 publishTime
  const lastContentId = ref(null)  // 双游标：最后一条的 itemId
  const loading = ref(false)
  const hasMore = ref(true)

  /**
   * 获取当前用户ID
   */
  function getUserId() {
    const authStore = useAuthStore()
    return authStore.currentUser?.userId
  }

  /**
   * 更新游标为列表最后一条数据
   */
  function updateCursor() {
    if (posts.value.length > 0) {
      const last = posts.value[posts.value.length - 1]
      lastTime.value = last.publishTime
      lastContentId.value = last.itemId
    }
  }

  /**
   * 首次加载 / 下拉刷新
   */
  async function fetchFeed() {
    const userId = getUserId()
    if (!userId) return

    loading.value = true
    try {
      const res = await getFeed({
        userId,
        type: 0,
        lastTime: null,
        contentId: null,
        num: INITIAL_SIZE
      })
      posts.value = res.data || []
      hasMore.value = posts.value.length >= INITIAL_SIZE
      updateCursor()
    } finally {
      loading.value = false
    }
  }

  /**
   * 上滑加载更多
   */
  async function loadMore() {
    if (loading.value || !hasMore.value) return

    const userId = getUserId()
    if (!userId) return

    loading.value = true
    try {
      const res = await getFeed({
        userId,
        type: 1,
        lastTime: lastTime.value,
        contentId: lastContentId.value,
        num: PAGE_SIZE
      })
      const newPosts = res.data || []
      posts.value.push(...newPosts)
      hasMore.value = newPosts.length >= PAGE_SIZE
      updateCursor()
    } finally {
      loading.value = false
    }
  }

  function addPost(post) {
    posts.value.unshift(post)
  }

  return { posts, loading, hasMore, fetchFeed, loadMore, addPost }
})
