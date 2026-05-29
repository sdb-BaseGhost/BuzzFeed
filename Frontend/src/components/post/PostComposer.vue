<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usePost } from '@/composables/usePost'

const authStore = useAuthStore()
const { loading, publishPost } = usePost()

const content = ref('')
const isExpanded = ref(false)
const maxLength = 280

const charCount = computed(() => content.value.length)
const isOverLimit = computed(() => charCount.value > maxLength)
const canSubmit = computed(() => content.value.trim().length > 0 && !isOverLimit.value && !loading.value)

async function handleSubmit() {
  if (!canSubmit.value) return
  await publishPost({ creatorId: authStore.currentUser?.userId, shortText: content.value })
  content.value = ''
  isExpanded.value = false
}
</script>

<template>
  <div class="border-b border-border-custom p-4">
    <div class="flex gap-3">
      <div class="w-10 h-10 rounded-full bg-border-custom flex-shrink-0 flex items-center justify-center text-text-secondary text-xs font-medium">
        {{ authStore.currentUser?.displayName?.charAt(0) }}
      </div>

      <div class="flex-1">
        <textarea
          v-model="content"
          placeholder="有什么新鲜事？"
          class="w-full bg-transparent text-text-primary text-lg placeholder-text-secondary outline-none resize-none min-h-[56px]"
          :rows="isExpanded ? 4 : 2"
          @focus="isExpanded = true"
        ></textarea>

        <div v-if="isExpanded" class="flex items-center justify-between mt-3 pt-3 border-t border-border-custom">
          <div class="flex items-center gap-2">
            <span
              class="text-sm"
              :class="isOverLimit ? 'text-danger' : charCount > maxLength * 0.9 ? 'text-yellow-500' : 'text-text-secondary'"
            >
              {{ charCount }}/{{ maxLength }}
            </span>
          </div>

          <button
            :disabled="!canSubmit"
            class="bg-accent text-white px-5 py-1.5 rounded text-sm font-semibold hover:bg-accent/90 transition-colors disabled:opacity-40"
            @click="handleSubmit"
          >
            {{ loading ? '发布中...' : '发布' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
