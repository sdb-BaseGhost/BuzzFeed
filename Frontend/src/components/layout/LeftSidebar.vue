<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const navItems = [
  { label: '首页', icon: 'home', path: '/' },
  { label: '探索', icon: 'explore', path: '/explore' },
  { label: '通知', icon: 'notifications', path: '/notifications' },
  { label: '消息', icon: 'messages', path: '/messages' },
]

const icons = {
  home: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-4 0h4',
  explore: 'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z',
  notifications: 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9',
  messages: 'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z',
}
</script>

<template>
  <nav class="flex flex-col items-center lg:items-stretch h-screen sticky top-0 py-4">
    <div class="flex justify-center lg:justify-start mb-6 px-3">
      <span class="text-accent text-xl font-bold tracking-tight">BuzzFeed</span>
    </div>

    <router-link
      v-for="item in navItems"
      :key="item.path"
      :to="item.path"
      class="flex items-center justify-center lg:justify-start gap-4 px-4 py-2.5 mx-1 rounded-full transition-colors mb-0.5 group w-fit"
      :class="route.path === item.path ? 'text-text-primary font-semibold' : 'text-text-secondary hover:text-text-primary hover:bg-bg-hover/50'"
    >
      <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" :d="icons[item.icon]" />
      </svg>
      <span class="hidden lg:block text-base" :class="route.path === item.path ? 'font-semibold' : ''">{{ item.label }}</span>
    </router-link>

    <button
      class="w-10 h-10 lg:w-auto lg:px-6 lg:py-2.5 border border-accent text-accent rounded-full mt-6 mx-1 text-sm font-semibold hover:bg-accent/5 transition-colors self-center lg:self-start"
      @click="router.push('/compose')"
    >
      <span class="hidden lg:block">发布</span>
      <span class="lg:hidden text-lg">+</span>
    </button>

    <div class="mt-auto">
      <router-link
        v-if="authStore.currentUser"
        :to="`/profile/${authStore.currentUser.userId}`"
        class="flex items-center justify-center lg:justify-start gap-3 px-3 py-2.5 rounded-lg hover:bg-bg-hover/50 transition-colors"
      >
        <div class="w-8 h-8 rounded-full bg-border-custom flex items-center justify-center text-text-secondary text-xs font-medium">
          {{ authStore.currentUser.username?.charAt(0)?.toUpperCase() }}
        </div>
        <div class="hidden lg:block min-w-0">
          <div class="text-sm font-medium text-text-primary truncate">{{ authStore.currentUser.username }}</div>
          <div class="text-text-secondary text-xs truncate">@{{ authStore.currentUser.username }}</div>
        </div>
      </router-link>
    </div>
  </nav>
</template>
