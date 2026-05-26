# BuzzFeed Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Twitter-like social media frontend with Vue 3, using mock data for independent development, ready for Spring Boot backend integration.

**Architecture:** Three-column Twitter layout (left nav / center feed / right sidebar) with Vue 3 Composition API, Pinia state management, Vue Router with auth guards, and Axios with mock/real API toggle. Components are organized by feature (feed, post, user, comment, common).

**Tech Stack:** Vue 3, Vite, Tailwind CSS, Pinia, Vue Router 4, Axios

---

## Phase 1: Skeleton + Auth

### Task 1: Project Initialization

**Files:**
- Create: `Frontend/package.json`
- Create: `Frontend/vite.config.js`
- Create: `Frontend/tailwind.config.js`
- Create: `Frontend/postcss.config.js`
- Create: `Frontend/index.html`
- Create: `Frontend/src/main.js`
- Create: `Frontend/src/App.vue`
- Create: `Frontend/src/style.css`
- Create: `Frontend/.env`

- [ ] **Step 1: Create package.json**

```json
{
  "name": "buzzfeed-frontend",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.7",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.4.0",
    "tailwindcss": "^3.4.0",
    "postcss": "^8.4.35",
    "autoprefixer": "^10.4.18"
  }
}
```

- [ ] **Step 2: Create vite.config.js**

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3000
  }
})
```

- [ ] **Step 3: Create tailwind.config.js**

```js
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}'
  ],
  theme: {
    extend: {
      colors: {
        'bg-primary': '#15202b',
        'bg-secondary': '#192734',
        'bg-hover': '#1d2f3f',
        'border-custom': '#38444d',
        'text-primary': '#e7e9ea',
        'text-secondary': '#536471',
        'accent': '#1d9bf0',
        'danger': '#f4212e',
        'success': '#00ba7c'
      }
    }
  },
  plugins: []
}
```

- [ ] **Step 4: Create postcss.config.js**

```js
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {}
  }
}
```

- [ ] **Step 5: Create index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>BuzzFeed</title>
  </head>
  <body class="bg-bg-primary text-text-primary">
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

- [ ] **Step 6: Create src/style.css**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}

::-webkit-scrollbar {
  width: 4px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #38444d;
  border-radius: 2px;
}
```

- [ ] **Step 7: Create .env**

```
VITE_USE_MOCK=true
VITE_API_BASE_URL=http://localhost:8000
```

- [ ] **Step 8: Create src/main.js**

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
```

- [ ] **Step 9: Create src/App.vue**

```vue
<script setup>
import AppLayout from '@/components/layout/AppLayout.vue'
</script>

<template>
  <AppLayout />
</template>
```

- [ ] **Step 10: Install dependencies and verify**

Run: `cd D:/BuzzFeed/Frontend && npm install`
Expected: Dependencies installed successfully

- [ ] **Step 11: Commit**

```bash
cd D:/BuzzFeed/Frontend
git init
git add -A
git commit -m "chore: initialize Vue 3 + Vite + Tailwind project"
```

---

### Task 2: Three-Column Layout Skeleton

**Files:**
- Create: `Frontend/src/components/layout/AppLayout.vue`
- Create: `Frontend/src/components/layout/LeftSidebar.vue`
- Create: `Frontend/src/components/layout/RightSidebar.vue`

- [ ] **Step 1: Create AppLayout.vue**

```vue
<script setup>
import LeftSidebar from './LeftSidebar.vue'
import RightSidebar from './RightSidebar.vue'
</script>

<template>
  <div class="flex justify-center min-h-screen mx-auto max-w-[1280px]">
    <LeftSidebar class="w-[60px] lg:w-[275px] flex-shrink-0" />
    <main class="w-full max-w-[600px] border-x border-border-custom min-h-screen">
      <router-view />
    </main>
    <RightSidebar class="w-[350px] flex-shrink-0 hidden lg:block ml-7" />
  </div>
</template>
```

- [ ] **Step 2: Create LeftSidebar.vue**

```vue
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
```

- [ ] **Step 3: Create RightSidebar.vue**

```vue
<script setup>
import { ref } from 'vue'

const searchQuery = ref('')
const trendingTopics = ref([
  { id: 1, category: '科技', title: 'Vue 3', posts: '12.5K' },
  { id: 2, category: '编程', title: 'JavaScript', posts: '8.2K' },
  { id: 3, category: '开发', title: 'Spring Boot', posts: '5.1K' },
])

const suggestedUsers = ref([
  { id: 1, username: 'vuejs', displayName: 'Vue.js', avatar: null },
  { id: 2, username: 'springboot', displayName: 'Spring Boot', avatar: null },
  { id: 3, username: 'tailwindcss', displayName: 'Tailwind CSS', avatar: null },
])
</script>

<template>
  <aside class="py-3 pr-6">
    <div class="sticky top-0 pt-1 pb-3 bg-bg-primary z-10">
      <div class="relative">
        <span class="absolute left-4 top-1/2 -translate-y-1/2 text-text-secondary">🔍</span>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索"
          class="w-full bg-bg-secondary border border-transparent focus:border-accent rounded-full py-3 pl-12 pr-4 text-text-primary placeholder-text-secondary outline-none transition-colors"
        />
      </div>
    </div>

    <div class="bg-bg-secondary rounded-2xl mt-3 p-4">
      <h2 class="text-xl font-bold mb-3">热门话题</h2>
      <div
        v-for="topic in trendingTopics"
        :key="topic.id"
        class="py-3 hover:bg-bg-hover rounded-lg px-2 cursor-pointer transition-colors"
      >
        <div class="text-text-secondary text-sm">{{ topic.category }} · 热门</div>
        <div class="font-bold mt-0.5">{{ topic.title }}</div>
        <div class="text-text-secondary text-sm mt-0.5">{{ topic.posts }} 条帖子</div>
      </div>
    </div>

    <div class="bg-bg-secondary rounded-2xl mt-4 p-4">
      <h2 class="text-xl font-bold mb-3">推荐关注</h2>
      <div
        v-for="user in suggestedUsers"
        :key="user.id"
        class="flex items-center justify-between py-3"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-accent/20 flex items-center justify-center">
            <span class="text-sm font-bold">{{ user.displayName.charAt(0) }}</span>
          </div>
          <div>
            <div class="font-bold text-sm">{{ user.displayName }}</div>
            <div class="text-text-secondary text-sm">@{{ user.username }}</div>
          </div>
        </div>
        <button class="bg-text-primary text-bg-primary px-4 py-1.5 rounded-full text-sm font-bold hover:bg-text-primary/90 transition-colors">
          关注
        </button>
      </div>
    </div>
  </aside>
</template>
```

- [ ] **Step 4: Verify layout renders**

Run: `cd D:/BuzzFeed/Frontend && npm run dev`
Expected: Dev server starts, three-column layout visible in browser

- [ ] **Step 5: Commit**

```bash
cd D:/BuzzFeed/Frontend
git add -A
git commit -m "feat: add three-column layout skeleton (left/center/right)"
```

---

### Task 3: Router + Auth Guards

**Files:**
- Create: `Frontend/src/router/index.js`

- [ ] **Step 1: Create router/index.js**

```js
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresGuest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresGuest: true }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/following',
    name: 'Following',
    component: () => import('@/views/Following.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/explore',
    name: 'Explore',
    component: () => import('@/views/Explore.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/Notifications.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('@/views/Messages.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/:userId',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/:userId/followers',
    name: 'Followers',
    component: () => import('@/views/Followers.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/:userId/following',
    name: 'FollowingList',
    component: () => import('@/views/FollowingList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/post/:postId',
    name: 'PostDetail',
    component: () => import('@/views/PostDetail.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.requiresGuest && authStore.isLoggedIn) {
    next('/')
  } else {
    next()
  }
})

export default router
```

- [ ] **Step 2: Create placeholder view files**

Create each view file with minimal content so the router can lazy-load them:

**Frontend/src/views/Login.vue:**
```vue
<template>
  <div class="p-4"><h1 class="text-2xl font-bold">登录</h1></div>
</template>
```

**Frontend/src/views/Register.vue:**
```vue
<template>
  <div class="p-4"><h1 class="text-2xl font-bold">注册</h1></div>
</template>
```

**Frontend/src/views/Home.vue:**
```vue
<template>
  <div class="p-4"><h1 class="text-2xl font-bold">首页</h1></div>
</template>
```

**Frontend/src/views/Following.vue:**
```vue
<template>
  <div class="p-4"><h1 class="text-2xl font-bold">关注</h1></div>
</template>
```

**Frontend/src/views/Explore.vue:**
```vue
<template>
  <div class="p-4"><h1 class="text-2xl font-bold">探索</h1></div>
</template>
```

**Frontend/src/views/Notifications.vue:**
```vue
<template>
  <div class="p-4"><h1 class="text-2xl font-bold">通知</h1></div>
</template>
```

**Frontend/src/views/Messages.vue:**
```vue
<template>
  <div class="p-4"><h1 class="text-2xl font-bold">消息</h1></div>
</template>
```

**Frontend/src/views/Profile.vue:**
```vue
<template>
  <div class="p-4"><h1 class="text-2xl font-bold">个人资料</h1></div>
</template>
```

**Frontend/src/views/Followers.vue:**
```vue
<template>
  <div class="p-4"><h1 class="text-2xl font-bold">粉丝</h1></div>
</template>
```

**Frontend/src/views/FollowingList.vue:**
```vue
<template>
  <div class="p-4"><h1 class="text-2xl font-bold">关注列表</h1></div>
</template>
```

**Frontend/src/views/PostDetail.vue:**
```vue
<template>
  <div class="p-4"><h1 class="text-2xl font-bold">帖子详情</h1></div>
</template>
```

- [ ] **Step 3: Verify routing works**

Run: `cd D:/BuzzFeed/Frontend && npm run dev`
Expected: Navigate to `/login` shows Login page, navigate to `/` redirects to `/login`

- [ ] **Step 4: Commit**

```bash
cd D:/BuzzFeed/Frontend
git add -A
git commit -m "feat: add Vue Router with auth guards and placeholder views"
```

---

### Task 4: Pinia Stores (auth, feed, ui)

**Files:**
- Create: `Frontend/src/stores/auth.js`
- Create: `Frontend/src/stores/feed.js`
- Create: `Frontend/src/stores/ui.js`

- [ ] **Step 1: Create stores/auth.js**

```js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, register as apiRegister, getCurrentUser } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const currentUser = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  async function login(username, password) {
    const res = await apiLogin({ username, password })
    token.value = res.data.token
    localStorage.setItem('token', res.data.token)
    await fetchCurrentUser()
  }

  async function register(username, password, email) {
    await apiRegister({ username, password, email })
  }

  async function fetchCurrentUser() {
    if (!token.value) return
    try {
      const res = await getCurrentUser()
      currentUser.value = res.data
    } catch {
      logout()
    }
  }

  function logout() {
    token.value = null
    currentUser.value = null
    localStorage.removeItem('token')
  }

  return { token, currentUser, isLoggedIn, login, register, fetchCurrentUser, logout }
})
```

- [ ] **Step 2: Create stores/feed.js**

```js
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getFeed } from '@/api/feed'
import { toggleLike as apiToggleLike } from '@/api/post'

export const useFeedStore = defineStore('feed', () => {
  const posts = ref([])
  const followingPosts = ref([])
  const cursor = ref(null)
  const loading = ref(false)
  const hasMore = ref(true)

  async function fetchFeed(type = 'recommend') {
    loading.value = true
    try {
      const res = await getFeed({ type, cursor: null })
      if (type === 'recommend') {
        posts.value = res.data.posts
      } else {
        followingPosts.value = res.data.posts
      }
      cursor.value = res.data.cursor
      hasMore.value = res.data.hasMore
    } finally {
      loading.value = false
    }
  }

  async function loadMore(type = 'recommend') {
    if (loading.value || !hasMore.value) return
    loading.value = true
    try {
      const res = await getFeed({ type, cursor: cursor.value })
      const target = type === 'recommend' ? posts : followingPosts
      target.value.push(...res.data.posts)
      cursor.value = res.data.cursor
      hasMore.value = res.data.hasMore
    } finally {
      loading.value = false
    }
  }

  function addPost(post) {
    posts.value.unshift(post)
  }

  async function toggleLike(postId) {
    const res = await apiToggleLike(postId)
    const post = posts.value.find(p => p.postId === postId) || followingPosts.value.find(p => p.postId === postId)
    if (post) {
      post.isLiked = res.data.isLiked
      post.likeCount += res.data.isLiked ? 1 : -1
    }
  }

  return { posts, followingPosts, cursor, loading, hasMore, fetchFeed, loadMore, addPost, toggleLike }
})
```

- [ ] **Step 3: Create stores/ui.js**

```js
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const showPostComposer = ref(false)
  const showLoginModal = ref(false)

  return { showPostComposer, showLoginModal }
})
```

- [ ] **Step 4: Commit**

```bash
cd D:/BuzzFeed/Frontend
git add -A
git commit -m "feat: add Pinia stores (auth, feed, ui)"
```

---

### Task 5: API Layer + Mock Infrastructure

**Files:**
- Create: `Frontend/src/api/index.js`
- Create: `Frontend/src/api/auth.js`
- Create: `Frontend/src/api/feed.js`
- Create: `Frontend/src/api/post.js`
- Create: `Frontend/src/api/user.js`
- Create: `Frontend/src/api/search.js`
- Create: `Frontend/src/mock/index.js`
- Create: `Frontend/src/mock/auth.js`
- Create: `Frontend/src/mock/feed.js`
- Create: `Frontend/src/mock/users.js`
- Create: `Frontend/src/mock/posts.js`

- [ ] **Step 1: Create api/index.js**

```js
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000',
  timeout: 10000
})

api.interceptors.request.use(config => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})

api.interceptors.response.use(
  response => {
    const { code, msg, data } = response.data
    if (code !== 200) {
      return Promise.reject(new Error(msg || '请求失败'))
    }
    return { code, msg, data }
  },
  error => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

export default api
```

- [ ] **Step 2: Create mock/index.js**

```js
let nextId = 100

export function generateId() {
  return String(nextId++)
}

export function delay(ms = 300) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

export function mockResult(data) {
  return { code: 200, msg: 'success', data }
}
```

- [ ] **Step 3: Create mock/users.js**

```js
export const mockUsers = {
  '1': { userId: '1', username: 'testuser', displayName: '测试用户', bio: '这是测试用户', avatar: null, followerCount: 120, followingCount: 45, postCount: 88 },
  '2': { userId: '2', username: 'vuejs', displayName: 'Vue.js', bio: 'The Progressive JavaScript Framework', avatar: null, followerCount: 50000, followingCount: 100, postCount: 500 },
  '3': { userId: '3', username: 'springboot', displayName: 'Spring Boot', bio: 'Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications', avatar: null, followerCount: 30000, followingCount: 50, postCount: 200 },
}

export const currentUser = {
  userId: '1',
  username: 'testuser',
  displayName: '测试用户',
  bio: '这是测试用户的简介',
  avatar: null,
  followerCount: 120,
  followingCount: 45,
  postCount: 88,
  email: 'test@example.com'
}
```

- [ ] **Step 4: Create mock/posts.js**

```js
export const mockPosts = [
  {
    postId: '1',
    userId: '2',
    username: 'vuejs',
    displayName: 'Vue.js',
    avatar: null,
    shortText: 'Vue 3.4 正式发布！带来了更好的类型支持和性能提升。',
    longText: '',
    photos: [],
    video: null,
    likeCount: 1200,
    commentCount: 340,
    isLiked: false,
    publishTime: '2026-05-26T10:00:00Z'
  },
  {
    postId: '2',
    userId: '3',
    username: 'springboot',
    displayName: 'Spring Boot',
    avatar: null,
    shortText: 'Spring Boot 3.2 带来了虚拟线程支持，大幅提升并发性能。',
    longText: '详细内容：虚拟线程是 Java 21 的重要特性，Spring Boot 3.2 对其提供了原生支持...',
    photos: [],
    video: null,
    likeCount: 800,
    commentCount: 120,
    isLiked: true,
    publishTime: '2026-05-26T09:30:00Z'
  },
  {
    postId: '3',
    userId: '1',
    username: 'testuser',
    displayName: '测试用户',
    avatar: null,
    shortText: '今天学习了 Tailwind CSS，原子化 CSS 真的很方便！',
    longText: '',
    photos: ['https://picsum.photos/400/300'],
    video: null,
    likeCount: 42,
    commentCount: 8,
    isLiked: false,
    publishTime: '2026-05-26T08:15:00Z'
  }
]
```

- [ ] **Step 5: Create mock/feed.js**

```js
import { mockPosts } from './posts'

export function getMockFeed(params) {
  const { type, cursor } = params || {}
  let filteredPosts = [...mockPosts]

  if (type === 'following') {
    filteredPosts = filteredPosts.filter(p => ['2', '3'].includes(p.userId))
  }

  const pageSize = 10
  const start = cursor ? filteredPosts.findIndex(p => p.publishTime < cursor) : 0
  const page = filteredPosts.slice(start, start + pageSize)

  return {
    posts: page,
    cursor: page.length > 0 ? page[page.length - 1].publishTime : null,
    hasMore: start + pageSize < filteredPosts.length
  }
}
```

- [ ] **Step 6: Create mock/auth.js**

```js
import { currentUser } from './users'

export function mockLogin(params) {
  const { username, password } = params
  if (username === 'testuser' && password === '123456') {
    return { token: 'mock-jwt-token-12345' }
  }
  throw new Error('用户名或密码错误')
}

export function mockRegister(params) {
  return { userId: '99', username: params.username }
}

export function mockGetCurrentUser() {
  return currentUser
}
```

- [ ] **Step 7: Create api/auth.js**

```js
import api, { USE_MOCK } from './index'
import { mockLogin, mockRegister, mockGetCurrentUser } from '@/mock/auth'
import { delay, mockResult } from '@/mock'

export async function login(data) {
  if (USE_MOCK) {
    await delay()
    return mockResult(mockLogin(data))
  }
  return api.post('/auth/login', data)
}

export async function register(data) {
  if (USE_MOCK) {
    await delay()
    return mockResult(mockRegister(data))
  }
  return api.post('/auth/register', data)
}

export async function getCurrentUser() {
  if (USE_MOCK) {
    await delay()
    return mockResult(mockGetCurrentUser())
  }
  return api.get('/auth/me')
}
```

- [ ] **Step 8: Create api/feed.js**

```js
import api, { USE_MOCK } from './index'
import { getMockFeed } from '@/mock/feed'
import { delay, mockResult } from '@/mock'

export async function getFeed(params) {
  if (USE_MOCK) {
    await delay()
    return mockResult(getMockFeed(params))
  }
  return api.post('/feed/getFeed', params)
}
```

- [ ] **Step 9: Create api/post.js**

```js
import api, { USE_MOCK } from './index'
import { mockPosts } from '@/mock/posts'
import { delay, mockResult, generateId } from '@/mock'

export async function createPost(data) {
  if (USE_MOCK) {
    await delay()
    const newPost = {
      postId: generateId(),
      userId: '1',
      username: 'testuser',
      displayName: '测试用户',
      avatar: null,
      shortText: data.shortText || '',
      longText: data.longText || '',
      photos: data.photo ? [data.photo] : [],
      video: data.video || null,
      likeCount: 0,
      commentCount: 0,
      isLiked: false,
      publishTime: new Date().toISOString()
    }
    mockPosts.unshift(newPost)
    return mockResult(newPost)
  }
  return api.post('/post', data)
}

export async function toggleLike(postId) {
  if (USE_MOCK) {
    await delay(100)
    const post = mockPosts.find(p => p.postId === postId)
    if (post) {
      post.isLiked = !post.isLiked
      post.likeCount += post.isLiked ? 1 : -1
    }
    return mockResult({ isLiked: post?.isLiked })
  }
  return api.post(`/post/${postId}/like`)
}

export async function getPostDetail(postId) {
  if (USE_MOCK) {
    await delay()
    const post = mockPosts.find(p => p.postId === postId)
    return mockResult(post)
  }
  return api.get(`/post/${postId}`)
}

export async function getComments(postId) {
  if (USE_MOCK) {
    await delay()
    return mockResult([
      { commentId: '1', postId, userId: '2', username: 'vuejs', displayName: 'Vue.js', content: '好文章！', publishTime: '2026-05-26T11:00:00Z' },
      { commentId: '2', postId, userId: '3', username: 'springboot', displayName: 'Spring Boot', content: '收藏了', publishTime: '2026-05-26T11:30:00Z' },
    ])
  }
  return api.get(`/post/${postId}/comments`)
}

export async function addComment(postId, content) {
  if (USE_MOCK) {
    await delay()
    return mockResult({ commentId: generateId(), postId, userId: '1', username: 'testuser', displayName: '测试用户', content, publishTime: new Date().toISOString() })
  }
  return api.post(`/post/${postId}/comment`, { content })
}
```

- [ ] **Step 10: Create api/user.js**

```js
import api, { USE_MOCK } from './index'
import { mockUsers } from '@/mock/users'
import { delay, mockResult } from '@/mock'

export async function getUser(userId) {
  if (USE_MOCK) {
    await delay()
    const user = mockUsers[userId]
    if (!user) throw new Error('用户不存在')
    return mockResult(user)
  }
  return api.get(`/user/${userId}`)
}

export async function followUser(userId) {
  if (USE_MOCK) {
    await delay()
    return mockResult({ isFollowing: true })
  }
  return api.post(`/user/${userId}/follow`)
}

export async function unfollowUser(userId) {
  if (USE_MOCK) {
    await delay()
    return mockResult({ isFollowing: false })
  }
  return api.delete(`/user/${userId}/follow`)
}
```

- [ ] **Step 11: Create api/search.js**

```js
import api, { USE_MOCK } from './index'
import { mockPosts } from '@/mock/posts'
import { mockUsers } from '@/mock/users'
import { delay, mockResult } from '@/mock'

export async function search(query) {
  if (USE_MOCK) {
    await delay()
    const q = query.toLowerCase()
    const users = Object.values(mockUsers).filter(u => u.username.toLowerCase().includes(q) || u.displayName.toLowerCase().includes(q))
    const posts = mockPosts.filter(p => p.shortText.toLowerCase().includes(q))
    return mockResult({ users, posts })
  }
  return api.get('/search', { params: { q: query } })
}
```

- [ ] **Step 12: Commit**

```bash
cd D:/BuzzFeed/Frontend
git add -A
git commit -m "feat: add API layer with mock infrastructure"
```

---

### Task 6: Login/Register Pages

**Files:**
- Modify: `Frontend/src/views/Login.vue`
- Modify: `Frontend/src/views/Register.vue`
- Create: `Frontend/src/composables/useAuth.js`

- [ ] **Step 1: Create composables/useAuth.js**

```js
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ref } from 'vue'

export function useAuth() {
  const authStore = useAuthStore()
  const router = useRouter()
  const loading = ref(false)
  const error = ref('')

  async function login(username, password) {
    error.value = ''
    loading.value = true
    try {
      await authStore.login(username, password)
      router.push('/')
    } catch (e) {
      error.value = e.message || '登录失败'
    } finally {
      loading.value = false
    }
  }

  async function register(username, password, email) {
    error.value = ''
    loading.value = true
    try {
      await authStore.register(username, password, email)
      router.push('/login')
    } catch (e) {
      error.value = e.message || '注册失败'
    } finally {
      loading.value = false
    }
  }

  function logout() {
    authStore.logout()
    router.push('/login')
  }

  return { loading, error, login, register, logout }
}
```

- [ ] **Step 2: Implement Login.vue**

```vue
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { loading, error, login } = useAuth()

const username = ref('')
const password = ref('')

async function handleSubmit() {
  await login(username.value, password.value)
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-bg-primary">
    <div class="w-full max-w-md p-8">
      <div class="text-center mb-8">
        <span class="text-accent text-4xl font-bold">BuzzFeed</span>
        <h1 class="text-2xl font-bold mt-4">登录到你的账号</h1>
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-4">
        <div>
          <input
            v-model="username"
            type="text"
            placeholder="用户名"
            class="w-full bg-bg-secondary border border-border-custom rounded-lg px-4 py-3 text-text-primary placeholder-text-secondary focus:border-accent outline-none transition-colors"
          />
        </div>
        <div>
          <input
            v-model="password"
            type="password"
            placeholder="密码"
            class="w-full bg-bg-secondary border border-border-custom rounded-lg px-4 py-3 text-text-primary placeholder-text-secondary focus:border-accent outline-none transition-colors"
          />
        </div>

        <p v-if="error" class="text-danger text-sm">{{ error }}</p>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-accent text-white py-3 rounded-full font-bold hover:bg-accent/90 transition-colors disabled:opacity-50"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <p class="text-center mt-6 text-text-secondary">
        还没有账号？
        <router-link to="/register" class="text-accent hover:underline">注册</router-link>
      </p>

      <p class="text-center mt-4 text-text-secondary text-sm">
        测试账号：testuser / 123456
      </p>
    </div>
  </div>
</template>
```

- [ ] **Step 3: Implement Register.vue**

```vue
<script setup>
import { ref } from 'vue'
import { useAuth } from '@/composables/useAuth'

const { loading, error, register } = useAuth()

const username = ref('')
const email = ref('')
const password = ref('')

async function handleSubmit() {
  await register(username.value, password.value, email.value)
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-bg-primary">
    <div class="w-full max-w-md p-8">
      <div class="text-center mb-8">
        <span class="text-accent text-4xl font-bold">BuzzFeed</span>
        <h1 class="text-2xl font-bold mt-4">创建你的账号</h1>
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-4">
        <div>
          <input
            v-model="username"
            type="text"
            placeholder="用户名"
            class="w-full bg-bg-secondary border border-border-custom rounded-lg px-4 py-3 text-text-primary placeholder-text-secondary focus:border-accent outline-none transition-colors"
          />
        </div>
        <div>
          <input
            v-model="email"
            type="email"
            placeholder="邮箱"
            class="w-full bg-bg-secondary border border-border-custom rounded-lg px-4 py-3 text-text-primary placeholder-text-secondary focus:border-accent outline-none transition-colors"
          />
        </div>
        <div>
          <input
            v-model="password"
            type="password"
            placeholder="密码"
            class="w-full bg-bg-secondary border border-border-custom rounded-lg px-4 py-3 text-text-primary placeholder-text-secondary focus:border-accent outline-none transition-colors"
          />
        </div>

        <p v-if="error" class="text-danger text-sm">{{ error }}</p>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-accent text-white py-3 rounded-full font-bold hover:bg-accent/90 transition-colors disabled:opacity-50"
        >
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <p class="text-center mt-6 text-text-secondary">
        已有账号？
        <router-link to="/login" class="text-accent hover:underline">登录</router-link>
      </p>
    </div>
  </div>
</template>
```

- [ ] **Step 4: Test login flow**

Run: `cd D:/BuzzFeed/Frontend && npm run dev`
Expected: Login page renders, entering testuser/123456 logs in and redirects to Home

- [ ] **Step 5: Commit**

```bash
cd D:/BuzzFeed/Frontend
git add -A
git commit -m "feat: add login/register pages with auth composable"
```

---

## Phase 2: Feed Core

### Task 7: FeedList + FeedItem Components

**Files:**
- Create: `Frontend/src/components/feed/FeedList.vue`
- Create: `Frontend/src/components/feed/FeedItem.vue`
- Create: `Frontend/src/composables/useFeed.js`

- [ ] **Step 1: Create composables/useFeed.js**

```js
import { useFeedStore } from '@/stores/feed'
import { ref } from 'vue'

export function useFeed() {
  const feedStore = useFeedStore()
  const loading = ref(false)

  async function loadFeed(type = 'recommend') {
    loading.value = true
    try {
      await feedStore.fetchFeed(type)
    } finally {
      loading.value = false
    }
  }

  async function loadMore(type = 'recommend') {
    await feedStore.loadMore(type)
  }

  return { ...feedStore, loading, loadFeed, loadMore }
}
```

- [ ] **Step 2: Create FeedItem.vue**

```vue
<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useFeedStore } from '@/stores/feed'

const props = defineProps({
  post: { type: Object, required: true }
})

const router = useRouter()
const feedStore = useFeedStore()

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

function goToPost() {
  router.push(`/post/${props.post.postId}`)
}

function goToProfile() {
  router.push(`/profile/${props.post.userId}`)
}

async function handleLike() {
  await feedStore.toggleLike(props.post.postId)
}
</script>

<template>
  <article
    class="border-b border-border-custom p-4 hover:bg-bg-hover/50 cursor-pointer transition-colors"
    @click="goToPost"
  >
    <div class="flex gap-3">
      <div
        class="w-10 h-10 rounded-full bg-accent/20 flex-shrink-0 flex items-center justify-center cursor-pointer"
        @click.stop="goToProfile"
      >
        <span class="text-sm font-bold">{{ post.displayName?.charAt(0) }}</span>
      </div>

      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2">
          <span class="font-bold text-text-primary truncate">{{ post.displayName }}</span>
          <span class="text-text-secondary truncate">@{{ post.username }}</span>
          <span class="text-text-secondary">·</span>
          <span class="text-text-secondary text-sm flex-shrink-0">{{ timeAgo }}</span>
        </div>

        <p class="mt-2 text-text-primary whitespace-pre-wrap break-words">{{ post.shortText }}</p>

        <div v-if="post.photos?.length" class="mt-3 grid gap-1 rounded-2xl overflow-hidden" :class="post.photos.length === 1 ? 'grid-cols-1' : 'grid-cols-2'">
          <img
            v-for="(photo, idx) in post.photos.slice(0, 4)"
            :key="idx"
            :src="photo"
            class="w-full h-48 object-cover"
          />
        </div>

        <div class="flex items-center gap-12 mt-3">
          <button class="flex items-center gap-1.5 text-text-secondary hover:text-accent transition-colors group">
            <span class="p-1.5 rounded-full group-hover:bg-accent/10">💬</span>
            <span class="text-sm">{{ post.commentCount }}</span>
          </button>

          <button
            class="flex items-center gap-1.5 transition-colors group"
            :class="post.isLiked ? 'text-danger' : 'text-text-secondary hover:text-danger'"
            @click.stop="handleLike"
          >
            <span class="p-1.5 rounded-full" :class="post.isLiked ? 'bg-danger/10' : 'group-hover:bg-danger/10'">
              {{ post.isLiked ? '❤️' : '🤍' }}
            </span>
            <span class="text-sm">{{ post.likeCount }}</span>
          </button>
        </div>
      </div>
    </div>
  </article>
</template>
```

- [ ] **Step 3: Create FeedList.vue**

```vue
<script setup>
import { onMounted, ref } from 'vue'
import { useFeed } from '@/composables/useFeed'
import FeedItem from './FeedItem.vue'

const props = defineProps({
  type: { type: String, default: 'recommend' }
})

const { posts, followingPosts, loading, hasMore, loadFeed, loadMore } = useFeed()
const sentinel = ref(null)

const currentPosts = computed(() => props.type === 'recommend' ? posts : followingPosts)

onMounted(() => {
  loadFeed(props.type)
})

onMounted(() => {
  const observer = new IntersectionObserver(entries => {
    if (entries[0].isIntersecting && hasMore.value && !loading.value) {
      loadMore(props.type)
    }
  }, { threshold: 0.1 })

  if (sentinel.value) {
    observer.observe(sentinel.value)
  }
})
</script>

<template>
  <div>
    <div v-if="loading && currentPosts.length === 0" class="p-4 space-y-4">
      <div v-for="i in 3" :key="i" class="animate-pulse flex gap-3">
        <div class="w-10 h-10 rounded-full bg-bg-secondary"></div>
        <div class="flex-1 space-y-2">
          <div class="h-4 bg-bg-secondary rounded w-1/3"></div>
          <div class="h-4 bg-bg-secondary rounded w-full"></div>
          <div class="h-4 bg-bg-secondary rounded w-2/3"></div>
        </div>
      </div>
    </div>

    <FeedItem v-for="post in currentPosts" :key="post.postId" :post="post" />

    <div ref="sentinel" class="h-10"></div>

    <div v-if="!hasMore && currentPosts.length > 0" class="p-4 text-center text-text-secondary">
      没有更多了
    </div>

    <div v-if="loading && currentPosts.length > 0" class="p-4 text-center text-text-secondary">
      加载中...
    </div>
  </div>
</template>
```

- [ ] **Step 4: Commit**

```bash
cd D:/BuzzFeed/Frontend
git add -A
git commit -m "feat: add FeedList + FeedItem with infinite scroll"
```

---

### Task 8: Home and Following Pages

**Files:**
- Modify: `Frontend/src/views/Home.vue`
- Modify: `Frontend/src/views/Following.vue`

- [ ] **Step 1: Implement Home.vue**

```vue
<script setup>
import { ref } from 'vue'
import FeedList from '@/components/feed/FeedList.vue'
import PostComposer from '@/components/post/PostComposer.vue'

const activeTab = ref('recommend')
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom">
      <div class="px-4 py-3">
        <h1 class="text-xl font-bold">首页</h1>
      </div>
      <div class="flex">
        <button
          class="flex-1 py-3 text-center font-bold transition-colors relative"
          :class="activeTab === 'recommend' ? 'text-text-primary' : 'text-text-secondary hover:text-text-primary hover:bg-bg-hover'"
          @click="activeTab = 'recommend'"
        >
          推荐
          <div v-if="activeTab === 'recommend'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-14 h-1 bg-accent rounded-full"></div>
        </button>
        <button
          class="flex-1 py-3 text-center font-bold transition-colors relative"
          :class="activeTab === 'following' ? 'text-text-primary' : 'text-text-secondary hover:text-text-primary hover:bg-bg-hover'"
          @click="activeTab = 'following'"
        >
          关注
          <div v-if="activeTab === 'following'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-14 h-1 bg-accent rounded-full"></div>
        </button>
      </div>
    </header>

    <PostComposer />
    <FeedList :type="activeTab" />
  </div>
</template>
```

- [ ] **Step 2: Implement Following.vue**

```vue
<script setup>
import FeedList from '@/components/feed/FeedList.vue'
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom">
      <div class="px-4 py-3">
        <h1 class="text-xl font-bold">关注</h1>
      </div>
    </header>
    <FeedList type="following" />
  </div>
</template>
```

- [ ] **Step 3: Commit**

```bash
cd D:/BuzzFeed/Frontend
git add -A
git commit -m "feat: implement Home and Following pages"
```

---

## Phase 3: Interaction Features

### Task 9: PostComposer Component

**Files:**
- Create: `Frontend/src/components/post/PostComposer.vue`
- Create: `Frontend/src/composables/usePost.js`

- [ ] **Step 1: Create composables/usePost.js**

```js
import { ref } from 'vue'
import { createPost } from '@/api/post'
import { useFeedStore } from '@/stores/feed'

export function usePost() {
  const feedStore = useFeedStore()
  const loading = ref(false)
  const error = ref('')

  async function publishPost(data) {
    error.value = ''
    loading.value = true
    try {
      const res = await createPost(data)
      feedStore.addPost(res.data)
      return res.data
    } catch (e) {
      error.value = e.message || '发布失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  return { loading, error, publishPost }
}
```

- [ ] **Step 2: Create PostComposer.vue**

```vue
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
  await publishPost({ shortText: content.value })
  content.value = ''
  isExpanded.value = false
}
</script>

<template>
  <div class="border-b border-border-custom p-4">
    <div class="flex gap-3">
      <div class="w-10 h-10 rounded-full bg-accent/20 flex-shrink-0 flex items-center justify-center">
        <span class="text-sm font-bold">{{ authStore.currentUser?.displayName?.charAt(0) }}</span>
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
            class="bg-accent text-white px-5 py-1.5 rounded-full font-bold hover:bg-accent/90 transition-colors disabled:opacity-50"
            @click="handleSubmit"
          >
            {{ loading ? '发布中...' : '发布' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 3: Commit**

```bash
cd D:/BuzzFeed/Frontend
git add -A
git commit -m "feat: add PostComposer with character count"
```

---

### Task 10: PostDetail Page + Comments

**Files:**
- Modify: `Frontend/src/views/PostDetail.vue`
- Create: `Frontend/src/components/comment/CommentList.vue`
- Create: `Frontend/src/components/comment/CommentItem.vue`

- [ ] **Step 1: Create CommentItem.vue**

```vue
<script setup>
defineProps({
  comment: { type: Object, required: true }
})

function timeAgo(dateStr) {
  const diff = Date.now() - new Date(dateStr).getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  return `${Math.floor(hours / 24)}天前`
}
</script>

<template>
  <div class="flex gap-3 p-4 border-b border-border-custom">
    <div class="w-8 h-8 rounded-full bg-accent/20 flex-shrink-0 flex items-center justify-center">
      <span class="text-xs font-bold">{{ comment.displayName?.charAt(0) }}</span>
    </div>
    <div class="flex-1">
      <div class="flex items-center gap-2">
        <span class="font-bold text-sm">{{ comment.displayName }}</span>
        <span class="text-text-secondary text-sm">@{{ comment.username }}</span>
        <span class="text-text-secondary text-sm">· {{ timeAgo(comment.publishTime) }}</span>
      </div>
      <p class="mt-1 text-text-primary">{{ comment.content }}</p>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Create CommentList.vue**

```vue
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
```

- [ ] **Step 3: Implement PostDetail.vue**

```vue
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPostDetail, toggleLike } from '@/api/post'
import CommentList from '@/components/comment/CommentList.vue'

const route = useRoute()
const router = useRouter()
const post = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getPostDetail(route.params.postId)
    post.value = res.data
  } finally {
    loading.value = false
  }
})

async function handleLike() {
  const res = await toggleLike(post.value.postId)
  post.value.isLiked = res.data.isLiked
  post.value.likeCount += res.data.isLiked ? 1 : -1
}

function goToProfile() {
  router.push(`/profile/${post.value.userId}`)
}
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom flex items-center gap-6 px-4 py-3">
      <button class="text-xl" @click="router.back()">←</button>
      <h1 class="text-xl font-bold">帖子</h1>
    </header>

    <div v-if="loading" class="p-8 text-center text-text-secondary">加载中...</div>

    <div v-else-if="post">
      <div class="p-4">
        <div class="flex items-center gap-3 mb-4">
          <div
            class="w-12 h-12 rounded-full bg-accent/20 flex items-center justify-center cursor-pointer"
            @click="goToProfile"
          >
            <span class="font-bold">{{ post.displayName?.charAt(0) }}</span>
          </div>
          <div>
            <div class="font-bold">{{ post.displayName }}</div>
            <div class="text-text-secondary">@{{ post.username }}</div>
          </div>
        </div>

        <p class="text-xl whitespace-pre-wrap break-words">{{ post.shortText }}</p>
        <p v-if="post.longText" class="mt-4 text-text-primary whitespace-pre-wrap break-words">{{ post.longText }}</p>

        <div v-if="post.photos?.length" class="mt-4 grid gap-1 rounded-2xl overflow-hidden" :class="post.photos.length === 1 ? 'grid-cols-1' : 'grid-cols-2'">
          <img v-for="(photo, idx) in post.photos" :key="idx" :src="photo" class="w-full h-64 object-cover" />
        </div>

        <div class="flex items-center gap-6 mt-4 py-3 border-t border-b border-border-custom text-text-secondary text-sm">
          <span>{{ post.likeCount }} 赞</span>
          <span>{{ post.commentCount }} 评论</span>
        </div>

        <div class="flex items-center gap-12 py-3">
          <button
            class="flex items-center gap-2 transition-colors"
            :class="post.isLiked ? 'text-danger' : 'text-text-secondary hover:text-danger'"
            @click="handleLike"
          >
            {{ post.isLiked ? '❤️' : '🤍' }}
            <span>{{ post.isLiked ? '已赞' : '赞' }}</span>
          </button>
        </div>
      </div>

      <CommentList :post-id="post.postId" />
    </div>
  </div>
</template>
```

- [ ] **Step 4: Commit**

```bash
cd D:/BuzzFeed/Frontend
git add -A
git commit -m "feat: add PostDetail page with comments"
```

---

## Phase 4: Social Features

### Task 11: Profile Page

**Files:**
- Modify: `Frontend/src/views/Profile.vue`
- Create: `Frontend/src/components/user/UserAvatar.vue`
- Create: `Frontend/src/components/user/FollowButton.vue`

- [ ] **Step 1: Create UserAvatar.vue**

```vue
<script setup>
defineProps({
  username: { type: String, default: '' },
  size: { type: String, default: 'md' }
})

const sizeClasses = {
  sm: 'w-8 h-8 text-xs',
  md: 'w-10 h-10 text-sm',
  lg: 'w-16 h-16 text-xl',
  xl: 'w-24 h-24 text-3xl'
}
</script>

<template>
  <div
    class="rounded-full bg-accent/20 flex items-center justify-center font-bold flex-shrink-0"
    :class="sizeClasses[size] || sizeClasses.md"
  >
    {{ username?.charAt(0)?.toUpperCase() }}
  </div>
</template>
```

- [ ] **Step 2: Create FollowButton.vue**

```vue
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
```

- [ ] **Step 3: Implement Profile.vue**

```vue
<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getUser } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useFeed } from '@/composables/useFeed'
import UserAvatar from '@/components/user/UserAvatar.vue'
import FollowButton from '@/components/user/FollowButton.vue'
import FeedItem from '@/components/feed/FeedItem.vue'

const route = useRoute()
const authStore = useAuthStore()
const user = ref(null)
const loading = ref(true)
const activeTab = ref('posts')

const { posts, loadFeed } = useFeed()

const isSelf = computed(() => authStore.currentUser?.userId === route.params.userId)

onMounted(async () => {
  try {
    const res = await getUser(route.params.userId)
    user.value = res.data
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom flex items-center gap-6 px-4 py-3">
      <button class="text-xl" @click="$router.back()">←</button>
      <div>
        <h1 class="text-xl font-bold">{{ user?.displayName }}</h1>
        <span class="text-text-secondary text-sm">{{ user?.postCount }} 条帖子</span>
      </div>
    </header>

    <div v-if="loading" class="p-8 text-center text-text-secondary">加载中...</div>

    <div v-else-if="user">
      <div class="h-48 bg-bg-secondary"></div>

      <div class="px-4 pb-4">
        <div class="flex justify-between items-end -mt-12 mb-3">
          <UserAvatar :username="user.username" size="xl" />
          <FollowButton
            v-if="!isSelf"
            :user-id="user.userId"
            :initial-following="false"
          />
        </div>

        <h2 class="text-xl font-bold">{{ user.displayName }}</h2>
        <p class="text-text-secondary">@{{ user.username }}</p>
        <p v-if="user.bio" class="mt-2 text-text-primary">{{ user.bio }}</p>

        <div class="flex gap-4 mt-3 text-sm">
          <span><strong>{{ user.followingCount }}</strong> <span class="text-text-secondary">关注</span></span>
          <span><strong>{{ user.followerCount }}</strong> <span class="text-text-secondary">粉丝</span></span>
        </div>
      </div>

      <div class="flex border-b border-border-custom">
        <button
          class="flex-1 py-3 text-center font-bold relative"
          :class="activeTab === 'posts' ? 'text-text-primary' : 'text-text-secondary'"
          @click="activeTab = 'posts'"
        >
          帖子
          <div v-if="activeTab === 'posts'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-14 h-1 bg-accent rounded-full"></div>
        </button>
        <button
          class="flex-1 py-3 text-center font-bold relative"
          :class="activeTab === 'media' ? 'text-text-primary' : 'text-text-secondary'"
          @click="activeTab = 'media'"
        >
          媒体
          <div v-if="activeTab === 'media'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-14 h-1 bg-accent rounded-full"></div>
        </button>
        <button
          class="flex-1 py-3 text-center font-bold relative"
          :class="activeTab === 'likes' ? 'text-text-primary' : 'text-text-secondary'"
          @click="activeTab = 'likes'"
        >
          喜欢
          <div v-if="activeTab === 'likes'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-14 h-1 bg-accent rounded-full"></div>
        </button>
      </div>

      <FeedItem v-for="post in posts" :key="post.postId" :post="post" />
    </div>
  </div>
</template>
```

- [ ] **Step 4: Commit**

```bash
cd D:/BuzzFeed/Frontend
git add -A
git commit -m "feat: add Profile page with FollowButton and UserAvatar"
```

---

### Task 12: Explore Search Page

**Files:**
- Modify: `Frontend/src/views/Explore.vue`

- [ ] **Step 1: Implement Explore.vue**

```vue
<script setup>
import { ref } from 'vue'
import { search } from '@/api/search'
import FeedItem from '@/components/feed/FeedItem.vue'
import UserAvatar from '@/components/user/UserAvatar.vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const query = ref('')
const results = ref(null)
const loading = ref(false)

async function handleSearch() {
  if (!query.value.trim()) return
  loading.value = true
  try {
    const res = await search(query.value)
    results.value = res.data
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom px-4 py-3">
      <div class="relative">
        <span class="absolute left-4 top-1/2 -translate-y-1/2 text-text-secondary">🔍</span>
        <input
          v-model="query"
          type="text"
          placeholder="搜索用户或帖子"
          class="w-full bg-bg-secondary border border-transparent focus:border-accent rounded-full py-3 pl-12 pr-4 text-text-primary placeholder-text-secondary outline-none transition-colors"
          @keyup.enter="handleSearch"
        />
      </div>
    </header>

    <div v-if="loading" class="p-4 text-center text-text-secondary">搜索中...</div>

    <div v-else-if="results">
      <div v-if="results.users?.length" class="border-b border-border-custom">
        <h3 class="px-4 py-3 font-bold text-lg">用户</h3>
        <div
          v-for="user in results.users"
          :key="user.userId"
          class="flex items-center gap-3 px-4 py-3 hover:bg-bg-hover cursor-pointer transition-colors"
          @click="router.push(`/profile/${user.userId}`)"
        >
          <UserAvatar :username="user.username" />
          <div>
            <div class="font-bold">{{ user.displayName }}</div>
            <div class="text-text-secondary">@{{ user.username }}</div>
          </div>
        </div>
      </div>

      <div v-if="results.posts?.length">
        <h3 class="px-4 py-3 font-bold text-lg">帖子</h3>
        <FeedItem v-for="post in results.posts" :key="post.postId" :post="post" />
      </div>

      <div v-if="!results.users?.length && !results.posts?.length" class="p-8 text-center text-text-secondary">
        没有找到相关结果
      </div>
    </div>

    <div v-else class="p-8 text-center text-text-secondary">
      搜索你感兴趣的内容
    </div>
  </div>
</template>
```

- [ ] **Step 2: Commit**

```bash
cd D:/BuzzFeed/Frontend
git add -A
git commit -m "feat: add Explore search page"
```

---

### Task 13: Notifications and Messages Pages

**Files:**
- Modify: `Frontend/src/views/Notifications.vue`
- Modify: `Frontend/src/views/Messages.vue`

- [ ] **Step 1: Implement Notifications.vue**

```vue
<script setup>
import { ref, onMounted } from 'vue'

const notifications = ref([])
const loading = ref(true)

onMounted(() => {
  setTimeout(() => {
    notifications.value = [
      { id: 1, type: 'like', username: 'vuejs', displayName: 'Vue.js', content: '赞了你的帖子', time: '2小时前', read: false },
      { id: 2, type: 'comment', username: 'springboot', displayName: 'Spring Boot', content: '评论了你的帖子：好文章！', time: '3小时前', read: true },
      { id: 3, type: 'follow', username: 'tailwindcss', displayName: 'Tailwind CSS', content: '关注了你', time: '1天前', read: true },
    ]
    loading.value = false
  }, 500)
})
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom px-4 py-3">
      <h1 class="text-xl font-bold">通知</h1>
    </header>

    <div v-if="loading" class="p-4 text-center text-text-secondary">加载中...</div>

    <div v-else>
      <div
        v-for="notif in notifications"
        :key="notif.id"
        class="flex gap-3 p-4 border-b border-border-custom hover:bg-bg-hover transition-colors cursor-pointer"
        :class="{ 'bg-accent/5': !notif.read }"
      >
        <div class="w-10 h-10 rounded-full bg-accent/20 flex-shrink-0 flex items-center justify-center">
          <span class="text-sm">{{ notif.type === 'like' ? '❤️' : notif.type === 'comment' ? '💬' : '👤' }}</span>
        </div>
        <div class="flex-1">
          <p>
            <strong>{{ notif.displayName }}</strong>
            <span class="text-text-secondary ml-1">{{ notif.content }}</span>
          </p>
          <span class="text-text-secondary text-sm">{{ notif.time }}</span>
        </div>
      </div>

      <div v-if="notifications.length === 0" class="p-8 text-center text-text-secondary">
        暂无通知
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Implement Messages.vue**

```vue
<script setup>
import { ref, onMounted } from 'vue'

const conversations = ref([])
const loading = ref(true)

onMounted(() => {
  setTimeout(() => {
    conversations.value = [
      { id: 1, username: 'vuejs', displayName: 'Vue.js', lastMessage: '你好！欢迎使用 Vue 3', time: '10:30', unread: 2 },
      { id: 2, username: 'springboot', displayName: 'Spring Boot', lastMessage: '新的 API 文档已经更新', time: '昨天', unread: 0 },
    ]
    loading.value = false
  }, 500)
})
</script>

<template>
  <div>
    <header class="sticky top-0 z-10 bg-bg-primary/80 backdrop-blur-md border-b border-border-custom px-4 py-3">
      <h1 class="text-xl font-bold">消息</h1>
    </header>

    <div v-if="loading" class="p-4 text-center text-text-secondary">加载中...</div>

    <div v-else>
      <div
        v-for="conv in conversations"
        :key="conv.id"
        class="flex gap-3 p-4 border-b border-border-custom hover:bg-bg-hover transition-colors cursor-pointer"
      >
        <div class="w-12 h-12 rounded-full bg-accent/20 flex-shrink-0 flex items-center justify-center">
          <span class="font-bold">{{ conv.displayName.charAt(0) }}</span>
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex justify-between items-center">
            <span class="font-bold">{{ conv.displayName }}</span>
            <span class="text-text-secondary text-sm">{{ conv.time }}</span>
          </div>
          <p class="text-text-secondary text-sm truncate">{{ conv.lastMessage }}</p>
        </div>
        <div v-if="conv.unread" class="w-5 h-5 rounded-full bg-accent flex items-center justify-center flex-shrink-0">
          <span class="text-white text-xs">{{ conv.unread }}</span>
        </div>
      </div>

      <div v-if="conversations.length === 0" class="p-8 text-center text-text-secondary">
        暂无消息
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 3: Commit**

```bash
cd D:/BuzzFeed/Frontend
git add -A
git commit -m "feat: add Notifications and Messages pages"
```

---

## Final Verification

- [ ] Run `cd D:/BuzzFeed/Frontend && npm run dev` and verify:
  - Login works with testuser/123456
  - Home feed loads with mock posts
  - Infinite scroll works
  - Post composer works
  - Post detail + comments work
  - Profile page loads
  - Search works
  - Notifications and Messages pages render
  - All navigation links work
  - Dark theme is consistent
