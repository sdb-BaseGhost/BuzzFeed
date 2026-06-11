<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getFollowerList } from '@/api/follow'
import UserAvatar from '@/components/user/UserAvatar.vue'
import FollowButton from '@/components/user/FollowButton.vue'

const route = useRoute()
const users = ref([])
const loading = ref(true)

async function loadFollowers() {
  loading.value = true
  try {
    const res = await getFollowerList(route.params.userId)
    users.value = res.data?.list || []
  } catch {
    users.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadFollowers)
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom flex items-center gap-4 px-4 py-3">
      <button class="text-xl" @click="$router.back()">&larr;</button>
      <h1 class="text-xl font-bold">粉丝</h1>
    </header>

    <div v-if="loading" class="p-8 text-center text-text-secondary">
      <div class="inline-block w-6 h-6 border-2 border-border-custom border-t-accent rounded-full animate-spin"></div>
    </div>

    <div v-else-if="users.length === 0" class="p-8 text-center text-text-secondary">
      暂无粉丝
    </div>

    <div v-else>
      <div
        v-for="user in users"
        :key="user.userId"
        class="flex items-center gap-3 px-4 py-3 hover:bg-bg-hover transition-colors border-b border-border-custom"
      >
        <router-link :to="`/profile/${user.userId}`" class="contents">
          <UserAvatar :username="user.username" size="md" />
        </router-link>
        <div class="min-w-0 flex-1">
          <router-link :to="`/profile/${user.userId}`" class="contents">
            <div class="text-sm font-bold text-text-primary truncate">{{ user.nickname || user.displayName }}</div>
            <div class="text-sm text-text-secondary truncate">@{{ user.username }}</div>
          </router-link>
        </div>
        <FollowButton
          :user-id="user.userId"
          :initial-following="user.followStatus === 'FOLLOWING' || user.followStatus === 'MUTUAL_FOLLOW'"
        />
      </div>
    </div>
  </div>
</template>
