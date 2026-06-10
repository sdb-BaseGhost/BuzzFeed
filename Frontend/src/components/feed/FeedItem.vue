<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  post: { type: Object, required: true }
})

const router = useRouter()

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
  router.push(`/post/${props.post.itemId}`)
}

function goToProfile() {
  router.push(`/profile/${props.post.creatorId}`)
}
</script>

<template>
  <article
    class="border-b border-border-custom p-4 hover:bg-bg-hover/50 cursor-pointer transition-colors"
    @click="goToPost"
  >
    <div class="flex gap-3">
      <!-- 头像 -->
      <div
        class="w-10 h-10 rounded-full bg-accent/20 flex-shrink-0 flex items-center justify-center cursor-pointer overflow-hidden"
        @click.stop="goToProfile"
      >
        <img v-if="post.avatar" :src="post.avatar" class="w-full h-full object-cover" />
        <span v-else class="text-sm font-bold">{{ post.displayName?.charAt(0) || post.username?.charAt(0) }}</span>
      </div>

      <div class="flex-1 min-w-0">
        <!-- 用户名 + 昵称 + 时间 -->
        <div class="flex items-center gap-2">
          <span class="font-bold text-text-primary truncate">{{ post.displayName || post.username }}</span>
          <span class="text-text-secondary truncate">@{{ post.username }}</span>
          <span class="text-text-secondary">·</span>
          <span class="text-text-secondary text-sm flex-shrink-0">{{ timeAgo }}</span>
        </div>

        <!-- 标题 -->
        <h3 v-if="post.title" class="mt-2 text-text-primary font-semibold break-words">{{ post.title }}</h3>

        <!-- 正文 -->
        <p v-if="post.summary" class="mt-1 text-text-primary whitespace-pre-wrap break-words">{{ post.summary }}</p>
      </div>
    </div>
  </article>
</template>
