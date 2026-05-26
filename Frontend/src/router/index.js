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
