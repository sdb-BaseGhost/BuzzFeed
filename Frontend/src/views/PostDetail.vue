<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPostDetail, toggleLike } from '@/api/post'
import CommentList from '@/components/comment/CommentList.vue'

const route = useRoute()
const router = useRouter()
const post = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getPostDetail(route.params.postId)
    post.value = res.data
  } finally {
    loading.value = false
  }
})

async function handleLike() {
  const res = await toggleLike(post.value.postId)
  post.value.isLiked = res.data.isLiked
  post.value.likeCount += res.data.isLiked ? 1 : -1
}

function goToProfile() {
  router.push(`/profile/${post.value.userId}`)
}
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom flex items-center gap-6 px-4 py-3">
      <button class="text-xl" @click="router.back()">←</button>
      <h1 class="text-xl font-bold">帖子</h1>
    </header>

    <div v-if="loading" class="p-8 text-center text-text-secondary">加载中...</div>

    <div v-else-if="post">
      <div class="p-4">
        <div class="flex items-center gap-3 mb-4">
          <div
            class="w-12 h-12 rounded-full bg-accent/20 flex items-center justify-center cursor-pointer"
            @click="goToProfile"
          >
            <span class="font-bold">{{ post.displayName?.charAt(0) }}</span>
          </div>
          <div>
            <div class="font-bold">{{ post.displayName }}</div>
            <div class="text-text-secondary">@{{ post.username }}</div>
          </div>
        </div>

        <p class="text-xl whitespace-pre-wrap break-words">{{ post.shortText }}</p>
        <p v-if="post.longText" class="mt-4 text-text-primary whitespace-pre-wrap break-words">{{ post.longText }}</p>

        <div v-if="post.photos?.length" class="mt-4 grid gap-1 rounded-2xl overflow-hidden" :class="post.photos.length === 1 ? 'grid-cols-1' : 'grid-cols-2'">
          <img v-for="(photo, idx) in post.photos" :key="idx" :src="photo" class="w-full h-64 object-cover" />
        </div>

        <div class="flex items-center gap-6 mt-4 py-3 border-t border-b border-border-custom text-text-secondary text-sm">
          <span>{{ post.likeCount }} 赞</span>
          <span>{{ post.commentCount }} 评论</span>
        </div>

        <div class="flex items-center gap-12 py-3">
          <button
            class="flex items-center gap-2 transition-colors"
            :class="post.isLiked ? 'text-danger' : 'text-text-secondary hover:text-danger'"
            @click="handleLike"
          >
            {{ post.isLiked ? '❤️' : '🤍' }}
            <span>{{ post.isLiked ? '已赞' : '赞' }}</span>
          </button>
        </div>
      </div>

      <CommentList :post-id="post.postId" />
    </div>
  </div>
</template>
