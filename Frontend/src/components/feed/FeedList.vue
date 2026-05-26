<script setup>
import { onMounted, computed, ref } from 'vue'
import { useFeed } from '@/composables/useFeed'
import FeedItem from './FeedItem.vue'

const props = defineProps({
  type: { type: String, default: 'recommend' }
})

const { posts, followingPosts, loading, hasMore, loadFeed, loadMore } = useFeed()
const sentinel = ref(null)

const currentPosts = computed(() => props.type === 'recommend' ? posts : followingPosts)

onMounted(() => {
  loadFeed(props.type)
})

onMounted(() => {
  const observer = new IntersectionObserver(entries => {
    if (entries[0].isIntersecting && hasMore.value && !loading.value) {
      loadMore(props.type)
    }
  }, { threshold: 0.1 })

  if (sentinel.value) {
    observer.observe(sentinel.value)
  }
})
</script>

<template>
  <div>
    <div v-if="loading && currentPosts.length === 0" class="p-4 space-y-4">
      <div v-for="i in 3" :key="i" class="animate-pulse flex gap-3">
        <div class="w-10 h-10 rounded-full bg-bg-secondary"></div>
        <div class="flex-1 space-y-2">
          <div class="h-4 bg-bg-secondary rounded w-1/3"></div>
          <div class="h-4 bg-bg-secondary rounded w-full"></div>
          <div class="h-4 bg-bg-secondary rounded w-2/3"></div>
        </div>
      </div>
    </div>

    <FeedItem v-for="post in currentPosts" :key="post.postId" :post="post" />

    <div ref="sentinel" class="h-10"></div>

    <div v-if="!hasMore && currentPosts.length > 0" class="p-4 text-center text-text-secondary">
      没有更多了
    </div>

    <div v-if="loading && currentPosts.length > 0" class="p-4 text-center text-text-secondary">
      加载中...
    </div>
  </div>
</template>
