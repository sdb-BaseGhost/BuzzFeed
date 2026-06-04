<script setup>
import { ref } from 'vue'
import { useAuth } from '@/composables/useAuth'

const { loading, error, register } = useAuth()

const username = ref('')
const email = ref('')
const displayName = ref('')
const password = ref('')

async function handleSubmit() {
  await register(username.value, password.value, email.value, displayName.value)
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-bg-primary">
    <div class="w-full max-w-md p-8">
      <div class="text-center mb-8">
        <span class="text-accent text-4xl font-bold">BuzzFeed</span>
        <h1 class="text-2xl font-bold mt-4">创建你的账号</h1>
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-4">
        <div>
          <input
            v-model="username"
            type="text"
            placeholder="用户名 *"
            required
            class="w-full bg-bg-secondary border border-border-custom rounded-lg px-4 py-3 text-text-primary placeholder-text-secondary focus:border-accent outline-none transition-colors"
          />
        </div>
        <div>
          <input
            v-model="displayName"
            type="text"
            placeholder="显示名称（可选）"
            class="w-full bg-bg-secondary border border-border-custom rounded-lg px-4 py-3 text-text-primary placeholder-text-secondary focus:border-accent outline-none transition-colors"
          />
        </div>
        <div>
          <input
            v-model="email"
            type="email"
            placeholder="邮箱（可选）"
            class="w-full bg-bg-secondary border border-border-custom rounded-lg px-4 py-3 text-text-primary placeholder-text-secondary focus:border-accent outline-none transition-colors"
          />
        </div>
        <div>
          <input
            v-model="password"
            type="password"
            placeholder="密码（至少6位）"
            required
            minlength="6"
            class="w-full bg-bg-secondary border border-border-custom rounded-lg px-4 py-3 text-text-primary placeholder-text-secondary focus:border-accent outline-none transition-colors"
          />
        </div>

        <p v-if="error" class="text-danger text-sm">{{ error }}</p>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-accent text-white py-3 rounded-full font-bold hover:bg-accent/90 transition-colors disabled:opacity-50"
        >
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <p class="text-center mt-6 text-text-secondary">
        已有账号？
        <router-link to="/login" class="text-accent hover:underline">登录</router-link>
      </p>
    </div>
  </div>
</template>
