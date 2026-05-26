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
