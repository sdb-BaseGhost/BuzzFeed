<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useFeedStore } from '@/stores/feed'
import { useAuthStore } from '@/stores/auth'
import FeedItem from './FeedItem.vue'

const feedStore = useFeedStore()
const authStore = useAuthStore()

const posts = computed(() => feedStore.posts)
const loading = computed(() => feedStore.loading)
const hasMore = computed(() => feedStore.hasMore)

/** 底部哨兵元素 */
const sentinelRef = ref(null)
let observer = null

// currentUser 加载完成后自动拉取 Feed（解决页面刷新时序问题）
watch(
  () => authStore.currentUser,
  (user) => {
    if (user) {
      feedStore.fetchFeed()
    }
  },
  { immediate: true }
)

/**
 * 逐级向上查找最近的 overflow-y-auto 滚动祖先
 */
function findScrollRoot(el) {
  let node = el?.parentElement
  while (node) {
    const style = window.getComputedStyle(node)
    if (style.overflowY === 'auto' || style.overflowY === 'scroll') {
      return node
    }
    node = node.parentElement
  }
  return null
}

/**
 * IntersectionObserver：当底部哨兵元素进入滚动容器可视区域时自动加载更多
 */
async function setupObserver() {
  await nextTick()
  if (!sentinelRef.value) return

  const scrollRoot = findScrollRoot(sentinelRef.value)

  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting && hasMore.value && !loading.value) {
        feedStore.loadMore()
      }
    },
    { root: scrollRoot, rootMargin: '200px' }
  )
  observer.observe(sentinelRef.value)
}

onMounted(() => {
  setupObserver()
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
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

    <!-- 底部哨兵：IntersectionObserver 监控此元素，进入视口即触发 loadMore -->
    <div ref="sentinelRef" class="h-1" />

    <div v-if="!hasMore && posts.length > 0" class="p-4 text-center text-text-secondary">
      没有更多了
    </div>

    <div v-if="loading && posts.length > 0" class="p-4 text-center text-text-secondary">
      加载中...
    </div>
  </div>
</template>
