<script setup>
import { ref, onMounted } from 'vue'
import { getComments, addComment } from '@/api/post'
import CommentItem from './CommentItem.vue'

const props = defineProps({
  postId: { type: String, required: true }
})

const comments = ref([])
const newComment = ref('')
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getComments(props.postId)
    comments.value = res.data
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  if (!newComment.value.trim()) return
  const res = await addComment(props.postId, newComment.value)
  comments.value.push(res.data)
  newComment.value = ''
}
</script>

<template>
  <div>
    <div class="p-4 border-b border-border-custom">
      <div class="flex gap-3">
        <input
          v-model="newComment"
          placeholder="发表评论..."
          class="flex-1 bg-bg-secondary border border-border-custom rounded-full px-4 py-2 text-text-primary placeholder-text-secondary outline-none focus:border-accent transition-colors"
          @keyup.enter="handleSubmit"
        />
        <button
          :disabled="!newComment.trim()"
          class="bg-accent text-white px-4 py-2 rounded-full font-bold hover:bg-accent/90 transition-colors disabled:opacity-50"
          @click="handleSubmit"
        >
          评论
        </button>
      </div>
    </div>

    <div v-if="loading" class="p-4 text-center text-text-secondary">加载中...</div>
    <CommentItem v-for="comment in comments" :key="comment.commentId" :comment="comment" />
    <div v-if="!loading && comments.length === 0" class="p-8 text-center text-text-secondary">暂无评论</div>
  </div>
</template>
