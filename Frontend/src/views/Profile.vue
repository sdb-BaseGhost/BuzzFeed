<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUser } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useFeed } from '@/composables/useFeed'
import UserAvatar from '@/components/user/UserAvatar.vue'
import FollowButton from '@/components/user/FollowButton.vue'
import FeedItem from '@/components/feed/FeedItem.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const user = ref(null)
const loading = ref(true)
const activeTab = ref('posts')
const showLogoutConfirm = ref(false)

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

function confirmLogout() {
  showLogoutConfirm.value = false
  authStore.logout()
  router.push('/login')
}
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

      <!-- 退出登录（仅本人可见） -->
      <div v-if="isSelf" class="border-t border-border-custom p-4 mt-4">
        <button
          class="flex items-center gap-3 px-4 py-3 w-full rounded-lg text-text-secondary hover:text-danger hover:bg-danger/5 transition-colors"
          @click="showLogoutConfirm = true"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9" />
          </svg>
          <span class="text-sm font-medium">退出登录</span>
        </button>
      </div>
    </div>

    <!-- 退出确认弹窗 -->
    <Teleport to="body">
      <div
        v-if="showLogoutConfirm"
        class="fixed inset-0 z-50 flex items-center justify-center"
        @click.self="showLogoutConfirm = false"
      >
        <div class="fixed inset-0 bg-white/5 backdrop-blur-sm"></div>
        <div class="relative bg-bg-secondary border border-border-custom rounded-2xl p-6 w-full max-w-sm mx-4 shadow-xl">
          <h3 class="text-lg font-bold text-text-primary">退出登录</h3>
          <p class="mt-2 text-text-secondary text-sm">确定要退出当前账号吗？退出后需要重新登录。</p>
          <div class="flex gap-3 mt-6 justify-end">
            <button
              class="px-5 py-2 rounded-full border border-border-custom text-text-primary text-sm font-medium hover:bg-bg-hover transition-colors"
              @click="showLogoutConfirm = false"
            >
              取消
            </button>
            <button
              class="px-5 py-2 rounded-full bg-danger text-white text-sm font-medium hover:bg-danger/90 transition-colors"
              @click="confirmLogout"
            >
              退出
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
