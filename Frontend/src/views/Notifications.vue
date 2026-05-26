<script setup>
import { ref, onMounted } from 'vue'

const notifications = ref([])
const loading = ref(true)

onMounted(() => {
  setTimeout(() => {
    notifications.value = [
      { id: 1, type: 'like', username: 'vuejs', displayName: 'Vue.js', content: '赞了你的帖子', time: '2小时前', read: false },
      { id: 2, type: 'comment', username: 'springboot', displayName: 'Spring Boot', content: '评论了你的帖子：好文章！', time: '3小时前', read: true },
      { id: 3, type: 'follow', username: 'tailwindcss', displayName: 'Tailwind CSS', content: '关注了你', time: '1天前', read: true },
    ]
    loading.value = false
  }, 500)
})
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom px-4 py-3">
      <h1 class="text-xl font-bold">通知</h1>
    </header>

    <div v-if="loading" class="p-4 text-center text-text-secondary">加载中...</div>

    <div v-else>
      <div
        v-for="notif in notifications"
        :key="notif.id"
        class="flex gap-3 p-4 border-b border-border-custom hover:bg-bg-hover transition-colors cursor-pointer"
        :class="{ 'bg-accent/5': !notif.read }"
      >
        <div class="w-10 h-10 rounded-full bg-accent/20 flex-shrink-0 flex items-center justify-center">
          <span class="text-sm">{{ notif.type === 'like' ? '❤️' : notif.type === 'comment' ? '💬' : '👤' }}</span>
        </div>
        <div class="flex-1">
          <p>
            <strong>{{ notif.displayName }}</strong>
            <span class="text-text-secondary ml-1">{{ notif.content }}</span>
          </p>
          <span class="text-text-secondary text-sm">{{ notif.time }}</span>
        </div>
      </div>

      <div v-if="notifications.length === 0" class="p-8 text-center text-text-secondary">
        暂无通知
      </div>
    </div>
  </div>
</template>
