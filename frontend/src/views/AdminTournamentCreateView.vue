<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { coerceBearerToken } from '../lib/api'
import { LEONBON_TOKEN_STORAGE_KEY, useAuthStore } from '../stores/auth'
import { useTournamentsStore, type CreateTournamentPayload } from '../stores/tournaments'

const auth = useAuthStore()
const tournaments = useTournamentsStore()
const router = useRouter()

const name = ref('')
const organizers = ref('')
const game = ref<CreateTournamentPayload['game']>('VALORANT')
const format = ref<CreateTournamentPayload['format']>('SINGLE_ELIM')
const registrationStartLocal = ref('')
const registrationEndLocal = ref('')
const competitionStartLocal = ref('')
const competitionEndLocal = ref('')
const streamUrl = ref('')

const localError = ref<string | null>(null)

function resolveBearer(): string | null {
  return coerceBearerToken(auth.token) ?? (typeof localStorage !== 'undefined' ? localStorage.getItem(LEONBON_TOKEN_STORAGE_KEY) : null)
}

function toIso(localDatetime: string): string {
  const d = new Date(localDatetime)
  if (Number.isNaN(d.getTime())) {
    throw new Error('Fecha inválida')
  }
  return d.toISOString()
}

async function submit() {
  localError.value = null
  const bearer = resolveBearer()
  if (!bearer) {
    localError.value = 'Necesitas iniciar sesión.'
    return
  }
  if (auth.me?.role !== 'ADMIN') {
    localError.value = 'Solo administradores pueden crear torneos.'
    return
  }

  const n = name.value.trim()
  const o = organizers.value.trim()
  if (!n || !o) {
    localError.value = 'Nombre y organizadores son obligatorios.'
    return
  }
  if (!registrationStartLocal.value || !registrationEndLocal.value || !competitionStartLocal.value || !competitionEndLocal.value) {
    localError.value = 'Completa las cuatro fechas.'
    return
  }

  let registrationStartAt: string
  let registrationEndAt: string
  let competitionStartAt: string
  let competitionEndAt: string
  try {
    registrationStartAt = toIso(registrationStartLocal.value)
    registrationEndAt = toIso(registrationEndLocal.value)
    competitionStartAt = toIso(competitionStartLocal.value)
    competitionEndAt = toIso(competitionEndLocal.value)
  } catch {
    localError.value = 'Revisa las fechas (formato inválido).'
    return
  }

  const body: CreateTournamentPayload = {
    name: n,
    organizers: o,
    game: game.value,
    format: format.value,
    registrationStartAt,
    registrationEndAt,
    competitionStartAt,
    competitionEndAt,
    streamUrl: streamUrl.value.trim() || null,
  }

  try {
    const created = await tournaments.createTournamentAdmin(bearer, body)
    await router.push({ name: 'tournament-detail', params: { tournamentId: created.id } })
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}
</script>

<template>
  <div class="mx-auto max-w-xl space-y-6">
    <div>
      <RouterLink to="/tournaments" class="text-sm text-zinc-400 hover:text-zinc-200">← Torneos</RouterLink>
      <h1 class="mt-4 text-2xl font-semibold tracking-tight text-zinc-100">Crear torneo (admin)</h1>
      <p class="mt-2 text-sm text-zinc-400">
        Las fechas se convierten a UTC según la zona horaria de tu navegador.
      </p>
    </div>

    <p v-if="localError" class="text-sm text-rose-300">{{ localError }}</p>

    <form class="space-y-4 rounded-xl border border-zinc-800 bg-zinc-900/40 p-5" @submit.prevent="submit()">
      <div>
        <label class="block text-xs text-zinc-500">Nombre</label>
        <input
          v-model="name"
          type="text"
          required
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100"
        />
      </div>
      <div>
        <label class="block text-xs text-zinc-500">Organizadores</label>
        <input
          v-model="organizers"
          type="text"
          required
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100"
        />
      </div>
      <div class="grid gap-4 sm:grid-cols-2">
        <div>
          <label class="block text-xs text-zinc-500">Juego</label>
          <select v-model="game" class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100">
            <option value="VALORANT">VALORANT</option>
            <option value="FORTNITE">FORTNITE</option>
            <option value="MLB">MLB</option>
          </select>
        </div>
        <div>
          <label class="block text-xs text-zinc-500">Formato</label>
          <select v-model="format" class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100">
            <option value="SINGLE_ELIM">SINGLE_ELIM</option>
            <option value="DOUBLE_ELIM">DOUBLE_ELIM</option>
            <option value="ROUND_ROBIN">ROUND_ROBIN</option>
          </select>
        </div>
      </div>
      <p class="text-xs text-zinc-500">
        <span class="font-mono text-zinc-400">DOUBLE_ELIM</span>: el generador soporta hasta 8 huecos en cuadro de ganadores (potencia de 2).
        <span class="font-mono text-zinc-400">ROUND_ROBIN</span>: todos contra todos; el torneo se marca finalizado cuando todas las partidas tienen ganador.
      </p>

      <div class="grid gap-4 sm:grid-cols-2">
        <div>
          <label class="block text-xs text-zinc-500">Inicio inscripción</label>
          <input
            v-model="registrationStartLocal"
            type="datetime-local"
            required
            class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100"
          />
        </div>
        <div>
          <label class="block text-xs text-zinc-500">Fin inscripción</label>
          <input
            v-model="registrationEndLocal"
            type="datetime-local"
            required
            class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100"
          />
        </div>
      </div>

      <div class="grid gap-4 sm:grid-cols-2">
        <div>
          <label class="block text-xs text-zinc-500">Inicio competencia</label>
          <input
            v-model="competitionStartLocal"
            type="datetime-local"
            required
            class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100"
          />
        </div>
        <div>
          <label class="block text-xs text-zinc-500">Fin competencia</label>
          <input
            v-model="competitionEndLocal"
            type="datetime-local"
            required
            class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100"
          />
        </div>
      </div>

      <div>
        <label class="block text-xs text-zinc-500">URL stream (opcional)</label>
        <input
          v-model="streamUrl"
          type="url"
          placeholder="https://…"
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100"
        />
      </div>

      <p class="text-xs text-zinc-500">
        El backend exige: inicio inscripción &lt; fin inscripción &lt; inicio competencia &lt; fin competencia.
      </p>

      <button
        type="submit"
        class="w-full rounded-md bg-white px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-zinc-200 disabled:opacity-50"
        :disabled="tournaments.busy"
      >
        Crear torneo
      </button>
    </form>
  </div>
</template>
