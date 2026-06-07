<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getUser } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  show: { type: Boolean, default: false },
  userId: { type: [String, Number], default: null }
})

const emit = defineEmits(['close'])

const router = useRouter()
const authStore = useAuthStore()
const user = ref(null)
const loading = ref(false)
const showLogoutConfirm = ref(false)

watch(() => props.show, async (val) => {
  if (val && props.userId) {
    loading.value = true
    try {
      const res = await getUser(props.userId)
      user.value = res.data
    } catch {
      user.value = null
    } finally {
      loading.value = false
    }
  }
})

function goToProfile() {
  emit('close')
  router.push(`/profile/${props.userId}`)
}

function confirmLogout() {
  showLogoutConfirm.value = false
  emit('close')
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center" @click.self="emit('close')">
        <!-- 背景遮罩 -->
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>

        <!-- 弹窗主体 -->
        <div class="relative bg-bg-secondary border border-border-custom rounded-2xl w-full max-w-md mx-4 shadow-2xl overflow-hidden">
          <!-- 顶部关闭按钮 -->
          <div class="flex items-center justify-between px-4 py-3 border-b border-border-custom">
            <span class="text-base font-semibold text-text-primary">个人资料</span>
            <button
              class="w-8 h-8 flex items-center justify-center rounded-full text-text-secondary hover:text-text-primary hover:bg-bg-hover transition-colors"
              @click="emit('close')"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- 加载中 -->
          <div v-if="loading" class="p-10 text-center text-text-secondary">
            <div class="inline-block w-6 h-6 border-2 border-border-custom border-t-accent rounded-full animate-spin"></div>
          </div>

          <!-- 用户信息 -->
          <div v-else-if="user">
            <!-- 封面 + 头像 -->
            <div class="relative">
              <div class="h-28 bg-gradient-to-br from-bg-secondary to-bg-hover"></div>
              <div class="absolute -bottom-10 left-5">
                <div class="w-[76px] h-[76px] rounded-full bg-bg-primary p-[3px] ring-[3px] ring-bg-secondary">
                  <div class="w-full h-full rounded-full bg-accent/20 flex items-center justify-center text-2xl font-bold text-accent">
                    {{ user.username?.charAt(0)?.toUpperCase() }}
                  </div>
                </div>
              </div>
            </div>

            <!-- 用户名信息 -->
            <div class="pt-12 px-5 pb-5">
              <h2 class="text-lg font-bold text-text-primary leading-tight">{{ user.displayName }}</h2>
              <p class="text-sm text-text-secondary">@{{ user.username }}</p>

              <!-- bio -->
              <p v-if="user.bio" class="mt-3 text-sm text-text-primary leading-relaxed">{{ user.bio }}</p>

              <!-- 关注/粉丝 -->
              <div class="flex gap-5 mt-4 text-sm">
                <router-link :to="`/profile/${user.userId}/following`" @click="emit('close')">
                  <strong class="text-text-primary">{{ user.followsNumber ?? 0 }}</strong>
                  <span class="text-text-secondary ml-1">关注</span>
                </router-link>
                <router-link :to="`/profile/${user.userId}/followers`" @click="emit('close')">
                  <strong class="text-text-primary">{{ user.followerNumber ?? 0 }}</strong>
                  <span class="text-text-secondary ml-1">粉丝</span>
                </router-link>
              </div>
            </div>

            <!-- 操作区 -->
            <div class="border-t border-border-custom">
              <!-- 查看完整资料 -->
              <button
                class="flex items-center gap-3 w-full px-5 py-3.5 text-sm text-text-primary hover:bg-bg-hover transition-colors"
                @click="goToProfile"
              >
                <svg class="w-[18px] h-[18px] text-text-secondary" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                </svg>
                查看完整资料
              </button>

              <!-- 退出登录 -->
              <button
                class="flex items-center gap-3 w-full px-5 py-3.5 text-sm text-text-secondary hover:text-danger hover:bg-danger/5 transition-colors"
                @click="showLogoutConfirm = true"
              >
                <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9" />
                </svg>
                退出登录
              </button>
            </div>
          </div>
        </div>

        <!-- 退出确认二级弹窗 -->
        <Transition name="modal">
          <div
            v-if="showLogoutConfirm"
            class="absolute inset-0 z-10 flex items-center justify-center"
            @click.self="showLogoutConfirm = false"
          >
            <div class="bg-bg-secondary border border-border-custom rounded-2xl p-6 w-full max-w-xs mx-4 shadow-2xl">
              <h3 class="text-base font-bold text-text-primary">退出登录？</h3>
              <p class="mt-2 text-text-secondary text-sm leading-relaxed">确定要退出当前账号吗？退出后需要重新登录。</p>
              <div class="flex flex-col gap-2 mt-5">
                <button
                  class="w-full py-2.5 rounded-xl bg-danger text-white text-sm font-semibold hover:bg-danger/90 transition-colors"
                  @click="confirmLogout"
                >
                  退出
                </button>
                <button
                  class="w-full py-2.5 rounded-xl border border-border-custom text-text-primary text-sm font-medium hover:bg-bg-hover transition-colors"
                  @click="showLogoutConfirm = false"
                >
                  取消
                </button>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
