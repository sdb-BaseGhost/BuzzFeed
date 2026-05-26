<script setup>
import { ref } from 'vue'
import FeedList from '@/components/feed/FeedList.vue'
import PostComposer from '@/components/post/PostComposer.vue'

const activeTab = ref('recommend')
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom">
      <div class="px-4 py-3">
        <h1 class="text-xl font-bold">首页</h1>
      </div>
      <div class="flex">
        <button
          class="flex-1 py-3 text-center font-bold transition-colors relative"
          :class="activeTab === 'recommend' ? 'text-text-primary' : 'text-text-secondary hover:text-text-primary hover:bg-bg-hover'"
          @click="activeTab = 'recommend'"
        >
          推荐
          <div v-if="activeTab === 'recommend'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-14 h-1 bg-accent rounded-full"></div>
        </button>
        <button
          class="flex-1 py-3 text-center font-bold transition-colors relative"
          :class="activeTab === 'following' ? 'text-text-primary' : 'text-text-secondary hover:text-text-primary hover:bg-bg-hover'"
          @click="activeTab = 'following'"
        >
          关注
          <div v-if="activeTab === 'following'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-14 h-1 bg-accent rounded-full"></div>
        </button>
      </div>
    </header>

    <PostComposer />
    <FeedList :type="activeTab" />
  </div>
</template>
