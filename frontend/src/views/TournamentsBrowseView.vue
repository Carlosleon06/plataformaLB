<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useTournamentsStore, type Tournament } from '../stores/tournaments'

const auth = useAuthStore()
const tournaments = useTournamentsStore()
const items = ref<Tournament[]>([])
const localError = ref<string | null>(null)

/** MVP pulido: filtro solo en cliente; el API devuelve hasta 80 torneos (todos los estados). */
const filter = ref<'all' | 'open'>('all')

const filteredItems = computed(() => {
  if (filter.value === 'open') {
    return items.value.filter((t) => t.lifecycleStatus === 'REGISTRATION_OPEN')
  }
  return items.value
})

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

function lifecycleLabel(s: string): string {
  switch (s) {
    case 'REGISTRATION_OPEN':
      return 'Inscripción abierta'
    case 'REGISTRATION_CLOSED':
      return 'Inscripción cerrada'
    case 'LIVE':
      return 'En curso'
    case 'COMPLETED':
      return 'Finalizado'
    default:
      return s
  }
}

function lifecycleBadgeClass(s: string): string {
  switch (s) {
    case 'REGISTRATION_OPEN':
      return 'border-emerald-800/60 bg-emerald-950/40 text-emerald-200'
    case 'REGISTRATION_CLOSED':
      return 'border-amber-800/50 bg-amber-950/30 text-amber-100'
    case 'LIVE':
      return 'border-sky-800/50 bg-sky-950/40 text-sky-200'
    case 'COMPLETED':
      return 'border-zinc-700 bg-zinc-900 text-zinc-400'
    default:
      return 'border-zinc-700 bg-zinc-900 text-zinc-400'
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">Torneos</h1>
        <p class="mt-2 max-w-2xl text-sm text-zinc-400">
          Lista pública (hasta 80 torneos): inscripción abierta, en curso o ya finalizados. Usa el filtro para ver solo los que
          aceptan inscripciones.
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

    <div v-if="items.length > 0" class="flex flex-wrap gap-2">
      <button
        type="button"
        class="rounded-md border px-3 py-1.5 text-xs"
        :class="
          filter === 'all'
            ? 'border-zinc-500 bg-zinc-800 text-zinc-100'
            : 'border-zinc-800 bg-zinc-950 text-zinc-400 hover:border-zinc-600'
        "
        @click="filter = 'all'"
      >
        Todos ({{ items.length }})
      </button>
      <button
        type="button"
        class="rounded-md border px-3 py-1.5 text-xs"
        :class="
          filter === 'open'
            ? 'border-zinc-500 bg-zinc-800 text-zinc-100'
            : 'border-zinc-800 bg-zinc-950 text-zinc-400 hover:border-zinc-600'
        "
        @click="filter = 'open'"
      >
        Inscripción abierta ({{ items.filter((x) => x.lifecycleStatus === 'REGISTRATION_OPEN').length }})
      </button>
    </div>

    <p v-if="localError" class="text-sm text-rose-300">{{ localError }}</p>

    <div v-if="items.length === 0 && !localError" class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-6 text-sm text-zinc-400">
      Aún no hay torneos publicados.
    </div>

    <div
      v-else-if="filteredItems.length === 0 && !localError"
      class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-6 text-sm text-zinc-400"
    >
      Ningún torneo coincide con este filtro.
    </div>

    <div v-else-if="filteredItems.length > 0" class="overflow-hidden rounded-xl border border-zinc-800">
      <table class="w-full text-left text-sm">
        <thead class="bg-zinc-950 text-xs text-zinc-400">
          <tr>
            <th class="px-4 py-2">Nombre</th>
            <th class="px-4 py-2">Estado</th>
            <th class="px-4 py-2">Juego</th>
            <th class="hidden px-4 py-2 sm:table-cell">Inscripción hasta</th>
            <th class="hidden px-4 py-2 md:table-cell">Competencia</th>
            <th class="px-4 py-2 text-right"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in filteredItems" :key="t.id" class="border-t border-zinc-800 bg-zinc-950/40">
            <td class="px-4 py-3 text-zinc-100">{{ t.name }}</td>
            <td class="px-4 py-3">
              <span
                class="inline-block rounded border px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide"
                :class="lifecycleBadgeClass(t.lifecycleStatus)"
              >
                {{ lifecycleLabel(t.lifecycleStatus) }}
              </span>
            </td>
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
