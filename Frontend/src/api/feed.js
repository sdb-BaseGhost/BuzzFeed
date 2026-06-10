import api from './index'

/**
 * 获取关注 Feed 流
 * @param {Object} params
 * @param {number}  params.userId      当前用户ID
 * @param {number}  params.type        0=下拉刷新, 1=上滑加载
 * @param {string}  [params.lastTime]  游标：最后一条的 publishTime
 * @param {number}  [params.contentId] 游标：最后一条的 itemId
 * @param {number}  [params.num=5]     每次拉取条数
 */
export async function getFeed(params) {
  return api.post('/feed/getFeed', params)
}
