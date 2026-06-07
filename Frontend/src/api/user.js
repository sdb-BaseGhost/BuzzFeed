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
  return api.get(`/api/user/${userId}`)
}

export async function getFollowing(userId) {
  if (USE_MOCK) {
    await delay()
    const list = Object.values(mockUsers).filter(u => u.userId !== String(userId))
    return mockResult(list)
  }
  return api.get(`/api/user/${userId}/following`)
}

export async function getFollowers(userId) {
  if (USE_MOCK) {
    await delay()
    const list = Object.values(mockUsers).filter(u => u.userId !== String(userId))
    return mockResult(list)
  }
  return api.get(`/api/user/${userId}/followers`)
}

export async function followUser(userId) {
  if (USE_MOCK) {
    await delay()
    return mockResult({ isFollowing: true })
  }
  return api.post(`/api/user/${userId}/follow`)
}

export async function unfollowUser(userId) {
  if (USE_MOCK) {
    await delay()
    return mockResult({ isFollowing: false })
  }
  return api.delete(`/api/user/${userId}/follow`)
}
