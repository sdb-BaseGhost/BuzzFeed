<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { createPost } from '@/api/post'
import { uploadImage, uploadVideo } from '@/api/upload'

const router = useRouter()
const authStore = useAuthStore()

const content = ref('')
const maxLength = 280
const mediaFiles = ref([])
const mediaPreview = ref([])
const fileInputRef = ref(null)
const videoInputRef = ref(null)
const loading = ref(false)

const charCount = computed(() => content.value.length)
const isOverLimit = computed(() => charCount.value > maxLength)
const canSubmit = computed(() => (content.value.trim().length > 0 || mediaFiles.value.length > 0) && !isOverLimit.value && !loading.value)

function handleFileSelect(e) {
  const files = Array.from(e.target.files)
  files.forEach(file => {
    if (mediaFiles.value.length >= 9) return
    mediaFiles.value.push(file)
    const reader = new FileReader()
    reader.onload = (ev) => {
      mediaPreview.value.push({
        url: ev.target.result,
        type: 'image',
        name: file.name
      })
    }
    reader.readAsDataURL(file)
  })
  e.target.value = ''
}

function handleVideoSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  mediaFiles.value.push(file)
  const reader = new FileReader()
  reader.onload = (ev) => {
    mediaPreview.value.push({
      url: ev.target.result,
      type: 'video',
      name: file.name
    })
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}

function removeMedia(index) {
  mediaFiles.value.splice(index, 1)
  mediaPreview.value.splice(index, 1)
}

async function handleSubmit() {
  if (!canSubmit.value) return
  loading.value = true
  try {
    const imageUrls = []
    let videoUrl = null

    for (let i = 0; i < mediaFiles.value.length; i++) {
      const file = mediaFiles.value[i]
      if (mediaPreview.value[i].type === 'video') {
        const res = await uploadVideo(file)
        videoUrl = res.data
      } else {
        const res = await uploadImage(file)
        imageUrls.push(res.data)
      }
    }

    const data = {
      creatorId: authStore.currentUser?.userId,
      shortText: content.value,
      imageUrls: imageUrls.length > 0 ? imageUrls : null,
      videoUrl: videoUrl
    }

    await createPost(data)
    router.push('/')
  } catch (e) {
    alert('发布失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom flex items-center justify-between px-4 py-3">
      <button class="text-text-secondary hover:text-text-primary transition-colors" @click="router.back()">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
      <button
        :disabled="!canSubmit"
        class="bg-accent text-white px-5 py-1.5 rounded text-sm font-semibold hover:bg-accent/90 transition-colors disabled:opacity-40"
        @click="handleSubmit"
      >
        {{ loading ? '发布中...' : '发布' }}
      </button>
    </header>

    <div class="p-4">
      <div class="flex gap-3">
        <div class="w-10 h-10 rounded-full bg-border-custom flex-shrink-0 flex items-center justify-center text-text-secondary text-xs font-medium">
          {{ authStore.currentUser?.displayName?.charAt(0) }}
        </div>
        <div class="flex-1 min-w-0">
          <textarea
            v-model="content"
            placeholder="有什么新鲜事？"
            class="w-full bg-transparent text-text-primary text-lg placeholder-text-secondary outline-none resize-none min-h-[160px]"
            autofocus
          ></textarea>
        </div>
      </div>

      <!-- Media preview -->
      <div v-if="mediaPreview.length" class="mt-3 grid gap-2" :class="mediaPreview.length === 1 ? 'grid-cols-1' : 'grid-cols-2'">
        <div
          v-for="(item, idx) in mediaPreview"
          :key="idx"
          class="relative rounded-lg overflow-hidden border border-border-custom"
        >
          <img v-if="item.type === 'image'" :src="item.url" class="w-full h-48 object-cover" />
          <video v-else :src="item.url" class="w-full h-48 object-cover" controls />
          <button
            class="absolute top-2 right-2 w-7 h-7 rounded-full bg-black/60 text-white flex items-center justify-center hover:bg-black/80 transition-colors"
            @click="removeMedia(idx)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
          <span class="absolute bottom-2 left-2 text-xs text-white bg-black/50 px-2 py-0.5 rounded">
            {{ item.name }}
          </span>
        </div>
      </div>
    </div>

    <!-- Bottom toolbar -->
    <div class="border-t border-border-custom px-4 py-3 flex items-center justify-between">
      <div class="flex items-center gap-1">
        <input ref="fileInputRef" type="file" accept="image/*" multiple class="hidden" @change="handleFileSelect" />
        <button
          class="p-2 rounded-full text-accent hover:bg-accent/5 transition-colors"
          title="上传图片"
          @click="fileInputRef?.click()"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909M3.75 21h16.5A2.25 2.25 0 0022.5 18.75V5.25A2.25 2.25 0 0020.25 3H3.75A2.25 2.25 0 001.5 5.25v13.5A2.25 2.25 0 003.75 21zM10.5 8.25a1.125 1.125 0 11-2.25 0 1.125 1.125 0 012.25 0z" />
          </svg>
        </button>
        <input ref="videoInputRef" type="file" accept="video/*" class="hidden" @change="handleVideoSelect" />
        <button
          class="p-2 rounded-full text-accent hover:bg-accent/5 transition-colors"
          title="上传视频"
          @click="videoInputRef?.click()"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3.375 19.5h17.25m-17.25 0a1.125 1.125 0 01-1.125-1.125M3.375 19.5h1.5C5.496 19.5 6 18.996 6 18.375m-3.75 0V5.625m0 12.75v-1.5c0-.621.504-1.125 1.125-1.125m18.375 2.625V5.625m0 12.75c0 .621-.504 1.125-1.125 1.125m1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125m0 3.75h-1.5A1.125 1.125 0 0118 18.375M20.625 4.5H3.375m17.25 0c.621 0 1.125.504 1.125 1.125M20.625 4.5h-1.5C18.504 4.5 18 5.004 18 5.625m3.75 0v1.5c0 .621-.504 1.125-1.125 1.125M3.375 4.5c-.621 0-1.125.504-1.125 1.125M3.375 4.5h1.5C5.496 4.5 6 5.004 6 5.625m-3.75 0v1.5c0 .621.504 1.125 1.125 1.125m0 0h1.5m-1.5 0c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125m1.5-3.75C5.496 8.25 6 7.746 6 7.125v-1.5M4.875 8.25C5.496 8.25 6 8.754 6 9.375v1.5m0-5.25v5.25m0-5.25C6 5.004 6.504 4.5 7.125 4.5h9.75c.621 0 1.125.504 1.125 1.125m1.125 2.625h1.5m-1.5 0A1.125 1.125 0 0118 7.125v-1.5m1.125 2.625c-.621 0-1.125.504-1.125 1.125v1.5m2.625-2.625c.621 0 1.125.504 1.125 1.125v1.5c0 .621-.504 1.125-1.125 1.125M18 5.625v5.25M7.125 12h9.75m-9.75 0A1.125 1.125 0 016 10.875M7.125 12C6.504 12 6 12.504 6 13.125m0-2.25C6 11.496 5.496 12 4.875 12M18 10.875c0 .621-.504 1.125-1.125 1.125M18 10.875c0 .621.504 1.125 1.125 1.125m-2.25 0c.621 0 1.125.504 1.125 1.125m-12 5.25v-5.25m0 5.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125m-12 0v-1.5c0-.621-.504-1.125-1.125-1.125M18 18.375v-5.25m0 5.25v-1.5c0-.621.504-1.125 1.125-1.125M18 13.125v1.5c0 .621.504 1.125 1.125 1.125M18 13.125c0-.621.504-1.125 1.125-1.125M6 13.125v1.5c0 .621-.504 1.125-1.125 1.125M6 13.125C6 12.504 5.496 12 4.875 12m-1.5 0h1.5m-1.5 0c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125m1.5-3.75c.621 0 1.125.504 1.125 1.125v1.5c0 .621-.504 1.125-1.125 1.125m0 0h-1.5" />
          </svg>
        </button>
      </div>

      <span
        class="text-sm"
        :class="isOverLimit ? 'text-danger' : charCount > maxLength * 0.9 ? 'text-yellow-500' : 'text-text-secondary'"
      >
        {{ charCount }}/{{ maxLength }}
      </span>
    </div>
  </div>
</template>
