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
