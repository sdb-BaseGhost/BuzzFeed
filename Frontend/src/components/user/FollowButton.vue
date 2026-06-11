<script setup>
import { ref, onMounted } from 'vue'
import { followUser, unfollowUser, getFollowStatus } from '@/api/follow'

const props = defineProps({
  userId: { type: [String, Number], required: true },
  initialFollowing: { type: Boolean, default: false }
})

const emit = defineEmits(['toggle'])

// FOLLOWING / NOT_FOLLOWING / MUTUAL_FOLLOW
const status = ref(props.initialFollowing ? 'FOLLOWING' : 'NOT_FOLLOWING')
const loading = ref(false)

onMounted(async () => {
  try {
    const res = await getFollowStatus(props.userId)
    status.value = res.data?.status || 'NOT_FOLLOWING'
  } catch {
    // ignore, keep default
  }
})

const isFollowing = () => status.value === 'FOLLOWING' || status.value === 'MUTUAL_FOLLOW'

async function handleToggle() {
  loading.value = true
  try {
    if (isFollowing()) {
      await unfollowUser(props.userId)
      status.value = 'NOT_FOLLOWING'
    } else {
      await followUser(props.userId)
      // 重新查一下真实状态（可能是 MUTUAL_FOLLOW）
      try {
        const res = await getFollowStatus(props.userId)
        status.value = res.data?.status || 'FOLLOWING'
      } catch {
        status.value = 'FOLLOWING'
      }
    }
    emit('toggle', isFollowing())
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <button
    :disabled="loading"
    class="px-5 py-1.5 rounded-full font-bold text-sm transition-colors"
    :class="isFollowing()
      ? 'border border-border-custom text-text-primary hover:border-danger hover:text-danger hover:bg-danger/10'
      : 'bg-text-primary text-bg-primary hover:bg-text-primary/90'"
    @click="handleToggle"
  >
    {{ loading ? '...' : isFollowing() ? (status === 'MUTUAL_FOLLOW' ? '互相关注' : '正在关注') : '关注' }}
  </button>
</template>
