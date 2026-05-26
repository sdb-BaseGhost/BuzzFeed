<script setup>
import { ref, onMounted } from 'vue'

const conversations = ref([])
const loading = ref(true)

onMounted(() => {
  setTimeout(() => {
    conversations.value = [
      { id: 1, username: 'vuejs', displayName: 'Vue.js', lastMessage: '你好！欢迎使用 Vue 3', time: '10:30', unread: 2 },
      { id: 2, username: 'springboot', displayName: 'Spring Boot', lastMessage: '新的 API 文档已经更新', time: '昨天', unread: 0 },
    ]
    loading.value = false
  }, 500)
})
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom px-4 py-3">
      <h1 class="text-xl font-bold">消息</h1>
    </header>

    <div v-if="loading" class="p-4 text-center text-text-secondary">加载中...</div>

    <div v-else>
      <div
        v-for="conv in conversations"
        :key="conv.id"
        class="flex gap-3 p-4 border-b border-border-custom hover:bg-bg-hover transition-colors cursor-pointer"
      >
        <div class="w-12 h-12 rounded-full bg-accent/20 flex-shrink-0 flex items-center justify-center">
          <span class="font-bold">{{ conv.displayName.charAt(0) }}</span>
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex justify-between items-center">
            <span class="font-bold">{{ conv.displayName }}</span>
            <span class="text-text-secondary text-sm">{{ conv.time }}</span>
          </div>
          <p class="text-text-secondary text-sm truncate">{{ conv.lastMessage }}</p>
        </div>
        <div v-if="conv.unread" class="w-5 h-5 rounded-full bg-accent flex items-center justify-center flex-shrink-0">
          <span class="text-white text-xs">{{ conv.unread }}</span>
        </div>
      </div>

      <div v-if="conversations.length === 0" class="p-8 text-center text-text-secondary">
        暂无消息
      </div>
    </div>
  </div>
</template>
