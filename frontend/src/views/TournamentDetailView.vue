<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useTeamsStore, type CaptainTeamSummary, type TeamCaptainView } from '../stores/teams'
import { useTournamentsStore, rosterSizeForGame, type Tournament, type TournamentEntry } from '../stores/tournaments'

const route = useRoute()
const auth = useAuthStore()
const teams = useTeamsStore()
const tournaments = useTournamentsStore()

const tournamentId = computed(() => String(route.params.tournamentId))

const tournament = ref<Tournament | null>(null)
const entries = ref<TournamentEntry[]>([])
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

async function reload() {
  localError.value = null
  const id = tournamentId.value
  tournament.value = await tournaments.getTournament(id)
  entries.value = await tournaments.listEntries(id)

  if (auth.isAuthed && auth.token) {
    captainTeams.value = await teams.listMyCaptainTeams(auth.token)
    if (isTeamGame.value && selectedTeamId.value) {
      await loadRosterDetail(selectedTeamId.value)
    }
  } else {
    captainTeams.value = []
    rosterDetail.value = null
  }
}

async function loadRosterDetail(teamId: string) {
  if (!auth.token || !teamId) {
    rosterDetail.value = null
    selectedRosterIds.value = []
    return
  }
  const t = (await teams.getTeam(auth.token, teamId)) as TeamCaptainView
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
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
})

async function submitTeam() {
  if (!auth.token || !tournament.value) return
  localError.value = null
  successMsg.value = null
  try {
    await tournaments.registerTeam(auth.token, tournament.value.id, {
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
  if (!auth.token || !tournament.value) return
  localError.value = null
  successMsg.value = null
  try {
    await tournaments.registerMlbSelf(auth.token, tournament.value.id)
    successMsg.value = 'Inscripción enviada (pendiente de aprobación).'
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

function fmt(iso: string) {
  return new Date(iso).toLocaleString()
}
</script>

<template>
  <div class="space-y-6">
    <RouterLink to="/tournaments" class="text-sm text-zinc-400 hover:text-zinc-200">← Torneos</RouterLink>

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

      <!-- Inscripción -->
      <template v-if="auth.isAuthed">
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
              <span v-for="e in myTeamEntries" :key="e.id" class="ml-1 font-mono text-zinc-200">{{ e.teamId }} ({{ e.status }})</span>
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
                  No eres capitán de ningún equipo aprobado.
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

      <section v-else class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
        <h2 class="text-sm font-semibold text-zinc-200">Inscripción</h2>
        <p class="mt-2 text-sm text-zinc-400">
          <RouterLink class="text-sky-400 hover:underline" to="/login">Inicia sesión</RouterLink>
          para inscribirte.
        </p>
      </section>

      <section class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
        <h2 class="text-sm font-semibold text-zinc-200">Participantes (solicitudes)</h2>
        <div class="mt-4 overflow-hidden rounded-lg border border-zinc-800">
          <table class="w-full text-left text-xs">
            <thead class="bg-zinc-950 text-zinc-400">
              <tr>
                <th class="px-3 py-2">Tipo</th>
                <th class="px-3 py-2">Equipo / jugador</th>
                <th class="px-3 py-2">Estado</th>
                <th class="px-3 py-2">Fecha</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="e in entries" :key="e.id" class="border-t border-zinc-800 bg-zinc-950/40">
                <td class="px-3 py-2 font-mono text-zinc-300">{{ e.type }}</td>
                <td class="px-3 py-2 font-mono text-zinc-200">{{ e.teamId ?? e.playerId ?? '—' }}</td>
                <td class="px-3 py-2">{{ e.status }}</td>
                <td class="px-3 py-2 text-zinc-400">{{ fmt(e.createdAt) }}</td>
              </tr>
              <tr v-if="entries.length === 0">
                <td class="px-3 py-6 text-center text-zinc-500" colspan="4">Sin inscripciones aún.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </div>
</template>
