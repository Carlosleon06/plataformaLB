<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { onMounted } from 'vue'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()

onMounted(() => {
  void auth.bootstrap()
})
</script>

<template>
  <div class="min-h-full">
    <header class="border-b border-zinc-800 bg-zinc-950/80 backdrop-blur">
      <div class="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-3">
        <RouterLink to="/" class="text-sm font-semibold tracking-wide text-zinc-100">
          LEON BON
        </RouterLink>

        <nav class="flex items-center gap-3 text-sm">
          <template v-if="auth.isAuthed">
            <div class="hidden text-zinc-300 sm:block">
              <span class="text-zinc-500">L-Coins</span>
              <span class="ml-2 font-mono">{{ auth.me?.leonCoinsBalance ?? '—' }}</span>
            </div>
            <button
              type="button"
              class="rounded-md border border-zinc-800 bg-zinc-900 px-3 py-1.5 text-zinc-100 hover:bg-zinc-800"
              @click="auth.logout()"
            >
              Salir
            </button>
          </template>
          <template v-else>
            <RouterLink class="text-zinc-300 hover:text-white" to="/login">Entrar</RouterLink>
            <RouterLink
              class="rounded-md bg-white px-3 py-1.5 text-sm font-medium text-zinc-950 hover:bg-zinc-200"
              to="/register"
            >
              Registro
            </RouterLink>
          </template>
        </nav>
      </div>
    </header>

    <main class="mx-auto max-w-5xl px-4 py-8">
      <RouterView />
    </main>
  </div>
</template>
