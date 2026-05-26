import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const showPostComposer = ref(false)
  const showLoginModal = ref(false)

  return { showPostComposer, showLoginModal }
})
