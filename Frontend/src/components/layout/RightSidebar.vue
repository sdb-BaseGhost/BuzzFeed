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
  <aside class="py-4 pr-6">
    <div class="sticky top-0 pt-0 pb-3 bg-bg-primary z-10">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="搜索"
        class="w-full bg-transparent border border-border-custom rounded-lg py-2.5 px-4 text-sm text-text-primary placeholder-text-secondary outline-none focus:border-text-secondary transition-colors"
      />
    </div>

    <div class="mt-4">
      <h2 class="text-base font-semibold mb-2 px-1">热门话题</h2>
      <div
        v-for="topic in trendingTopics"
        :key="topic.id"
        class="py-2.5 px-1 cursor-pointer hover:text-text-primary transition-colors border-b border-border-custom/50 last:border-0"
      >
        <div class="text-text-secondary text-xs">{{ topic.category }} · 热门</div>
        <div class="text-sm font-medium mt-0.5 text-text-primary">{{ topic.title }}</div>
        <div class="text-text-secondary text-xs mt-0.5">{{ topic.posts }} 条帖子</div>
      </div>
    </div>

    <div class="mt-5">
      <h2 class="text-base font-semibold mb-2 px-1">推荐关注</h2>
      <div
        v-for="user in suggestedUsers"
        :key="user.id"
        class="flex items-center justify-between py-2.5 px-1 border-b border-border-custom/50 last:border-0"
      >
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-full bg-border-custom flex items-center justify-center text-text-secondary text-xs font-medium">
            {{ user.displayName.charAt(0) }}
          </div>
          <div>
            <div class="text-sm font-medium text-text-primary">{{ user.displayName }}</div>
            <div class="text-text-secondary text-xs">@{{ user.username }}</div>
          </div>
        </div>
        <button class="border border-border-custom text-text-primary px-3 py-1 rounded text-xs font-medium hover:border-text-secondary transition-colors">
          关注
        </button>
      </div>
    </div>
  </aside>
</template>
