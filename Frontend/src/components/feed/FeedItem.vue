<script setup>
import { computed, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  post: { type: Object, required: true }
})

const emit = defineEmits(['like', 'comment'])

const router = useRouter()

// ========== 本地交互状态（后端未实现，先用前端状态模拟） ==========
const isLiked = ref(false)
const likeCount = ref(props.post.likeCount ?? 0)
const commentCount = ref(props.post.commentCount ?? 0)
const showCommentBox = ref(false)
const commentText = ref('')
const comments = ref([])  // 本地暂存的评论列表

// ========== 视频播放状态 ==========
const isVideoPlaying = ref(false)
const coverRatio = ref(null) // 封面宽高比 (width/height)，@load 后计算

function onCoverLoad(e) {
  const { naturalWidth: w, naturalHeight: h } = e.target
  if (w && h) coverRatio.value = w / h
}

// 是否为竖屏封面（宽高比 < 3:4）
const isVerticalCover = computed(() => coverRatio.value !== null && coverRatio.value < 0.75)

// ========== 图片 Lightbox ==========
const showLightbox = ref(false)
const lightboxIndex = ref(0)

const timeAgo = computed(() => {
  const diff = Date.now() - new Date(props.post.publishTime).getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  return `${days}天前`
})

const imageUrls = computed(() => props.post.imageUrls || [])
const hasImages = computed(() => imageUrls.value.length > 0)
const hasVideo = computed(() => !!props.post.videoUrl)

function goToPost() {
  router.push(`/post/${props.post.itemId}`)
}

function goToProfile() {
  router.push(`/profile/${props.post.creatorId}`)
}

/**
 * 点赞 / 取消点赞（纯前端，后端未对接）
 */
function handleLike() {
  isLiked.value = !isLiked.value
  likeCount.value += isLiked.value ? 1 : -1
  emit('like', { itemId: props.post.itemId, liked: isLiked.value })
}

/**
 * 打开/关闭评论输入框
 */
function toggleCommentBox() {
  showCommentBox.value = !showCommentBox.value
  if (showCommentBox.value) {
    nextTick(() => {
      const input = document.querySelector(`#comment-input-${props.post.itemId}`)
      input?.focus()
    })
  }
}

/**
 * 提交评论（纯前端暂存，后端未对接）
 */
function submitComment() {
  const text = commentText.value.trim()
  if (!text) return

  comments.value.push({
    id: Date.now(),
    text,
    username: '我',
    time: '刚刚'
  })
  commentCount.value++
  commentText.value = ''
  emit('comment', { itemId: props.post.itemId, text })
}

/**
 * 格式化视频时长
 */
function formatDuration(seconds) {
  if (!seconds) return ''
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

/**
 * 打开图片预览
 */
function openLightbox(index, event) {
  event.stopPropagation()
  lightboxIndex.value = index
  showLightbox.value = true
}

function closeLightbox() {
  showLightbox.value = false
}

function prevImage(event) {
  event.stopPropagation()
  lightboxIndex.value = (lightboxIndex.value - 1 + imageUrls.value.length) % imageUrls.value.length
}

function nextImage(event) {
  event.stopPropagation()
  lightboxIndex.value = (lightboxIndex.value + 1) % imageUrls.value.length
}

/**
 * 开始播放视频
 */
function playVideo(event) {
  event.stopPropagation()
  isVideoPlaying.value = true
}
</script>

<template>
  <article
    class="border-b border-border-custom p-4 hover:bg-bg-hover/50 cursor-pointer transition-colors"
    @click="goToPost"
  >
    <div class="flex gap-3">
      <!-- 头像 -->
      <div
        class="w-10 h-10 rounded-full bg-accent/20 flex-shrink-0 flex items-center justify-center cursor-pointer overflow-hidden"
        @click.stop="goToProfile"
      >
        <img v-if="post.avatar" :src="post.avatar" class="w-full h-full object-cover" />
        <span v-else class="text-sm font-bold">{{ post.displayName?.charAt(0) || post.username?.charAt(0) }}</span>
      </div>

      <div class="flex-1 min-w-0">
        <!-- 用户名 + 昵称 + 时间 -->
        <div class="flex items-center gap-2">
          <span class="font-bold text-text-primary truncate">{{ post.displayName || post.username }}</span>
          <span class="text-text-secondary truncate">@{{ post.username }}</span>
          <span class="text-text-secondary">·</span>
          <span class="text-text-secondary text-sm flex-shrink-0">{{ timeAgo }}</span>
        </div>

        <!-- 标题 -->
        <h3 v-if="post.title" class="mt-2 text-text-primary font-semibold break-words">{{ post.title }}</h3>

        <!-- 正文 -->
        <p v-if="post.summary" class="mt-1 text-text-primary whitespace-pre-wrap break-words">{{ post.summary }}</p>

        <!-- 图片网格 -->
        <div v-if="hasImages" class="mt-3">
          <!-- 1张：单张大图 -->
          <div v-if="imageUrls.length === 1" class="rounded-xl overflow-hidden">
            <img
              :src="imageUrls[0]"
              class="w-full max-h-[360px] object-cover cursor-pointer hover:opacity-90 transition-opacity"
              @click="openLightbox(0, $event)"
            />
          </div>
          <!-- 2张：左右并排 -->
          <div v-else-if="imageUrls.length === 2" class="grid grid-cols-2 gap-0.5 rounded-xl overflow-hidden">
            <img
              v-for="(url, idx) in imageUrls"
              :key="idx"
              :src="url"
              class="w-full h-[200px] object-cover cursor-pointer hover:opacity-90 transition-opacity"
              @click="openLightbox(idx, $event)"
            />
          </div>
          <!-- 3张：左大右二 -->
          <div v-else class="grid grid-cols-2 gap-0.5 rounded-xl overflow-hidden">
            <img
              :src="imageUrls[0]"
              class="w-full h-[280px] object-cover cursor-pointer hover:opacity-90 transition-opacity row-span-2"
              @click="openLightbox(0, $event)"
            />
            <div class="flex flex-col gap-0.5">
              <img
                :src="imageUrls[1]"
                class="w-full h-[139px] object-cover cursor-pointer hover:opacity-90 transition-opacity"
                @click="openLightbox(1, $event)"
              />
              <img
                :src="imageUrls[2]"
                class="w-full h-[139px] object-cover cursor-pointer hover:opacity-90 transition-opacity"
                @click="openLightbox(2, $event)"
              />
            </div>
          </div>
        </div>

        <!-- 视频展示 -->
        <div v-if="hasVideo" class="mt-3">
          <!-- 未播放：显示封面 + 播放按钮 -->
          <div
            v-if="!isVideoPlaying"
            class="relative rounded-xl overflow-hidden cursor-pointer group"
            @click="playVideo($event)"
          >
            <img
              v-if="post.videoCoverUrl"
              :src="post.videoCoverUrl"
              :style="isVerticalCover ? { maxHeight: '480px', objectFit: 'contain' } : {}"
              :class="isVerticalCover
                ? 'w-full group-hover:opacity-90 transition-opacity'
                : 'w-full max-h-[360px] object-cover group-hover:opacity-90 transition-opacity'"
              @load="onCoverLoad"
            />
            <div v-else class="w-full h-[240px] bg-black/80 flex items-center justify-center">
              <svg xmlns="http://www.w3.org/2000/svg" fill="white" viewBox="0 0 24 24" class="w-16 h-16 opacity-50">
                <path d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.347a1.125 1.125 0 010 1.972l-11.54 6.347a1.125 1.125 0 01-1.667-.986V5.653z" />
              </svg>
            </div>
            <!-- 中央播放按钮 -->
            <div class="absolute inset-0 flex items-center justify-center">
              <div class="w-14 h-14 bg-black/60 rounded-full flex items-center justify-center group-hover:bg-black/80 transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" fill="white" viewBox="0 0 24 24" class="w-7 h-7 ml-1">
                  <path d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.347a1.125 1.125 0 010 1.972l-11.54 6.347a1.125 1.125 0 01-1.667-.986V5.653z" />
                </svg>
              </div>
            </div>
            <!-- 时长标签 -->
            <span
              v-if="post.videoDuration"
              class="absolute bottom-2 right-2 bg-black/70 text-white text-xs px-2 py-0.5 rounded"
            >
              {{ formatDuration(post.videoDuration) }}
            </span>
          </div>
          <!-- 播放中：内嵌 video 标签 -->
          <video
            v-else
            :src="post.videoUrl"
            controls
            autoplay
            class="w-full max-h-[480px] rounded-xl"
          />
        </div>

        <!-- 操作栏 -->
        <div class="flex items-center gap-10 mt-3">
          <!-- 点赞按钮 -->
          <button
            class="flex items-center gap-1.5 transition-colors group"
            :class="isLiked ? 'text-danger' : 'text-text-secondary hover:text-danger'"
            @click.stop="handleLike"
          >
            <span
              class="p-1.5 rounded-full transition-colors text-lg leading-none"
              :class="isLiked ? 'bg-danger/10' : 'group-hover:bg-danger/10'"
            >
              <svg v-if="isLiked" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-5 h-5">
                <path d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.688 3A5.5 5.5 0 0112 5.052 5.5 5.5 0 0116.313 3c2.973 0 5.437 2.322 5.437 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z" />
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" />
              </svg>
            </span>
            <span class="text-sm">{{ likeCount || '' }}</span>
          </button>

          <!-- 评论按钮 -->
          <button
            class="flex items-center gap-1.5 text-text-secondary hover:text-accent transition-colors group"
            @click.stop="toggleCommentBox"
          >
            <span class="p-1.5 rounded-full group-hover:bg-accent/10 transition-colors">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 20.25c4.97 0 9-3.694 9-8.25s-4.03-8.25-9-8.25S3 7.444 3 12c0 2.104.859 4.023 2.273 5.48.432.447.74 1.04.586 1.641a4.483 4.483 0 01-.923 1.785A5.969 5.969 0 006 21c1.282 0 2.47-.402 3.445-1.087.81.22 1.668.337 2.555.337z" />
              </svg>
            </span>
            <span class="text-sm">{{ commentCount || '' }}</span>
          </button>
        </div>

        <!-- 评论输入框（展开/收起） -->
        <transition name="fade">
          <div v-if="showCommentBox" class="mt-3" @click.stop>
            <div class="flex gap-2">
              <input
                :id="`comment-input-${post.itemId}`"
                v-model="commentText"
                type="text"
                placeholder="写一条评论..."
                class="flex-1 bg-bg-secondary border border-border-custom rounded-full px-4 py-2 text-sm text-text-primary placeholder-text-secondary outline-none focus:border-accent transition-colors"
                @keyup.enter="submitComment"
              />
              <button
                class="px-4 py-2 bg-accent text-white text-sm font-bold rounded-full hover:bg-accent/80 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                :disabled="!commentText.trim()"
                @click.stop="submitComment"
              >
                发送
              </button>
            </div>

            <!-- 已发评论列表 -->
            <div v-if="comments.length" class="mt-2 space-y-2">
              <div
                v-for="c in comments"
                :key="c.id"
                class="flex gap-2 items-start"
              >
                <div class="w-6 h-6 rounded-full bg-accent/20 flex-shrink-0 flex items-center justify-center">
                  <span class="text-xs font-bold">{{ c.username.charAt(0) }}</span>
                </div>
                <div>
                  <span class="text-sm font-bold text-text-primary">{{ c.username }}</span>
                  <span class="text-sm text-text-secondary ml-2">{{ c.time }}</span>
                  <p class="text-sm text-text-primary mt-0.5">{{ c.text }}</p>
                </div>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <!-- 图片预览 Lightbox -->
    <teleport to="body">
      <div
        v-if="showLightbox"
        class="fixed inset-0 z-[9999] bg-black/90 flex items-center justify-center"
        @click.self="closeLightbox"
        @keyup.escape="closeLightbox"
      >
        <!-- 关闭按钮 -->
        <button
          class="absolute top-4 right-4 z-10 text-white/80 hover:text-white transition-colors"
          @click="closeLightbox"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-8 h-8">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <!-- 左箭头 -->
        <button
          v-if="imageUrls.length > 1"
          class="absolute left-4 z-10 w-10 h-10 bg-black/40 hover:bg-black/70 rounded-full flex items-center justify-center text-white transition-colors"
          @click="prevImage($event)"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
          </svg>
        </button>

        <!-- 图片 -->
        <img
          :src="imageUrls[lightboxIndex]"
          class="max-h-[85vh] max-w-[90vw] object-contain select-none"
          @click.stop
        />

        <!-- 右箭头 -->
        <button
          v-if="imageUrls.length > 1"
          class="absolute right-4 z-10 w-10 h-10 bg-black/40 hover:bg-black/70 rounded-full flex items-center justify-center text-white transition-colors"
          @click="nextImage($event)"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
          </svg>
        </button>

        <!-- 图片计数器 -->
        <div
          v-if="imageUrls.length > 1"
          class="absolute bottom-6 left-1/2 -translate-x-1/2 text-white/80 text-sm bg-black/50 px-3 py-1 rounded-full"
        >
          {{ lightboxIndex + 1 }} / {{ imageUrls.length }}
        </div>
      </div>
    </teleport>
  </article>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: all 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
