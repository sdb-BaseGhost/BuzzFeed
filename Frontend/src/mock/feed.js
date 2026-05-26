import { mockPosts } from './posts'

export function getMockFeed(params) {
  const { type, cursor } = params || {}
  let filteredPosts = [...mockPosts]

  if (type === 'following') {
    filteredPosts = filteredPosts.filter(p => ['2', '3'].includes(p.userId))
  }

  const pageSize = 10
  const start = cursor ? filteredPosts.findIndex(p => p.publishTime < cursor) : 0
  const page = filteredPosts.slice(start, start + pageSize)

  return {
    posts: page,
    cursor: page.length > 0 ? page[page.length - 1].publishTime : null,
    hasMore: start + pageSize < filteredPosts.length
  }
}
