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
