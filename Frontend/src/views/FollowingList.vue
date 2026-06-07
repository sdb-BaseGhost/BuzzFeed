<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getFollowing } from '@/api/user'
import UserAvatar from '@/components/user/UserAvatar.vue'

const route = useRoute()
const users = ref([])
const loading = ref(true)

async function loadFollowing() {
  loading.value = true
  try {
    const res = await getFollowing(route.params.userId)
    users.value = res.data || []
  } catch {
    users.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadFollowing)
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom flex items-center gap-4 px-4 py-3">
      <button class="text-xl" @click="$router.back()">&larr;</button>
      <h1 class="text-xl font-bold">关注</h1>
    </header>

    <div v-if="loading" class="p-8 text-center text-text-secondary">
      <div class="inline-block w-6 h-6 border-2 border-border-custom border-t-accent rounded-full animate-spin"></div>
    </div>

    <div v-else-if="users.length === 0" class="p-8 text-center text-text-secondary">
      暂无关注
    </div>

    <div v-else>
      <router-link
        v-for="user in users"
        :key="user.userId"
        :to="`/profile/${user.userId}`"
        class="flex items-center gap-3 px-4 py-3 hover:bg-bg-hover transition-colors border-b border-border-custom"
      >
        <UserAvatar :username="user.username" size="md" />
        <div class="min-w-0 flex-1">
          <div class="text-sm font-bold text-text-primary truncate">{{ user.displayName }}</div>
          <div class="text-sm text-text-secondary truncate">@{{ user.username }}</div>
          <div v-if="user.bio" class="text-sm text-text-secondary truncate mt-0.5">{{ user.bio }}</div>
        </div>
        <span class="text-xs text-text-secondary flex-shrink-0">{{ user.followerNumber ?? 0 }} 粉丝</span>
      </router-link>
    </div>
  </div>
</template>
