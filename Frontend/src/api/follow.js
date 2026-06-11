import api from './index'

/** 关注用户 */
export function followUser(toUserId) {
  return api.post('/api/follow', { toUserId })
}

/** 取消关注 */
export function unfollowUser(toUserId) {
  return api.delete(`/api/follow/${toUserId}`)
}

/** 查询关注列表 */
export function getFollowingList(userId, size = 20) {
  return api.get(`/api/follow/following/${userId}`, { params: { size } })
}

/** 查询粉丝列表 */
export function getFollowerList(userId, size = 20) {
  return api.get(`/api/follow/followers/${userId}`, { params: { size } })
}

/** 查询关注关系状态 */
export function getFollowStatus(targetUserId) {
  return api.get(`/api/follow/status/${targetUserId}`)
}
