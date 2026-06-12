<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUser } from '@/api/user'
import { getFollowStatus } from '@/api/follow'
import { getUserPosts } from '@/api/post'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/user/UserAvatar.vue'
import FollowButton from '@/components/user/FollowButton.vue'
import FeedItem from '@/components/feed/FeedItem.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const user = ref(null)
const followStatus = ref('NOT_FOLLOWING')
const loading = ref(true)
const loadError = ref(false)
const activeTab = ref('posts')
const showLogoutConfirm = ref(false)
const posts = ref([])

const isSelf = computed(() => authStore.currentUser?.userId == route.params.userId)

async function loadUser() {
  loading.value = true
  loadError.value = false
  try {
    const res = await getUser(route.params.userId)
    user.value = res.data
    // 加载该用户的帖子
    const postsRes = await getUserPosts(route.params.userId)
    posts.value = postsRes.data || []
    // 加载关注状态
    if (!isSelf.value) {
      try {
        const statusRes = await getFollowStatus(route.params.userId)
        followStatus.value = statusRes.data?.status || 'NOT_FOLLOWING'
      } catch {
        followStatus.value = 'NOT_FOLLOWING'
      }
    }
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(loadUser)

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

    <div v-else-if="loadError" class="p-8 text-center text-text-secondary">
      <p>加载用户信息失败，请稍后重试</p>
      <button class="mt-4 px-4 py-2 border border-border-custom rounded-full text-sm hover:bg-bg-hover transition-colors" @click="loadUser">
        重新加载
      </button>
    </div>

    <div v-else-if="user">
      <!-- 封面图 -->
      <div class="h-48 bg-bg-secondary"></div>

      <!-- 头像区域 -->
      <div class="px-4 pb-4">
        <div class="flex justify-between items-end -mt-14 mb-4">
          <!-- 圆形头像 + 白色边框 -->
          <div class="w-28 h-28 rounded-full bg-bg-primary p-[4px] ring-4 ring-bg-primary shadow-lg">
            <div
              class="w-full h-full rounded-full bg-accent/20 flex items-center justify-center text-4xl font-bold text-accent"
            >
              {{ user.username?.charAt(0)?.toUpperCase() }}
            </div>
          </div>
          <FollowButton
            v-if="!isSelf"
            :user-id="user.userId"
            :initial-following="followStatus === 'FOLLOWING' || followStatus === 'MUTUAL_FOLLOW'"
          />
        </div>

        <h2 class="text-xl font-bold">{{ user.displayName }}</h2>
        <p class="text-text-secondary text-sm">@{{ user.username }}</p>
        <p v-if="user.bio" class="mt-3 text-text-primary text-sm leading-relaxed">{{ user.bio }}</p>

        <div class="flex gap-5 mt-3 text-sm">
          <router-link :to="`/profile/${user.userId}/following`" class="cursor-pointer hover:underline">
            <strong class="text-text-primary">{{ user.followsNumber ?? 0 }}</strong>
            <span class="text-text-secondary">关注</span>
          </router-link>
          <router-link :to="`/profile/${user.userId}/followers`" class="cursor-pointer hover:underline">
            <strong class="text-text-primary">{{ user.followerNumber ?? 0 }}</strong>
            <span class="text-text-secondary">粉丝</span>
          </router-link>
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
      <div v-if="isSelf" class="border-t border-border-custom px-4 py-3 mt-6 mb-8">
        <button
          class="flex items-center gap-3 px-4 py-3 w-full rounded-xl text-text-secondary hover:text-danger hover:bg-danger/10 transition-colors"
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
