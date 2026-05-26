<script setup>
defineProps({
  comment: { type: Object, required: true }
})

function timeAgo(dateStr) {
  const diff = Date.now() - new Date(dateStr).getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  return `${Math.floor(hours / 24)}天前`
}
</script>

<template>
  <div class="flex gap-3 p-4 border-b border-border-custom">
    <div class="w-8 h-8 rounded-full bg-accent/20 flex-shrink-0 flex items-center justify-center">
      <span class="text-xs font-bold">{{ comment.displayName?.charAt(0) }}</span>
    </div>
    <div class="flex-1">
      <div class="flex items-center gap-2">
        <span class="font-bold text-sm">{{ comment.displayName }}</span>
        <span class="text-text-secondary text-sm">@{{ comment.username }}</span>
        <span class="text-text-secondary text-sm">· {{ timeAgo(comment.publishTime) }}</span>
      </div>
      <p class="mt-1 text-text-primary">{{ comment.content }}</p>
    </div>
  </div>
</template>
