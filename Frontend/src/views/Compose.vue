<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { createPost } from '@/api/post'
import { uploadImage, uploadVideo } from '@/api/upload'

const router = useRouter()
const authStore = useAuthStore()

const title = ref('')
const content = ref('')
const maxContentLength = 500

// 已上传的媒体文件（含远程 URL）
const uploadedImages = ref([])   // [{ url, objectName, name }]
const uploadedVideo = ref(null)  // { url, objectName, name }

const fileInputRef = ref(null)
const videoInputRef = ref(null)
const loading = ref(false)
const uploading = ref(false)
const coverUrl = ref(null)  // 视频封面图URL

const hasVideo = computed(() => uploadedVideo.value !== null)
const hasImages = computed(() => uploadedImages.value.length > 0)

const canSubmit = computed(() => {
  if (title.value.trim().length === 0) return false
  if (loading.value || uploading.value) return false
  if (content.value.length > maxContentLength) return false
  return content.value.trim().length > 0 || hasImages.value || hasVideo.value
})

async function handleFileSelect(e) {
  const files = Array.from(e.target.files)
  if (!files.length) return

  // 互斥：选图片时清除已有的视频
  if (hasVideo.value) {
    uploadedVideo.value = null
  }

  uploading.value = true
  try {
    for (const file of files) {
      if (uploadedImages.value.length >= 3) break
      const res = await uploadImage(file)
      uploadedImages.value.push({ url: res.data.url, objectName: res.data.objectName, name: file.name })
    }
  } catch (err) {
    alert('图片上传失败: ' + (err.message || '未知错误'))
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

async function handleVideoSelect(e) {
  const file = e.target.files[0]
  if (!file) return

  // 互斥：选视频时清除已有的图片
  if (hasImages.value) {
    uploadedImages.value = []
  }

  uploading.value = true
  coverUrl.value = null
  try {
    const res = await uploadVideo(file)
    uploadedVideo.value = { url: res.data.url, objectName: res.data.objectName, name: file.name }

    // 自动截取视频第一帧作为封面图
    try {
      const blob = await captureVideoFrame(file)
      if (blob) {
        const coverFile = new File([blob], 'cover.jpg', { type: 'image/jpeg' })
        const coverRes = await uploadImage(coverFile)
        coverUrl.value = coverRes.data.url
      }
    } catch (coverErr) {
      console.warn('封面截取失败，继续发布:', coverErr)
    }
  } catch (err) {
    alert('视频上传失败: ' + (err.message || '未知错误'))
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

/**
 * 从视频文件中截取某一帧，返回 Blob
 * @param {File} file 视频文件
 * @param {number} seekTime 截取的时间点（秒），默认1秒
 */
function captureVideoFrame(file, seekTime = 1) {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video')
    video.preload = 'auto'
    video.muted = true
    video.playsInline = true
    video.crossOrigin = 'anonymous'

    const url = URL.createObjectURL(file)

    video.onloadedmetadata = () => {
      // 确保 seekTime 不超过视频时长
      const target = Math.min(seekTime, video.duration * 0.5)
      video.currentTime = target > 0 ? target : 0.1
    }

    video.onseeked = () => {
      // 等一帧再画，确保解码完成
      requestAnimationFrame(() => {
        try {
          const canvas = document.createElement('canvas')
          canvas.width = video.videoWidth
          canvas.height = video.videoHeight
          const ctx = canvas.getContext('2d')
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
          canvas.toBlob((blob) => {
            URL.revokeObjectURL(url)
            if (blob && blob.size > 0) {
              resolve(blob)
            } else {
              reject(new Error('截帧结果为空'))
            }
          }, 'image/jpeg', 0.85)
        } catch (err) {
          URL.revokeObjectURL(url)
          reject(err)
        }
      })
    }

    video.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('视频加载失败'))
    }

    video.src = url
  })
}

function removeImage(index) {
  uploadedImages.value.splice(index, 1)
}

function removeVideo() {
  uploadedVideo.value = null
  coverUrl.value = null
}

async function handleSubmit() {
  if (!canSubmit.value) return
  loading.value = true
  try {
    const contentType = hasVideo.value ? 2 : (hasImages.value ? 1 : 0)

    await createPost({
      contentType,
      title: title.value.trim(),
      description: content.value.trim() || undefined,
      visibility: 1,
      imageUrls: hasImages.value ? uploadedImages.value.map(img => img.url) : undefined,
      videoUrl: hasVideo.value ? uploadedVideo.value.url : undefined,
      coverUrl: hasVideo.value && coverUrl.value ? coverUrl.value : undefined
    })
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
        {{ loading ? '发布中...' : uploading ? '上传中...' : '发布' }}
      </button>
    </header>

    <div class="p-4">
      <div class="flex gap-3">
        <div class="w-10 h-10 rounded-full bg-border-custom flex-shrink-0 flex items-center justify-center text-text-secondary text-xs font-medium">
          {{ authStore.currentUser?.displayName?.charAt(0) }}
        </div>
        <div class="flex-1 min-w-0">
          <input
            v-model="title"
            placeholder="标题（必填）"
            class="w-full bg-transparent text-text-primary text-xl font-semibold placeholder-text-secondary outline-none mb-3 pb-3 border-b border-border-custom"
            maxlength="128"
          />
          <textarea
            v-model="content"
            placeholder="添加描述..."
            class="w-full bg-transparent text-text-primary text-lg placeholder-text-secondary outline-none resize-none min-h-[120px]"
          ></textarea>
        </div>
      </div>

      <!-- 上传中提示 -->
      <div v-if="uploading" class="mt-3 text-sm text-accent">
        上传中...
      </div>

      <!-- 图片预览 -->
      <div v-if="hasImages" class="mt-3 grid gap-2" :class="uploadedImages.length === 1 ? 'grid-cols-1' : 'grid-cols-2'">
        <div
          v-for="(item, idx) in uploadedImages"
          :key="idx"
          class="relative rounded-lg overflow-hidden border border-border-custom"
        >
          <img :src="item.url" class="w-full h-48 object-cover" />
          <button
            class="absolute top-2 right-2 w-7 h-7 rounded-full bg-black/60 text-white flex items-center justify-center hover:bg-black/80 transition-colors"
            @click="removeImage(idx)"
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

      <!-- 视频预览 -->
      <div v-if="hasVideo" class="mt-3">
        <div class="relative rounded-lg overflow-hidden border border-border-custom">
          <video :src="uploadedVideo.url" class="w-full h-48 object-cover" controls />
          <button
            class="absolute top-2 right-2 w-7 h-7 rounded-full bg-black/60 text-white flex items-center justify-center hover:bg-black/80 transition-colors"
            @click="removeVideo()"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
          <span class="absolute bottom-2 left-2 text-xs text-white bg-black/50 px-2 py-0.5 rounded">
            {{ uploadedVideo.name }}
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
        :class="content.length > maxContentLength ? 'text-danger' : content.length > maxContentLength * 0.9 ? 'text-yellow-500' : 'text-text-secondary'"
      >
        {{ content.length }}/{{ maxContentLength }}
      </span>
    </div>
  </div>
</template>
