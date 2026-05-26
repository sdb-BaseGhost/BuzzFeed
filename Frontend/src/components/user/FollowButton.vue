<script setup>
import { ref } from 'vue'
import { followUser, unfollowUser } from '@/api/user'

const props = defineProps({
  userId: { type: String, required: true },
  initialFollowing: { type: Boolean, default: false }
})

const emit = defineEmits(['toggle'])

const isFollowing = ref(props.initialFollowing)
const loading = ref(false)

async function handleToggle() {
  loading.value = true
  try {
    if (isFollowing.value) {
      await unfollowUser(props.userId)
      isFollowing.value = false
    } else {
      await followUser(props.userId)
      isFollowing.value = true
    }
    emit('toggle', isFollowing.value)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <button
    :disabled="loading"
    class="px-5 py-1.5 rounded-full font-bold text-sm transition-colors"
    :class="isFollowing
      ? 'border border-border-custom text-text-primary hover:border-danger hover:text-danger hover:bg-danger/10'
      : 'bg-text-primary text-bg-primary hover:bg-text-primary/90'"
    @click="handleToggle"
  >
    {{ loading ? '...' : isFollowing ? '正在关注' : '关注' }}
  </button>
</template>
