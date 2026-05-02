<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')

async function submit() {
  await auth.login({ username: username.value, password: password.value })
  await router.push('/')
}
</script>

<template>
  <div class="mx-auto max-w-md space-y-4">
    <div>
      <h1 class="text-xl font-semibold">Entrar</h1>
      <p class="mt-2 text-sm text-zinc-400">Usuario + contraseña.</p>
    </div>

    <form class="space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/40 p-5" @submit.prevent="submit">
      <label class="block text-sm">
        <span class="text-zinc-400">Usuario</span>
        <input
          v-model="username"
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm outline-none ring-0 focus:border-zinc-600"
          autocomplete="username"
          required
        />
      </label>

      <label class="block text-sm">
        <span class="text-zinc-400">Contraseña</span>
        <input
          v-model="password"
          type="password"
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm outline-none ring-0 focus:border-zinc-600"
          autocomplete="current-password"
          required
        />
      </label>

      <button
        type="submit"
        class="w-full rounded-md bg-white px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-zinc-200 disabled:opacity-50"
        :disabled="auth.busy"
      >
        Continuar
      </button>

      <p v-if="auth.error" class="text-sm text-rose-300">{{ auth.error }}</p>
    </form>
  </div>
</template>
