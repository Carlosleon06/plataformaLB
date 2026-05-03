<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { coerceBearerToken } from '../lib/api'
import { formatDateTimeShort } from '../lib/format'
import { LEONBON_TOKEN_STORAGE_KEY, useAuthStore } from '../stores/auth'
import { useTournamentsStore, type Tournament } from '../stores/tournaments'

const auth = useAuthStore()
const tournaments = useTournamentsStore()

const items = ref<Tournament[]>([])
const localError = ref<string | null>(null)
const filter = ref<'all' | 'needs_bracket' | 'open' | 'live'>('all')

function resolveBearer(): string | null {
  return coerceBearerToken(auth.token) ?? (typeof localStorage !== 'undefined' ? localStorage.getItem(LEONBON_TOKEN_STORAGE_KEY) : null)
}

function needsBracket(t: Tournament): boolean {
  return t.lifecycleStatus === 'REGISTRATION_CLOSED' && (t.bracketSize == null || t.bracketSize === 0)
}

const filteredItems = computed(() => {
  switch (filter.value) {
    case 'needs_bracket':
      return items.value.filter(needsBracket)
    case 'open':
      return items.value.filter((t) => t.lifecycleStatus === 'REGISTRATION_OPEN')
    case 'live':
      return items.value.filter((t) => t.lifecycleStatus === 'LIVE')
    default:
      return items.value
  }
})

async function load() {
  const bearer = resolveBearer()
  if (!bearer) return
  localError.value = null
  try {
    items.value = await tournaments.listTournamentsAdmin(bearer)
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

onMounted(async () => {
  await auth.bootstrap()
  await load()
})

function fmt(iso: string) {
  return formatDateTimeShort(iso)
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
        <RouterLink to="/tournaments" class="text-sm text-zinc-400 hover:text-zinc-200">← Torneos públicos</RouterLink>
        <h1 class="mt-4 text-2xl font-semibold tracking-tight text-amber-100">Admin — Torneos</h1>
        <p class="mt-2 max-w-2xl text-sm text-amber-200/80">
          Listado interno (orden: última modificación). Usa
          <span class="font-medium text-amber-100">«Falta bracket»</span>
          para volver a un torneo al que ya cerraste inscripciones y aún no generaste el calendario.
        </p>
      </div>
      <RouterLink
        class="shrink-0 rounded-md bg-amber-400 px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-amber-300"
        to="/admin/tournaments/create"
      >
        Crear torneo
      </RouterLink>
    </div>

    <p v-if="auth.me?.role !== 'ADMIN'" class="text-sm text-rose-300">Necesitas cuenta de administrador para ver esta página.</p>

    <template v-else>
      <div class="flex flex-wrap gap-2">
        <button
          type="button"
          class="rounded-md border px-3 py-1.5 text-xs"
          :class="
            filter === 'all'
              ? 'border-amber-600/60 bg-amber-950/50 text-amber-50'
              : 'border-amber-900/40 bg-zinc-950 text-amber-200/70 hover:border-amber-800/60'
          "
          @click="filter = 'all'"
        >
          Todos ({{ items.length }})
        </button>
        <button
          type="button"
          class="rounded-md border px-3 py-1.5 text-xs"
          :class="
            filter === 'needs_bracket'
              ? 'border-amber-600/60 bg-amber-950/50 text-amber-50'
              : 'border-amber-900/40 bg-zinc-950 text-amber-200/70 hover:border-amber-800/60'
          "
          @click="filter = 'needs_bracket'"
        >
          Falta bracket ({{ items.filter(needsBracket).length }})
        </button>
        <button
          type="button"
          class="rounded-md border px-3 py-1.5 text-xs"
          :class="
            filter === 'open'
              ? 'border-amber-600/60 bg-amber-950/50 text-amber-50'
              : 'border-amber-900/40 bg-zinc-950 text-amber-200/70 hover:border-amber-800/60'
          "
          @click="filter = 'open'"
        >
          Inscripción abierta ({{ items.filter((x) => x.lifecycleStatus === 'REGISTRATION_OPEN').length }})
        </button>
        <button
          type="button"
          class="rounded-md border px-3 py-1.5 text-xs"
          :class="
            filter === 'live'
              ? 'border-amber-600/60 bg-amber-950/50 text-amber-50'
              : 'border-amber-900/40 bg-zinc-950 text-amber-200/70 hover:border-amber-800/60'
          "
          @click="filter = 'live'"
        >
          En curso ({{ items.filter((x) => x.lifecycleStatus === 'LIVE').length }})
        </button>
      </div>

      <p v-if="localError" class="text-sm text-rose-300">{{ localError }}</p>

      <div v-if="items.length === 0 && !localError" class="rounded-xl border border-amber-900/30 bg-amber-950/20 p-6 text-sm text-amber-100/80">
        No hay torneos en la base (o aún no cargan). Crea uno o revisa el backend.
      </div>

      <div
        v-else-if="filteredItems.length === 0 && !localError"
        class="rounded-xl border border-amber-900/30 bg-amber-950/20 p-6 text-sm text-amber-100/80"
      >
        Ningún torneo coincide con este filtro.
      </div>

      <div v-else class="overflow-x-auto rounded-xl border border-amber-900/30">
        <table class="w-full min-w-[44rem] text-left text-sm">
          <thead class="bg-zinc-950 text-xs text-amber-200/70">
            <tr>
              <th class="px-4 py-2">Nombre</th>
              <th class="px-4 py-2">Estado</th>
              <th class="px-4 py-2">Formato</th>
              <th class="px-4 py-2">Bracket</th>
              <th class="hidden px-4 py-2 lg:table-cell">Competencia</th>
              <th class="px-4 py-2 text-right">Gestión</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in filteredItems" :key="t.id" class="border-t border-zinc-800 bg-zinc-950/50">
              <td class="px-4 py-3 font-medium text-zinc-100">{{ t.name }}</td>
              <td class="px-4 py-3">
                <span
                  class="inline-block rounded border px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide"
                  :class="lifecycleBadgeClass(t.lifecycleStatus)"
                >
                  {{ lifecycleLabel(t.lifecycleStatus) }}
                </span>
              </td>
              <td class="px-4 py-3 font-mono text-xs text-zinc-300">{{ t.format }}</td>
              <td class="px-4 py-3 text-xs text-zinc-400">
                <span v-if="t.bracketSize != null && t.bracketSize > 0" class="text-emerald-300/90">Sí ({{ t.bracketSize }})</span>
                <span v-else class="text-zinc-500">No</span>
              </td>
              <td class="hidden px-4 py-3 text-xs text-zinc-500 lg:table-cell">
                {{ fmt(t.competitionStartAt) }} — {{ fmt(t.competitionEndAt) }}
              </td>
              <td class="px-4 py-3 text-right">
                <RouterLink
                  class="rounded-md border border-amber-800/50 bg-amber-950/40 px-2 py-1 text-xs text-amber-100 hover:bg-amber-950/70"
                  :to="`/tournaments/${t.id}`"
                >
                  Abrir
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>
