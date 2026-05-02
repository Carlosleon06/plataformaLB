<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useTeamsStore } from '../stores/teams'

const router = useRouter()
const auth = useAuthStore()
const teams = useTeamsStore()

const name = ref('')
const tag = ref('')
const regionServer = ref('')
const sponsorLinesDraft = ref('')
const streamCanonical = ref('')

async function submit() {
  if (!auth.token) {
    await router.push('/login')
    return
  }
  const sponsorLines = sponsorLinesDraft.value
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
    .slice(0, 15)
  const stream = streamCanonical.value.trim()

  const created = await teams.createTeam(auth.token, {
    name: name.value,
    tag: tag.value,
    regionServer: regionServer.value,
    sponsorLines,
    ...(stream ? { canonicalStreamUrl: stream } : {}),
  })
  await router.push(`/teams/${created.id}`)
}
</script>

<template>
  <div class="mx-auto max-w-md space-y-4">
    <div>
      <h1 class="text-xl font-semibold">Crear equipo</h1>
      <p class="mt-2 text-sm text-zinc-400">Queda en estado PENDING hasta que un ADMIN lo apruebe.</p>
    </div>

    <form class="space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/40 p-5" @submit.prevent="submit">
      <label class="block text-sm">
        <span class="text-zinc-400">Nombre</span>
        <input
          v-model="name"
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm outline-none ring-0 focus:border-zinc-600"
          required
        />
      </label>

      <label class="block text-sm">
        <span class="text-zinc-400">Siglas</span>
        <input
          v-model="tag"
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm outline-none ring-0 focus:border-zinc-600"
          required
        />
      </label>

      <label class="block text-sm">
        <span class="text-zinc-400">Patrocinio (opcional, una línea por sponsor)</span>
        <textarea
          v-model="sponsorLinesDraft"
          rows="3"
          class="mt-1 w-full resize-y rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs outline-none focus:border-zinc-600"
        />
      </label>

      <label class="block text-sm">
        <span class="text-zinc-400">Stream oficial (opcional)</span>
        <input v-model="streamCanonical" class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm outline-none ring-0 focus:border-zinc-600" />
      </label>

      <label class="block text-sm">
        <span class="text-zinc-400">Región / servidor</span>
        <input
          v-model="regionServer"
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm outline-none ring-0 focus:border-zinc-600"
          required
        />
      </label>

      <button
        type="submit"
        class="w-full rounded-md bg-white px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-zinc-200 disabled:opacity-50"
        :disabled="teams.busy"
      >
        Crear
      </button>

      <p v-if="teams.error" class="text-sm text-rose-300">{{ teams.error }}</p>
    </form>
  </div>
</template>
