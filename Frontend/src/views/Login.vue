<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { loading, error, login } = useAuth()

const username = ref('')
const password = ref('')

async function handleSubmit() {
  await login(username.value, password.value)
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-bg-primary">
    <div class="w-full max-w-md p-8">
      <div class="text-center mb-8">
        <span class="text-accent text-4xl font-bold">BuzzFeed</span>
        <h1 class="text-2xl font-bold mt-4">登录到你的账号</h1>
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-4">
        <div>
          <input
            v-model="username"
            type="text"
            placeholder="用户名"
            class="w-full bg-bg-secondary border border-border-custom rounded-lg px-4 py-3 text-text-primary placeholder-text-secondary focus:border-accent outline-none transition-colors"
          />
        </div>
        <div>
          <input
            v-model="password"
            type="password"
            placeholder="密码"
            class="w-full bg-bg-secondary border border-border-custom rounded-lg px-4 py-3 text-text-primary placeholder-text-secondary focus:border-accent outline-none transition-colors"
          />
        </div>

        <p v-if="error" class="text-danger text-sm">{{ error }}</p>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-accent text-white py-3 rounded-full font-bold hover:bg-accent/90 transition-colors disabled:opacity-50"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <p class="text-center mt-6 text-text-secondary">
        还没有账号？
        <router-link to="/register" class="text-accent hover:underline">注册</router-link>
      </p>

      <p class="text-center mt-4 text-text-secondary text-sm">
        测试账号：testuser / 123456
      </p>
    </div>
  </div>
</template>
