import { currentUser } from './users'

export function mockLogin(params) {
  const { username, password } = params
  if (username === 'testuser' && password === '123456') {
    return { token: 'mock-jwt-token-12345' }
  }
  throw new Error('用户名或密码错误')
}

export function mockRegister(params) {
  return { userId: '99', username: params.username }
}

export function mockGetCurrentUser() {
  return currentUser
}
