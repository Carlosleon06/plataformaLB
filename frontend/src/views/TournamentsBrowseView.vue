<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useTournamentsStore, type Tournament } from '../stores/tournaments'

const auth = useAuthStore()
const tournaments = useTournamentsStore()
const items = ref<Tournament[]>([])
const localError = ref<string | null>(null)

onMounted(async () => {
  localError.value = null
  try {
    items.value = await tournaments.listTournaments()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
})

function fmt(iso: string) {
  return new Date(iso).toLocaleString()
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">Torneos</h1>
        <p class="mt-2 max-w-2xl text-sm text-zinc-400">
          Torneos con inscripción abierta. Abre uno para ver fechas e inscribirte.
        </p>
      </div>
      <RouterLink
        v-if="auth.me?.role === 'ADMIN'"
        class="shrink-0 rounded-md border border-amber-900/50 bg-amber-950/30 px-3 py-2 text-sm text-amber-100 hover:bg-amber-950/50"
        to="/admin/tournaments/create"
      >
        Crear torneo
      </RouterLink>
    </div>

    <p v-if="localError" class="text-sm text-rose-300">{{ localError }}</p>

    <div v-if="items.length === 0 && !localError" class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-6 text-sm text-zinc-400">
      No hay torneos con inscripción abierta por ahora.
    </div>

    <div v-if="items.length > 0" class="overflow-hidden rounded-xl border border-zinc-800">
      <table class="w-full text-left text-sm">
        <thead class="bg-zinc-950 text-xs text-zinc-400">
          <tr>
            <th class="px-4 py-2">Nombre</th>
            <th class="px-4 py-2">Juego</th>
            <th class="hidden px-4 py-2 sm:table-cell">Inscripción hasta</th>
            <th class="hidden px-4 py-2 md:table-cell">Competencia</th>
            <th class="px-4 py-2 text-right"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in items" :key="t.id" class="border-t border-zinc-800 bg-zinc-950/40">
            <td class="px-4 py-3 text-zinc-100">{{ t.name }}</td>
            <td class="px-4 py-3 font-mono text-xs text-zinc-300">{{ t.game }}</td>
            <td class="hidden px-4 py-3 text-xs text-zinc-400 sm:table-cell">{{ fmt(t.registrationEndAt) }}</td>
            <td class="hidden px-4 py-3 text-xs text-zinc-400 md:table-cell">
              {{ fmt(t.competitionStartAt) }} — {{ fmt(t.competitionEndAt) }}
            </td>
            <td class="px-4 py-3 text-right">
              <RouterLink
                class="rounded-md border border-zinc-700 px-2 py-1 text-xs text-zinc-200 hover:bg-zinc-900"
                :to="`/tournaments/${t.id}`"
              >
                Ver
              </RouterLink>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
