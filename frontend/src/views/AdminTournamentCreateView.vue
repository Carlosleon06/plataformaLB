<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import DatetimePickerField from '../components/DatetimePickerField.vue'
import { coerceBearerToken } from '../lib/api'
import {
  addMsToLocal,
  defaultRegistrationStartLocal,
  parseDatetimeLocal,
} from '../lib/datetimeLocal'
import { LEONBON_TOKEN_STORAGE_KEY, useAuthStore } from '../stores/auth'
import { useTournamentsStore, type CreateTournamentPayload } from '../stores/tournaments'

const DAY_MS = 24 * 60 * 60 * 1000

const auth = useAuthStore()
const tournaments = useTournamentsStore()
const router = useRouter()

const name = ref('')
const organizers = ref('Leön Bon')
const game = ref<CreateTournamentPayload['game']>('VALORANT')
const format = ref<CreateTournamentPayload['format']>('SINGLE_ELIM')
const registrationStartLocal = ref('')
const registrationEndLocal = ref('')
const competitionStartLocal = ref('')
const competitionEndLocal = ref('')
const streamUrl = ref('')
const rulesHtml = ref('')
const eligibilityNotes = ref('')
const prizeNotes = ref('')
const maxApprovedParticipantsRaw = ref('')
const prizeWinnerSlotsCount = ref(0)
const prizeLeonDraft = ref<string[]>([])

const localError = ref<string | null>(null)

const minRegistrationEnd = computed(() => registrationStartLocal.value)
const minCompetitionStart = computed(() => registrationEndLocal.value)
const minCompetitionEnd = computed(() => competitionStartLocal.value)

function resolveBearer(): string | null {
  return coerceBearerToken(auth.token) ?? (typeof localStorage !== 'undefined' ? localStorage.getItem(LEONBON_TOKEN_STORAGE_KEY) : null)
}

function setPrizeRow(ix: number, val: string) {
  ensurePrizeDraftLength()
  const copy = [...prizeLeonDraft.value]
  copy[ix - 1] = val
  prizeLeonDraft.value = copy
}

function onPrizeInput(ix: number, e: Event) {
  const t = e.target as HTMLInputElement | null
  if (!t) return
  setPrizeRow(ix, t.value)
}

function ensurePrizeDraftLength() {
  let n = Math.floor(Number(prizeWinnerSlotsCount.value))
  if (!Number.isFinite(n) || n < 0) n = 0
  if (n > 32) n = 32
  prizeWinnerSlotsCount.value = n
  const cur = [...prizeLeonDraft.value]
  while (cur.length < n) cur.push('')
  prizeLeonDraft.value = cur.slice(0, n)
}

function placementLabel(rank: number): string {
  if (rank === 1) return '1.er lugar — L-Coins'
  if (rank === 2) return '2.º lugar — L-Coins'
  if (rank === 3) return '3.er lugar — L-Coins'
  return `${rank}.º lugar — L-Coins`
}

function toIso(localDatetime: string): string {
  const d = new Date(localDatetime)
  if (Number.isNaN(d.getTime())) {
    throw new Error('Fecha inválida')
  }
  return d.toISOString()
}

function applyDefaultSchedule() {
  const start = defaultRegistrationStartLocal()
  registrationStartLocal.value = start
  registrationEndLocal.value = addMsToLocal(start, 7 * DAY_MS)
  competitionStartLocal.value = addMsToLocal(registrationEndLocal.value, DAY_MS)
  competitionEndLocal.value = addMsToLocal(competitionStartLocal.value, 14 * DAY_MS)
}

onMounted(() => {
  applyDefaultSchedule()
})

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

  const rs = parseDatetimeLocal(registrationStartLocal.value)
  const re = parseDatetimeLocal(registrationEndLocal.value)
  const cs = parseDatetimeLocal(competitionStartLocal.value)
  const ce = parseDatetimeLocal(competitionEndLocal.value)
  if (!rs || !re || !cs || !ce) {
    localError.value = 'Revisa las fechas.'
    return
  }
  if (!(rs < re && re < cs && cs < ce)) {
    localError.value = 'Orden requerido: inicio inscripción < fin inscripción < inicio competencia < fin competencia.'
    return
  }

  const capStr = maxApprovedParticipantsRaw.value.trim()
  let maxApprovedParticipants: number | null = null
  if (capStr.length > 0) {
    const cap = Number.parseInt(capStr, 10)
    if (!Number.isFinite(cap) || cap < 1) {
      localError.value = 'Cupo máximo: introduce un entero ≥ 1 o déjalo vacío.'
      return
    }
    maxApprovedParticipants = cap
  }

  ensurePrizeDraftLength()
  const prizeSlots = prizeWinnerSlotsCount.value
  const prizeAmounts: number[] = []
  for (let i = 0; i < prizeSlots; i++) {
    const raw = (prizeLeonDraft.value[i] ?? '').trim()
    if (!raw.length) {
      prizeAmounts.push(0)
      continue
    }
    const amt = Number.parseInt(raw, 10)
    if (!Number.isFinite(amt) || amt < 0 || !Number.isInteger(amt)) {
      localError.value = `${placementLabel(i + 1)}: introduce un entero ≥ 0 o déjalo en 0.`
      return
    }
    prizeAmounts.push(amt)
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
    rulesHtml: rulesHtml.value.trim() || null,
    eligibilityNotes: eligibilityNotes.value.trim() || null,
    prizeNotes: prizeNotes.value.trim() || null,
    maxApprovedParticipants,
    prizeWinnerSlots: prizeSlots,
    prizeLeonCoinsByPlacement: prizeAmounts.length > 0 ? prizeAmounts : [],
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
      <div class="flex flex-wrap gap-3 text-sm">
        <RouterLink to="/tournaments" class="text-zinc-400 hover:text-zinc-200">← Torneos públicos</RouterLink>
        <RouterLink to="/admin/tournaments" class="text-amber-200/90 hover:text-amber-100">← Admin torneos</RouterLink>
      </div>
      <h1 class="mt-4 text-2xl font-semibold tracking-tight text-zinc-100">Crear torneo (admin)</h1>
      <p class="mt-2 text-sm text-zinc-400">
        Toca cada campo de fecha para abrir el calendario. Las cuatro etapas se configuran por separado.
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
          autofocus
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
        <span class="font-mono text-zinc-400">DOUBLE_ELIM</span>: hasta 8 en cuadro de ganadores.
        <span class="font-mono text-zinc-400">ROUND_ROBIN</span>: todos contra todos.
      </p>

      <div class="flex flex-wrap items-center justify-between gap-2">
        <p class="text-xs font-medium uppercase tracking-wide text-zinc-500">Calendario del torneo</p>
        <button
          type="button"
          class="text-xs text-sky-400 hover:text-sky-300"
          @click="applyDefaultSchedule()"
        >
          Restablecer sugeridas
        </button>
      </div>

      <div class="grid gap-4 sm:grid-cols-2">
        <DatetimePickerField v-model="registrationStartLocal" label="Inicio inscripción" />
        <DatetimePickerField
          v-model="registrationEndLocal"
          label="Fin inscripción"
          :min-local="minRegistrationEnd"
        />
        <DatetimePickerField
          v-model="competitionStartLocal"
          label="Inicio competencia"
          :min-local="minCompetitionStart"
        />
        <DatetimePickerField
          v-model="competitionEndLocal"
          label="Fin competencia"
          :min-local="minCompetitionEnd"
        />
      </div>
      <p class="text-xs text-zinc-500">
        Al crear, el torneo queda en <span class="font-mono text-zinc-400">Inscripción programada</span>. Los jugadores podrán inscribirse
        automáticamente entre inicio y fin de inscripción, o antes si un admin abre inscripciones manualmente en la ficha del torneo.
      </p>

      <div>
        <label class="block text-xs text-zinc-500">URL stream (opcional)</label>
        <input
          v-model="streamUrl"
          type="url"
          placeholder="https://…"
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100"
        />
      </div>
      <div>
        <label class="block text-xs text-zinc-500">Reglas (texto, opcional)</label>
        <textarea
          v-model="rulesHtml"
          rows="3"
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100"
          placeholder="Reglamento / formato de partidas…"
        />
      </div>
      <div>
        <label class="block text-xs text-zinc-500">Elegibilidad / requisitos (opcional)</label>
        <textarea
          v-model="eligibilityNotes"
          rows="2"
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100"
        />
      </div>
      <div>
        <label class="block text-xs text-zinc-500">Premios (texto, opcional)</label>
        <textarea
          v-model="prizeNotes"
          rows="2"
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100"
        />
      </div>

      <div class="rounded-lg border border-amber-900/30 bg-amber-950/10 p-4">
        <label class="block text-xs font-medium text-amber-200/95">L-Coins por posición</label>
        <div class="mt-3 max-w-xs">
          <label class="block text-xs text-zinc-500">Puestos con premio</label>
          <input
            v-model.number="prizeWinnerSlotsCount"
            type="number"
            min="0"
            max="32"
            step="1"
            inputmode="numeric"
            class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm font-mono text-zinc-100"
            @blur="ensurePrizeDraftLength()"
          />
        </div>
        <div v-if="prizeWinnerSlotsCount > 0" class="mt-4 space-y-3">
          <div v-for="ix in prizeWinnerSlotsCount" :key="ix" class="max-w-xs">
            <label class="block text-xs text-zinc-500">{{ placementLabel(ix) }}</label>
            <input
              :value="prizeLeonDraft[ix - 1] ?? ''"
              type="text"
              inputmode="numeric"
              placeholder="0"
              class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm font-mono text-zinc-100"
              @input="onPrizeInput(ix, $event)"
            />
          </div>
        </div>
      </div>

      <div>
        <label class="block text-xs text-zinc-500">Cupo máx. entradas (opcional)</label>
        <input
          v-model="maxApprovedParticipantsRaw"
          type="text"
          inputmode="numeric"
          placeholder="Vacío = sin límite"
          class="mt-1 w-full max-w-xs rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100"
        />
      </div>

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
