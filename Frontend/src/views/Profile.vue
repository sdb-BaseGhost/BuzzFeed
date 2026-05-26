<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getUser } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useFeed } from '@/composables/useFeed'
import UserAvatar from '@/components/user/UserAvatar.vue'
import FollowButton from '@/components/user/FollowButton.vue'
import FeedItem from '@/components/feed/FeedItem.vue'

const route = useRoute()
const authStore = useAuthStore()
const user = ref(null)
const loading = ref(true)
const activeTab = ref('posts')

const { posts, loadFeed } = useFeed()

const isSelf = computed(() => authStore.currentUser?.userId === route.params.userId)

onMounted(async () => {
  try {
    const res = await getUser(route.params.userId)
    user.value = res.data
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom flex items-center gap-6 px-4 py-3">
      <button class="text-xl" @click="$router.back()">&larr;</button>
      <div>
        <h1 class="text-xl font-bold">{{ user?.displayName }}</h1>
        <span class="text-text-secondary text-sm">{{ user?.postCount }} 条帖子</span>
      </div>
    </header>

    <div v-if="loading" class="p-8 text-center text-text-secondary">加载中...</div>

    <div v-else-if="user">
      <div class="h-48 bg-bg-secondary"></div>

      <div class="px-4 pb-4">
        <div class="flex justify-between items-end -mt-12 mb-3">
          <UserAvatar :username="user.username" size="xl" />
          <FollowButton
            v-if="!isSelf"
            :user-id="user.userId"
            :initial-following="false"
          />
        </div>

        <h2 class="text-xl font-bold">{{ user.displayName }}</h2>
        <p class="text-text-secondary">@{{ user.username }}</p>
        <p v-if="user.bio" class="mt-2 text-text-primary">{{ user.bio }}</p>

        <div class="flex gap-4 mt-3 text-sm">
          <span><strong>{{ user.followingCount }}</strong> <span class="text-text-secondary">关注</span></span>
          <span><strong>{{ user.followerCount }}</strong> <span class="text-text-secondary">粉丝</span></span>
        </div>
      </div>

      <div class="flex border-b border-border-custom">
        <button
          class="flex-1 py-3 text-center font-bold relative"
          :class="activeTab === 'posts' ? 'text-text-primary' : 'text-text-secondary'"
          @click="activeTab = 'posts'"
        >
          帖子
          <div v-if="activeTab === 'posts'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-14 h-1 bg-accent rounded-full"></div>
        </button>
        <button
          class="flex-1 py-3 text-center font-bold relative"
          :class="activeTab === 'media' ? 'text-text-primary' : 'text-text-secondary'"
          @click="activeTab = 'media'"
        >
          媒体
          <div v-if="activeTab === 'media'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-14 h-1 bg-accent rounded-full"></div>
        </button>
        <button
          class="flex-1 py-3 text-center font-bold relative"
          :class="activeTab === 'likes' ? 'text-text-primary' : 'text-text-secondary'"
          @click="activeTab = 'likes'"
        >
          喜欢
          <div v-if="activeTab === 'likes'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-14 h-1 bg-accent rounded-full"></div>
        </button>
      </div>

      <FeedItem v-for="post in posts" :key="post.postId" :post="post" />
    </div>
  </div>
</template>
