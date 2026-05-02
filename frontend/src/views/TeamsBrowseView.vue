<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useTeamsStore } from '../stores/teams'

const auth = useAuthStore()
const teams = useTeamsStore()

onMounted(async () => {
  try {
    await teams.loadPublicTeams(auth.token)
  } catch {
    // errors surfaced via teams.error
  }
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">Equipos</h1>
        <p class="mt-2 max-w-2xl text-sm text-zinc-400">Listado público (solo equipos aprobados).</p>
      </div>

      <div class="flex flex-wrap gap-2">
        <button
          type="button"
          class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 hover:bg-zinc-900 disabled:opacity-50"
          :disabled="teams.busy"
          @click="teams.loadPublicTeams(auth.token)"
        >
          Refrescar
        </button>
        <RouterLink
          v-if="auth.isAuthed"
          class="rounded-md bg-white px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-zinc-200"
          to="/teams/create"
        >
          Crear equipo
        </RouterLink>
      </div>
    </div>

    <p v-if="teams.error" class="text-sm text-rose-300">{{ teams.error }}</p>

    <div class="grid gap-3 md:grid-cols-2">
      <RouterLink
        v-for="t in teams.publicTeams"
        :key="t.id"
        :to="`/teams/${t.id}`"
        class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-4 hover:bg-zinc-900/60"
      >
        <div class="flex items-start justify-between gap-3">
          <div>
            <div class="text-sm font-semibold text-zinc-100">{{ t.name }}</div>
            <div class="mt-1 text-xs text-zinc-400">{{ t.regionServer }} · {{ t.memberCount }} miembros</div>
          </div>
          <div class="rounded-md border border-zinc-800 bg-zinc-950 px-2 py-1 text-xs font-mono text-zinc-200">
            {{ t.tag }}
          </div>
        </div>
      </RouterLink>

      <div v-if="!teams.busy && teams.publicTeams.length === 0" class="text-sm text-zinc-500">
        No hay equipos aprobados todavía.
      </div>
    </div>
  </div>
</template>
