<script setup>
defineProps({
  username: { type: String, default: '' },
  size: { type: String, default: 'md' }
})

const sizeClasses = {
  sm: 'w-8 h-8 text-xs',
  md: 'w-10 h-10 text-sm',
  lg: 'w-16 h-16 text-xl',
  xl: 'w-24 h-24 text-3xl'
}
</script>

<template>
  <div
    class="rounded-full bg-accent/20 flex items-center justify-center font-bold flex-shrink-0"
    :class="sizeClasses[size] || sizeClasses.md"
  >
    {{ username?.charAt(0)?.toUpperCase() }}
  </div>
</template>
