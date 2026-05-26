<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useFeedStore } from '@/stores/feed'

const props = defineProps({
  post: { type: Object, required: true }
})

const router = useRouter()
const feedStore = useFeedStore()

const timeAgo = computed(() => {
  const diff = Date.now() - new Date(props.post.publishTime).getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  return `${days}天前`
})

function goToPost() {
  router.push(`/post/${props.post.postId}`)
}

function goToProfile() {
  router.push(`/profile/${props.post.userId}`)
}

async function handleLike() {
  await feedStore.toggleLike(props.post.postId)
}
</script>

<template>
  <article
    class="border-b border-border-custom p-4 hover:bg-bg-hover/50 cursor-pointer transition-colors"
    @click="goToPost"
  >
    <div class="flex gap-3">
      <div
        class="w-10 h-10 rounded-full bg-accent/20 flex-shrink-0 flex items-center justify-center cursor-pointer"
        @click.stop="goToProfile"
      >
        <span class="text-sm font-bold">{{ post.displayName?.charAt(0) }}</span>
      </div>

      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2">
          <span class="font-bold text-text-primary truncate">{{ post.displayName }}</span>
          <span class="text-text-secondary truncate">@{{ post.username }}</span>
          <span class="text-text-secondary">·</span>
          <span class="text-text-secondary text-sm flex-shrink-0">{{ timeAgo }}</span>
        </div>

        <p class="mt-2 text-text-primary whitespace-pre-wrap break-words">{{ post.shortText }}</p>

        <div v-if="post.photos?.length" class="mt-3 grid gap-1 rounded-2xl overflow-hidden" :class="post.photos.length === 1 ? 'grid-cols-1' : 'grid-cols-2'">
          <img
            v-for="(photo, idx) in post.photos.slice(0, 4)"
            :key="idx"
            :src="photo"
            class="w-full h-48 object-cover"
          />
        </div>

        <div class="flex items-center gap-12 mt-3">
          <button class="flex items-center gap-1.5 text-text-secondary hover:text-accent transition-colors group">
            <span class="p-1.5 rounded-full group-hover:bg-accent/10">💬</span>
            <span class="text-sm">{{ post.commentCount }}</span>
          </button>

          <button
            class="flex items-center gap-1.5 transition-colors group"
            :class="post.isLiked ? 'text-danger' : 'text-text-secondary hover:text-danger'"
            @click.stop="handleLike"
          >
            <span class="p-1.5 rounded-full" :class="post.isLiked ? 'bg-danger/10' : 'group-hover:bg-danger/10'">
              {{ post.isLiked ? '❤️' : '🤍' }}
            </span>
            <span class="text-sm">{{ post.likeCount }}</span>
          </button>
        </div>
      </div>
    </div>
  </article>
</template>
