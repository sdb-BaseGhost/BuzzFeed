import api, { USE_MOCK } from './index'
import { mockPosts } from '@/mock/posts'
import { delay, mockResult, generateId } from '@/mock'

/**
 * 发布内容（multipart/form-data，文件和元数据一起提交）
 * @param {Object} params
 * @param {number}  params.contentType  1=图文 2=视频
 * @param {string}  params.title
 * @param {string}  [params.description]
 * @param {number}  [params.visibility=1]
 * @param {File[]}  [params.images]
 * @param {File}    [params.video]
 */
export async function createPost({ contentType, title, description, visibility, images, video }) {
  const formData = new FormData()
  formData.append('contentType', contentType)
  formData.append('title', title)
  if (description) formData.append('description', description)
  if (visibility !== undefined) formData.append('visibility', visibility)
  if (images && images.length > 0) {
    images.forEach(file => formData.append('images', file))
  }
  if (video) formData.append('video', video)

  return api.post('/post', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
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
