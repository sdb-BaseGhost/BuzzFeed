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
