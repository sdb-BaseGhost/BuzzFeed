<script setup>
import { ref } from 'vue'

const searchQuery = ref('')
const trendingTopics = ref([
  { id: 1, category: '科技', title: 'Vue 3', posts: '12.5K' },
  { id: 2, category: '编程', title: 'JavaScript', posts: '8.2K' },
  { id: 3, category: '开发', title: 'Spring Boot', posts: '5.1K' },
])

const suggestedUsers = ref([
  { id: 1, username: 'vuejs', displayName: 'Vue.js', avatar: null },
  { id: 2, username: 'springboot', displayName: 'Spring Boot', avatar: null },
  { id: 3, username: 'tailwindcss', displayName: 'Tailwind CSS', avatar: null },
])
</script>

<template>
  <aside class="py-3 pr-6">
    <div class="sticky top-0 pt-1 pb-3 bg-bg-primary z-10">
      <div class="relative">
        <span class="absolute left-4 top-1/2 -translate-y-1/2 text-text-secondary">🔍</span>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索"
          class="w-full bg-bg-secondary border border-transparent focus:border-accent rounded-full py-3 pl-12 pr-4 text-text-primary placeholder-text-secondary outline-none transition-colors"
        />
      </div>
    </div>

    <div class="bg-bg-secondary rounded-2xl mt-3 p-4">
      <h2 class="text-xl font-bold mb-3">热门话题</h2>
      <div
        v-for="topic in trendingTopics"
        :key="topic.id"
        class="py-3 hover:bg-bg-hover rounded-lg px-2 cursor-pointer transition-colors"
      >
        <div class="text-text-secondary text-sm">{{ topic.category }} · 热门</div>
        <div class="font-bold mt-0.5">{{ topic.title }}</div>
        <div class="text-text-secondary text-sm mt-0.5">{{ topic.posts }} 条帖子</div>
      </div>
    </div>

    <div class="bg-bg-secondary rounded-2xl mt-4 p-4">
      <h2 class="text-xl font-bold mb-3">推荐关注</h2>
      <div
        v-for="user in suggestedUsers"
        :key="user.id"
        class="flex items-center justify-between py-3"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-accent/20 flex items-center justify-center">
            <span class="text-sm font-bold">{{ user.displayName.charAt(0) }}</span>
          </div>
          <div>
            <div class="font-bold text-sm">{{ user.displayName }}</div>
            <div class="text-text-secondary text-sm">@{{ user.username }}</div>
          </div>
        </div>
        <button class="bg-text-primary text-bg-primary px-4 py-1.5 rounded-full text-sm font-bold hover:bg-text-primary/90 transition-colors">
          关注
        </button>
      </div>
    </div>
  </aside>
</template>
