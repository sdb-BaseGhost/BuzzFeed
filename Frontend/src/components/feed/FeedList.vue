<script setup>
import { onMounted, onUnmounted, watch } from 'vue'
import { useFeed } from '@/composables/useFeed'
import { useAuthStore } from '@/stores/auth'
import FeedItem from './FeedItem.vue'

const { posts, loading, hasMore, fetchFeed, loadMore } = useFeed()
const authStore = useAuthStore()

// currentUser 加载完成后自动拉取 Feed（解决页面刷新时序问题）
watch(
  () => authStore.currentUser,
  (user) => {
    if (user) {
      fetchFeed()
    }
  },
  { immediate: true }
)

// 滚动到底部自动加载更多（监听 main 滚动容器）
function onScroll() {
  const container = document.getElementById('main-scroll')
  if (!container) return

  const { scrollHeight, scrollTop, clientHeight } = container

  // 距离底部 200px 时触发预加载
  if (scrollHeight - scrollTop - clientHeight < 200) {
    if (!loading.value && hasMore.value) {
      loadMore()
    }
  }
}

onMounted(() => {
  const container = document.getElementById('main-scroll')
  if (container) {
    container.addEventListener('scroll', onScroll, { passive: true })
  }
})

onUnmounted(() => {
  const container = document.getElementById('main-scroll')
  if (container) {
    container.removeEventListener('scroll', onScroll)
  }
})
</script>

<template>
  <div>
    <div v-if="loading && posts.length === 0" class="p-4 space-y-4">
      <div v-for="i in 3" :key="i" class="animate-pulse flex gap-3">
        <div class="w-10 h-10 rounded-full bg-bg-secondary"></div>
        <div class="flex-1 space-y-2">
          <div class="h-4 bg-bg-secondary rounded w-1/3"></div>
          <div class="h-4 bg-bg-secondary rounded w-full"></div>
          <div class="h-4 bg-bg-secondary rounded w-2/3"></div>
        </div>
      </div>
    </div>

    <FeedItem v-for="post in posts" :key="post.itemId" :post="post" />

    <div v-if="!hasMore && posts.length > 0" class="p-4 text-center text-text-secondary">
      没有更多了
    </div>

    <div v-if="loading && posts.length > 0" class="p-4 text-center text-text-secondary">
      加载中...
    </div>
  </div>
</template>
