import api, { USE_MOCK } from './index'
import { mockPosts } from '@/mock/posts'
import { delay, mockResult, generateId } from '@/mock'

/**
 * 发布内容（JSON 提交，文件需先通过 /upload 接口上传拿到 URL）
 * @param {Object}   params
 * @param {number}   params.contentType   0=纯文本 1=图文 2=视频
 * @param {string}   params.title
 * @param {string}   [params.description]
 * @param {number}   [params.visibility=1]
 * @param {string[]} [params.imageUrls]   图文类型：已上传的图片 URL 列表
 * @param {string}   [params.videoUrl]    视频类型：已上传的视频 URL
 */
export async function createPost({ contentType, title, description, visibility, imageUrls, videoUrl, coverUrl }) {
  return api.post('/post', {
    contentType,
    title,
    description,
    visibility,
    imageUrls,
    videoUrl,
    coverUrl
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

/**
 * 获取指定用户的帖子列表
 * @param {number} userId
 * @param {number} [limit=20]
 */
export async function getUserPosts(userId, limit = 20) {
  if (USE_MOCK) {
    await delay()
    const userPosts = mockPosts.filter(p => String(p.creatorId) === String(userId))
    return mockResult(userPosts.slice(0, limit))
  }
  return api.get(`/api/post/user/${userId}`, { params: { limit } })
}
