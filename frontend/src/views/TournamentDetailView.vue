<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { coerceBearerToken } from '../lib/api'
import { LEONBON_TOKEN_STORAGE_KEY, useAuthStore } from '../stores/auth'
import { useTeamsStore, type CaptainTeamSummary, type TeamCaptainView } from '../stores/teams'
import {
  useTournamentsStore,
  entryParticipantLabel,
  rosterSizeForGame,
  type BracketMatch,
  type Tournament,
  type TournamentEntry,
} from '../stores/tournaments'

const route = useRoute()
const auth = useAuthStore()
const teams = useTeamsStore()
const tournaments = useTournamentsStore()

const tournamentId = computed(() => String(route.params.tournamentId))

const tournament = ref<Tournament | null>(null)
const entries = ref<TournamentEntry[]>([])
const matches = ref<BracketMatch[]>([])
const captainTeams = ref<CaptainTeamSummary[]>([])
const selectedTeamId = ref<string>('')
const rosterDetail = ref<TeamCaptainView | null>(null)
const selectedRosterIds = ref<string[]>([])
const localError = ref<string | null>(null)
const successMsg = ref<string | null>(null)

const requiredRoster = computed(() => (tournament.value ? rosterSizeForGame(tournament.value.game) : 0))

const isMlb = computed(() => tournament.value?.game === 'MLB')
const isTeamGame = computed(() => tournament.value && tournament.value.game !== 'MLB')

const myMlbEntry = computed(() => {
  const uid = auth.me?.id
  if (!uid) return null
  return entries.value.find((e) => e.type === 'PLAYER' && e.playerId === uid) ?? null
})

const myCaptainTeamIds = computed(() => new Set(captainTeams.value.map((t) => t.id)))

const myTeamEntries = computed(() =>
  entries.value.filter((e) => e.type === 'TEAM' && e.teamId && myCaptainTeamIds.value.has(e.teamId)),
)

const isAdmin = computed(() => auth.me?.role === 'ADMIN')

const tableColspan = computed(() => (isAdmin.value ? 5 : 4))

const adminEntryBusy = ref<string | null>(null)
const adminBracketBusy = ref(false)

const registrationOpen = computed(() => tournament.value?.lifecycleStatus === 'REGISTRATION_OPEN')
const canBuildBracket = computed(
  () =>
    isAdmin.value &&
    tournament.value?.lifecycleStatus === 'REGISTRATION_CLOSED' &&
    !tournament.value?.bracketSize,
)

function resolveBearer(): string | null {
  return coerceBearerToken(auth.token) ?? (typeof localStorage !== 'undefined' ? localStorage.getItem(LEONBON_TOKEN_STORAGE_KEY) : null)
}

/** Nombre legible para una entrada del torneo (id de TournamentEntry). */
function entryDisplayForBracket(entryId: string | null) {
  if (!entryId) return '—'
  const row = entries.value.find((x) => x.id === entryId)
  if (!row) return `…${entryId.slice(-6)}`
  return entryParticipantLabel(row)
}

/**
 * Celda A/B del bracket: nombre del equipo/jugador; si aún no hay entrada asignada y el partido espera
 * rondas previas, "—"; si es hueco de bye real en una partida ya armada, "BYE".
 */
function bracketPoolLabel(pool: string) {
  switch (pool) {
    case 'LB':
      return 'Perdedores'
    case 'GF':
      return 'Final'
    case 'RR':
      return 'Liga (RR)'
    default:
      return 'Ganadores'
  }
}

function bracketSlotLabel(entryId: string | null, match: BracketMatch) {
  if (entryId) {
    const row = entries.value.find((x) => x.id === entryId)
    if (!row) return `…${entryId.slice(-6)}`
    return entryParticipantLabel(row)
  }
  if (match.status === 'WAITING') return '—'
  return 'BYE'
}

async function reload() {
  localError.value = null
  const id = tournamentId.value
  tournament.value = await tournaments.getTournament(id)
  entries.value = await tournaments.listEntries(id)
  matches.value = await tournaments.listMatches(id)

  const bearer = resolveBearer()
  if (bearer) {
    try {
      captainTeams.value = await teams.listMyCaptainTeams(bearer)
    } catch (e) {
      captainTeams.value = []
      const msg = e instanceof Error ? e.message : ''
      if (msg.includes('401')) {
        localError.value =
          'No se pudo validar la sesión al cargar tus equipos (401). Prueba cerrar sesión y entrar de nuevo.'
      } else {
        localError.value = msg || 'No se pudieron cargar tus equipos de capitán.'
      }
    }
    if (isTeamGame.value && selectedTeamId.value) {
      try {
        await loadRosterDetail(selectedTeamId.value)
      } catch {
        rosterDetail.value = null
        selectedRosterIds.value = []
      }
    }
  } else {
    captainTeams.value = []
    rosterDetail.value = null
  }
}

async function loadRosterDetail(teamId: string) {
  const bearer = resolveBearer()
  if (!bearer || !teamId) {
    rosterDetail.value = null
    selectedRosterIds.value = []
    return
  }
  const t = (await teams.getTeam(bearer, teamId)) as TeamCaptainView
  if (!('memberUserIds' in t)) {
    rosterDetail.value = null
    selectedRosterIds.value = []
    return
  }
  rosterDetail.value = t
  selectedRosterIds.value = []
}

watch(selectedTeamId, (id) => {
  void loadRosterDetail(id)
})

function toggleRoster(uid: string) {
  const i = selectedRosterIds.value.indexOf(uid)
  const max = requiredRoster.value
  if (i >= 0) {
    selectedRosterIds.value = selectedRosterIds.value.filter((x) => x !== uid)
    return
  }
  if (selectedRosterIds.value.length >= max) {
    return
  }
  selectedRosterIds.value = [...selectedRosterIds.value, uid]
}

function memberLabel(idx: number) {
  const d = rosterDetail.value
  if (!d) return ''
  const name = d.memberUsernames[idx]
  return name && name.length > 0 ? name : d.memberUserIds[idx]
}

onMounted(async () => {
  try {
    await auth.bootstrap()
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
})

async function submitTeam() {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  localError.value = null
  successMsg.value = null
  try {
    await tournaments.registerTeam(bearer, tournament.value.id, {
      teamId: selectedTeamId.value,
      selectedRosterUserIds: selectedRosterIds.value,
    })
    successMsg.value = 'Inscripción enviada (pendiente de aprobación).'
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function submitMlb() {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  localError.value = null
  successMsg.value = null
  try {
    await tournaments.registerMlbSelf(bearer, tournament.value.id)
    successMsg.value = 'Inscripción enviada (pendiente de aprobación).'
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

function fmt(iso: string) {
  return new Date(iso).toLocaleString()
}

async function approveEntry(entryId: string) {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  localError.value = null
  adminEntryBusy.value = entryId
  try {
    await tournaments.approveEntryAdmin(bearer, tournament.value.id, entryId)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  } finally {
    adminEntryBusy.value = null
  }
}

async function rejectEntry(entryId: string) {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  localError.value = null
  adminEntryBusy.value = entryId
  try {
    await tournaments.rejectEntryAdmin(bearer, tournament.value.id, entryId)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  } finally {
    adminEntryBusy.value = null
  }
}

async function closeRegistration() {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  localError.value = null
  adminBracketBusy.value = true
  try {
    await tournaments.closeRegistrationAdmin(bearer, tournament.value.id)
    successMsg.value = 'Inscripción cerrada.'
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  } finally {
    adminBracketBusy.value = false
  }
}

async function generateBracket() {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  localError.value = null
  adminBracketBusy.value = true
  try {
    await tournaments.generateBracketAdmin(bearer, tournament.value.id)
    successMsg.value = 'Calendario / bracket generado. Torneo en LIVE.'
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  } finally {
    adminBracketBusy.value = false
  }
}

async function setMatchWinner(matchId: string, winnerEntryId: string) {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  localError.value = null
  adminBracketBusy.value = true
  try {
    await tournaments.setMatchWinnerAdmin(bearer, tournament.value.id, matchId, winnerEntryId)
    successMsg.value = 'Ganador registrado.'
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  } finally {
    adminBracketBusy.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-wrap items-center gap-3 text-sm">
      <RouterLink to="/tournaments" class="text-zinc-400 hover:text-zinc-200">← Torneos</RouterLink>
      <RouterLink v-if="isAdmin" to="/admin/tournaments" class="text-amber-200/90 hover:text-amber-100">Admin torneos</RouterLink>
    </div>

    <p v-if="localError" class="text-sm text-rose-300">{{ localError }}</p>
    <p v-if="successMsg" class="text-sm text-emerald-300">{{ successMsg }}</p>

    <div v-if="tournament" class="space-y-4">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight text-zinc-100">{{ tournament.name }}</h1>
        <p class="mt-1 text-sm text-zinc-400">{{ tournament.organizers }}</p>
      </div>

      <dl class="grid gap-3 text-sm sm:grid-cols-2">
        <div class="rounded-lg border border-zinc-800 bg-zinc-900/40 px-3 py-2">
          <dt class="text-zinc-500">Juego</dt>
          <dd class="mt-1 font-mono text-zinc-200">{{ tournament.game }}</dd>
        </div>
        <div class="rounded-lg border border-zinc-800 bg-zinc-900/40 px-3 py-2">
          <dt class="text-zinc-500">Formato</dt>
          <dd class="mt-1 font-mono text-zinc-200">{{ tournament.format }}</dd>
        </div>
        <div class="rounded-lg border border-zinc-800 bg-zinc-900/40 px-3 py-2 sm:col-span-2">
          <dt class="text-zinc-500">Estado del torneo</dt>
          <dd class="mt-1 font-mono text-zinc-200">{{ tournament.lifecycleStatus }}</dd>
        </div>
        <div class="rounded-lg border border-zinc-800 bg-zinc-900/40 px-3 py-2 sm:col-span-2">
          <dt class="text-zinc-500">Inscripción</dt>
          <dd class="mt-1 text-zinc-200">{{ fmt(tournament.registrationStartAt) }} — {{ fmt(tournament.registrationEndAt) }}</dd>
        </div>
        <div class="rounded-lg border border-zinc-800 bg-zinc-900/40 px-3 py-2 sm:col-span-2">
          <dt class="text-zinc-500">Competencia</dt>
          <dd class="mt-1 text-zinc-200">{{ fmt(tournament.competitionStartAt) }} — {{ fmt(tournament.competitionEndAt) }}</dd>
        </div>
        <div v-if="tournament.streamUrl" class="rounded-lg border border-zinc-800 bg-zinc-900/40 px-3 py-2 sm:col-span-2">
          <dt class="text-zinc-500">Stream</dt>
          <dd class="mt-1">
            <a :href="tournament.streamUrl" class="text-sky-400 hover:underline" target="_blank" rel="noopener noreferrer">{{
              tournament.streamUrl
            }}</a>
          </dd>
        </div>
      </dl>

      <section v-if="isAdmin" class="rounded-xl border border-amber-900/40 bg-amber-950/20 p-5">
        <h2 class="text-sm font-semibold text-amber-100">Admin — bracket</h2>
        <p class="mt-1 text-xs text-amber-200/80">
          Cierra inscripciones, aprueba al menos 2 entradas, luego genera el calendario.
          <span class="font-mono">SINGLE_ELIM</span> y <span class="font-mono">DOUBLE_ELIM</span> (hasta 8 slots en cuadro de
          ganadores). <span class="font-mono">ROUND_ROBIN</span>: todos contra todos; al terminar todas las partidas el torneo pasa a
          <span class="font-mono">COMPLETED</span>. Marca ganador en cada partida <span class="font-mono">READY</span>.
        </p>
        <div class="mt-4 flex flex-wrap gap-2">
          <button
            v-if="registrationOpen"
            type="button"
            class="rounded-md border border-amber-800 bg-amber-950/50 px-3 py-2 text-xs text-amber-100 hover:bg-amber-950/70 disabled:opacity-40"
            :disabled="adminBracketBusy"
            @click="closeRegistration()"
          >
            Cerrar inscripciones
          </button>
          <button
            v-if="canBuildBracket"
            type="button"
            class="rounded-md bg-amber-400 px-3 py-2 text-xs font-medium text-zinc-950 hover:bg-amber-300 disabled:opacity-40"
            :disabled="adminBracketBusy"
            @click="generateBracket()"
          >
            Generar bracket
          </button>
        </div>
      </section>

      <section v-if="matches.length > 0" class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
        <h2 class="text-sm font-semibold text-zinc-200">Partidas (bracket)</h2>
        <div class="mt-4 overflow-x-auto">
          <table class="w-full min-w-[28rem] text-left text-xs">
            <thead class="bg-zinc-950 text-zinc-400">
              <tr>
                <th class="px-3 py-2">Tabla</th>
                <th class="px-3 py-2">Ronda</th>
                <th class="px-3 py-2">#</th>
                <th class="px-3 py-2">A</th>
                <th class="px-3 py-2">B</th>
                <th class="px-3 py-2">Estado</th>
                <th class="px-3 py-2">Ganador</th>
                <th v-if="isAdmin" class="px-3 py-2 text-right">Admin</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in matches" :key="m.id" class="border-t border-zinc-800 bg-zinc-950/40">
                <td class="px-3 py-2 text-zinc-400">{{ bracketPoolLabel(m.bracketPool ?? 'WB') }}</td>
                <td class="px-3 py-2 font-mono text-zinc-300">{{ m.round }}</td>
                <td class="px-3 py-2 font-mono text-zinc-400">{{ m.indexInRound }}</td>
                <td class="px-3 py-2 text-zinc-200">{{ bracketSlotLabel(m.entryIdA, m) }}</td>
                <td class="px-3 py-2 text-zinc-200">{{ bracketSlotLabel(m.entryIdB, m) }}</td>
                <td class="px-3 py-2">{{ m.status }}</td>
                <td class="px-3 py-2 text-zinc-400">{{ m.winnerEntryId ? entryDisplayForBracket(m.winnerEntryId) : '—' }}</td>
                <td v-if="isAdmin" class="px-3 py-2 text-right">
                  <template v-if="m.status === 'READY' && m.entryIdA && m.entryIdB">
                    <button
                      type="button"
                      class="mr-1 rounded border border-emerald-800 px-1.5 py-0.5 text-emerald-300 hover:bg-emerald-950/60 disabled:opacity-40"
                      :disabled="adminBracketBusy"
                      @click="setMatchWinner(m.id, m.entryIdA!)"
                    >
                      A
                    </button>
                    <button
                      type="button"
                      class="rounded border border-emerald-800 px-1.5 py-0.5 text-emerald-300 hover:bg-emerald-950/60 disabled:opacity-40"
                      :disabled="adminBracketBusy"
                      @click="setMatchWinner(m.id, m.entryIdB!)"
                    >
                      B
                    </button>
                  </template>
                  <span v-else class="text-zinc-600">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Inscripción -->
      <template v-if="auth.isAuthed && registrationOpen">
        <section class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
          <h2 class="text-sm font-semibold text-zinc-200">Inscripción</h2>

          <template v-if="isMlb">
            <p v-if="myMlbEntry" class="mt-3 text-sm text-zinc-400">
              Ya estás inscrito en este torneo (estado: <span class="font-mono text-zinc-200">{{ myMlbEntry.status }}</span
              >).
            </p>
            <div v-else class="mt-4">
              <button
                type="button"
                class="rounded-md bg-white px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-zinc-200 disabled:opacity-50"
                :disabled="tournaments.busy"
                @click="submitMlb()"
              >
                Participar (individual)
              </button>
            </div>
          </template>

          <template v-else-if="isTeamGame">
            <p v-if="myTeamEntries.length > 0" class="mt-3 text-sm text-zinc-400">
              Tu(s) equipo(s) con inscripción en este torneo:
              <span v-for="e in myTeamEntries" :key="e.id" class="ml-1 text-zinc-200"
                >{{ entryParticipantLabel(e) }} <span class="font-mono text-zinc-400">({{ e.status }})</span></span
              >
            </p>

            <div class="mt-4 space-y-4">
              <div>
                <label class="block text-xs text-zinc-500">Equipo (solo capitán)</label>
                <select
                  v-model="selectedTeamId"
                  class="mt-1 w-full max-w-md rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100"
                >
                  <option value="">— Elegir equipo —</option>
                  <option v-for="ct in captainTeams" :key="ct.id" :value="ct.id">{{ ct.name }} [{{ ct.tag }}]</option>
                </select>
                <p v-if="captainTeams.length === 0" class="mt-2 text-xs text-zinc-500">
                  No hay equipos donde seas capitán y el equipo esté aprobado por staff (los pendientes no aparecen
                  aquí).
                  <RouterLink class="text-sky-400 hover:underline" to="/teams/create">Crear equipo</RouterLink>
                  o
                  <RouterLink class="text-sky-400 hover:underline" to="/teams">explorar equipos</RouterLink>.
                </p>
              </div>

              <div v-if="rosterDetail && selectedTeamId">
                <p class="text-xs text-zinc-500">
                  Selecciona exactamente <span class="font-mono text-zinc-300">{{ requiredRoster }}</span> jugadores del roster
                  ({{ tournament.game }}).
                </p>
                <p class="mt-1 text-xs text-zinc-500">Seleccionados: {{ selectedRosterIds.length }} / {{ requiredRoster }}</p>
                <ul class="mt-3 space-y-2">
                  <li v-for="(uid, idx) in rosterDetail.memberUserIds" :key="uid" class="flex items-center gap-2 text-sm">
                    <input
                      type="checkbox"
                      class="rounded border-zinc-700 bg-zinc-950"
                      :checked="selectedRosterIds.includes(uid)"
                      :disabled="!selectedRosterIds.includes(uid) && selectedRosterIds.length >= requiredRoster"
                      @click.prevent="toggleRoster(uid)"
                    />
                    <span class="text-zinc-200">{{ memberLabel(idx) }}</span>
                  </li>
                </ul>
                <button
                  type="button"
                  class="mt-4 rounded-md bg-white px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-zinc-200 disabled:opacity-50"
                  :disabled="tournaments.busy || !selectedTeamId || selectedRosterIds.length !== requiredRoster"
                  @click="submitTeam()"
                >
                  Inscribir equipo
                </button>
              </div>
            </div>
          </template>
        </section>
      </template>

      <section v-else-if="auth.isAuthed && tournament" class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
        <h2 class="text-sm font-semibold text-zinc-200">Inscripción</h2>
        <p class="mt-2 text-sm text-zinc-400">
          La inscripción no está abierta (estado: <span class="font-mono text-zinc-200">{{ tournament.lifecycleStatus }}</span
          >).
        </p>
      </section>

      <section v-else class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
        <h2 class="text-sm font-semibold text-zinc-200">Inscripción</h2>
        <p class="mt-2 text-sm text-zinc-400">
          <RouterLink class="text-sky-400 hover:underline" to="/login">Inicia sesión</RouterLink>
          para inscribirte.
        </p>
      </section>

      <section class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
        <h2 class="text-sm font-semibold text-zinc-200">Participantes (solicitudes)</h2>
        <p v-if="isAdmin" class="mt-1 text-xs text-zinc-500">Como admin puedes aprobar o rechazar solicitudes pendientes.</p>
        <div class="mt-4 overflow-hidden rounded-lg border border-zinc-800">
          <table class="w-full text-left text-xs">
            <thead class="bg-zinc-950 text-zinc-400">
              <tr>
                <th class="px-3 py-2">Tipo</th>
                <th class="px-3 py-2">Equipo / jugador</th>
                <th class="px-3 py-2">Estado</th>
                <th class="px-3 py-2">Fecha</th>
                <th v-if="isAdmin" class="px-3 py-2 text-right">Admin</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="e in entries" :key="e.id" class="border-t border-zinc-800 bg-zinc-950/40">
                <td class="px-3 py-2 font-mono text-zinc-300">{{ e.type }}</td>
                <td class="px-3 py-2 text-zinc-200">{{ entryParticipantLabel(e) }}</td>
                <td class="px-3 py-2">{{ e.status }}</td>
                <td class="px-3 py-2 text-zinc-400">{{ fmt(e.createdAt) }}</td>
                <td v-if="isAdmin" class="px-3 py-2 text-right">
                  <template v-if="e.status === 'PENDING'">
                    <button
                      type="button"
                      class="mr-1 rounded border border-emerald-800 px-2 py-0.5 text-emerald-300 hover:bg-emerald-950/60 disabled:opacity-40"
                      :disabled="adminEntryBusy === e.id"
                      @click="approveEntry(e.id)"
                    >
                      OK
                    </button>
                    <button
                      type="button"
                      class="rounded border border-rose-900 px-2 py-0.5 text-rose-300 hover:bg-rose-950/40 disabled:opacity-40"
                      :disabled="adminEntryBusy === e.id"
                      @click="rejectEntry(e.id)"
                    >
                      No
                    </button>
                  </template>
                  <span v-else class="text-zinc-600">—</span>
                </td>
              </tr>
              <tr v-if="entries.length === 0">
                <td class="px-3 py-6 text-center text-zinc-500" :colspan="tableColspan">Sin inscripciones aún.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </div>
</template>
