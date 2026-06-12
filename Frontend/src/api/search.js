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
  return api.get('/api/search', { params: { q: query } })
}
