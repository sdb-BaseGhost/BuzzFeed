<script setup>
import { ref } from 'vue'
import { search } from '@/api/search'
import FeedItem from '@/components/feed/FeedItem.vue'
import UserAvatar from '@/components/user/UserAvatar.vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const query = ref('')
const results = ref(null)
const loading = ref(false)

async function handleSearch() {
  if (!query.value.trim()) return
  loading.value = true
  try {
    const res = await search(query.value)
    results.value = res.data
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom px-4 py-3">
      <div class="relative">
        <span class="absolute left-4 top-1/2 -translate-y-1/2 text-text-secondary">🔍</span>
        <input
          v-model="query"
          type="text"
          placeholder="搜索用户或帖子"
          class="w-full bg-bg-secondary border border-transparent focus:border-accent rounded-full py-3 pl-12 pr-4 text-text-primary placeholder-text-secondary outline-none transition-colors"
          @keyup.enter="handleSearch"
        />
      </div>
    </header>

    <div v-if="loading" class="p-4 text-center text-text-secondary">搜索中...</div>

    <div v-else-if="results">
      <div v-if="results.users?.length" class="border-b border-border-custom">
        <h3 class="px-4 py-3 font-bold text-lg">用户</h3>
        <div
          v-for="user in results.users"
          :key="user.userId"
          class="flex items-center gap-3 px-4 py-3 hover:bg-bg-hover cursor-pointer transition-colors"
          @click="router.push(`/profile/${user.userId}`)"
        >
          <UserAvatar :username="user.username" />
          <div>
            <div class="font-bold">{{ user.displayName }}</div>
            <div class="text-text-secondary">@{{ user.username }}</div>
          </div>
        </div>
      </div>

      <div v-if="results.posts?.length">
        <h3 class="px-4 py-3 font-bold text-lg">帖子</h3>
        <FeedItem v-for="post in results.posts" :key="post.postId" :post="post" />
      </div>

      <div v-if="!results.users?.length && !results.posts?.length" class="p-8 text-center text-text-secondary">
        没有找到相关结果
      </div>
    </div>

    <div v-else class="p-8 text-center text-text-secondary">
      搜索你感兴趣的内容
    </div>
  </div>
</template>
