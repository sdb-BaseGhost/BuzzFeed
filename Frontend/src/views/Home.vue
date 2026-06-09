<script setup>
import { ref } from 'vue'
import FeedList from '@/components/feed/FeedList.vue'

const activeTab = ref('recommend')

const tabs = [
  { key: 'recommend', label: '推荐' },
  { key: 'following', label: '关注' },
]
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom">
      <h1 class="text-xl font-bold px-4 pt-3 pb-0">首页</h1>
      <div class="flex">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="flex-1 py-3 text-sm font-medium transition-colors relative hover:bg-bg-hover/50"
          :class="activeTab === tab.key ? 'text-text-primary font-bold' : 'text-text-secondary'"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <div
            v-if="activeTab === tab.key"
            class="absolute bottom-0 left-1/2 -translate-x-1/2 w-14 h-[3px] rounded-full bg-accent"
          ></div>
        </button>
      </div>
    </header>

    <!-- 推荐 tab -->
    <div v-if="activeTab === 'recommend'" class="p-8 text-center text-text-secondary">
      <p class="text-lg">🎉 推荐内容即将上线</p>
      <p class="mt-2 text-sm">敬请期待</p>
    </div>

    <!-- 关注 tab -->
    <FeedList v-if="activeTab === 'following'" />
  </div>
</template>
