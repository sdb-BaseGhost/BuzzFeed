import { useFeedStore } from '@/stores/feed'

export function useFeed() {
  const feedStore = useFeedStore()

  return { ...feedStore }
}
