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
