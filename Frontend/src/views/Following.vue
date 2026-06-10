<script setup>
import FeedList from '@/components/feed/FeedList.vue'
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom">
      <div class="px-4 py-3">
        <h1 class="text-xl font-bold">关注</h1>
      </div>
    </header>
    <FeedList />
  </div>
</template>
