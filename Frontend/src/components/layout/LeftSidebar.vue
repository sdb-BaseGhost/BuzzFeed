<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const navItems = [
  { label: '首页', icon: 'home', path: '/' },
  { label: '关注', icon: 'following', path: '/following' },
  { label: '探索', icon: 'explore', path: '/explore' },
  { label: '通知', icon: 'notifications', path: '/notifications' },
  { label: '消息', icon: 'messages', path: '/messages' },
]
</script>

<template>
  <nav class="flex flex-col items-center lg:items-start h-screen sticky top-0 py-3 px-2">
    <div class="p-3 mb-2">
      <span class="text-accent text-2xl font-bold">B</span>
    </div>

    <router-link
      v-for="item in navItems"
      :key="item.path"
      :to="item.path"
      class="flex items-center gap-4 p-3 rounded-full hover:bg-bg-hover transition-colors mb-1 group"
      :class="{ 'font-bold': route.path === item.path }"
    >
      <span class="text-xl">{{ item.icon === 'home' ? '🏠' : item.icon === 'following' ? '👥' : item.icon === 'explore' ? '🔍' : item.icon === 'notifications' ? '🔔' : '💬' }}</span>
      <span class="hidden lg:block text-text-primary text-lg">{{ item.label }}</span>
    </router-link>

    <button
      class="w-12 h-12 lg:w-full lg:h-auto lg:py-3 bg-accent rounded-full mt-4 text-white font-bold hover:bg-accent/90 transition-colors"
    >
      <span class="hidden lg:block">发布</span>
      <span class="lg:hidden text-xl">+</span>
    </button>

    <div class="mt-auto">
      <router-link
        v-if="authStore.currentUser"
        :to="`/profile/${authStore.currentUser.userId}`"
        class="flex items-center gap-3 p-3 rounded-full hover:bg-bg-hover transition-colors"
      >
        <div class="w-10 h-10 rounded-full bg-accent/20 flex items-center justify-center">
          <span class="text-sm">{{ authStore.currentUser.username?.charAt(0)?.toUpperCase() }}</span>
        </div>
        <div class="hidden lg:block">
          <div class="font-bold text-sm">{{ authStore.currentUser.username }}</div>
          <div class="text-text-secondary text-sm">@{{ authStore.currentUser.username }}</div>
        </div>
      </router-link>
    </div>
  </nav>
</template>
