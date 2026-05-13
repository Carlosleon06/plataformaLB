<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import {
  useTeamsStore,
  type JoinRequest,
  type TeamCaptainView,
  type TeamCollectiveBracketStats,
  type TeamPublic,
  type TrophyAward,
} from '../stores/teams'

const route = useRoute()
const auth = useAuthStore()
const teams = useTeamsStore()

const team = ref<TeamPublic | TeamCaptainView | null>(null)
const collectiveStats = ref<TeamCollectiveBracketStats | null>(null)
const trophies = ref<TrophyAward[]>([])
const joinRequests = ref<JoinRequest[]>([])
const localError = ref<string | null>(null)

const teamId = computed(() => String(route.params.teamId))

/** Vista con roster (capitán, o admin moderando cualquier estado). */
const isCaptainView = computed(() => {
  const t = team.value
  return Boolean(auth.isAuthed && t && 'captainUserId' in t)
})

const isCaptain = computed(() => {
  const t = team.value
  if (!t || !('captainUserId' in t)) return false
  return (t as TeamCaptainView).captainUserId === auth.me?.id
})

const isMember = computed(() => {
  const t = team.value
  if (!t || !auth.me?.id) return false
  if ('captainUserId' in t) {
    return (t as TeamCaptainView).memberUserIds.includes(auth.me.id)
  }
  return false
})

const isAdmin = computed(() => auth.me?.role === 'ADMIN')

const soleCaptain = computed(() => {
  const t = team.value
  if (!t || !isCaptain.value) return false
  return t.memberCount === 1
})

const logoFile = ref<HTMLInputElement | null>(null)
const sponsorDraft = ref('')
const streamDraft = ref('')
const commercialBusy = ref(false)

async function reload() {
  localError.value = null
  team.value = (await teams.getTeam(auth.token, teamId.value)) as TeamPublic | TeamCaptainView
  sponsorDraft.value = (team.value.sponsorLines ?? []).join('\n')
  streamDraft.value = team.value.canonicalStreamUrl ?? ''

  try {
    collectiveStats.value = await teams.fetchCollectiveBracketStats(teamId.value)
  } catch {
    collectiveStats.value = null
  }
  try {
    trophies.value = await teams.fetchTeamTrophies(teamId.value)
  } catch {
    trophies.value = []
  }

  if (isCaptain.value) {
    joinRequests.value = await teams.listJoinRequests(auth.token!, teamId.value)
  } else {
    joinRequests.value = []
  }
}

onMounted(async () => {
  try {
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
})

async function approveAdmin() {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.approveTeamAdmin(auth.token, teamId.value)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function rejectAdmin() {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.rejectTeamAdmin(auth.token, teamId.value)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function requestJoin() {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.requestJoin(auth.token, teamId.value)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function accept(reqId: string) {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.acceptJoin(auth.token, teamId.value, reqId)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function reject(reqId: string) {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.rejectJoin(auth.token, teamId.value, reqId)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function resetLogoAdmin() {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.resetLogoAdmin(auth.token, teamId.value)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function uploadLogo() {
  if (!auth.token) return
  const input = logoFile.value
  const file = input?.files?.[0]
  if (!file) return
  localError.value = null
  try {
    await teams.uploadTeamLogo(auth.token, teamId.value, file)
    if (input) input.value = ''
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function resetLogoCaptain() {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.resetTeamLogoCaptain(auth.token, teamId.value)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function leaveTeam() {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.leaveTeam(auth.token, teamId.value)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function disbandTeam() {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.disbandTeam(auth.token, teamId.value)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function saveCommercial() {
  if (!auth.token || !isCaptain.value) return
  localError.value = null
  commercialBusy.value = true
  try {
    const sponsorLines = sponsorDraft.value
      .split(/\r?\n/)
      .map((s) => s.trim())
      .filter(Boolean)
      .slice(0, 15)
    await teams.patchCaptainCommercial(auth.token, teamId.value, {
      sponsorLines,
      canonicalStreamUrl: streamDraft.value.trim() || '',
    })
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  } finally {
    commercialBusy.value = false
  }
}

function fmtStat(n: number | null | undefined): string {
  if (n == null || Number.isNaN(n)) return '—'
  return Number.isInteger(n) ? String(n) : n.toFixed(2)
}
</script>

<template>
  <div v-if="localError && !team" class="space-y-2">
    <p class="text-sm text-rose-400">{{ localError }}</p>
    <RouterLink to="/teams" class="text-sm text-sky-400 hover:underline">Volver a equipos</RouterLink>
  </div>
  <div v-else-if="!team" class="text-sm text-zinc-400">Cargando…</div>

  <div v-else class="space-y-6">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <div class="text-xs text-zinc-500">{{ team.status }}</div>
        <h1 class="mt-1 text-2xl font-semibold tracking-tight">{{ team.name }}</h1>
        <p class="mt-2 text-sm text-zinc-400">
          {{ team.regionServer }} · {{ team.memberCount }} miembros
          <span v-if="isCaptainView" class="text-zinc-500">
            · capitán: {{ (team as TeamCaptainView).captainUsername }}
          </span>
        </p>
      </div>

      <div class="flex flex-wrap gap-2">
        <button
          type="button"
          class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 hover:bg-zinc-900"
          @click="reload()"
        >
          Refrescar
        </button>

        <button
          v-if="isAdmin && team.status === 'PENDING'"
          type="button"
          class="rounded-md bg-emerald-400 px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-emerald-300"
          @click="approveAdmin()"
        >
          Aprobar (admin)
        </button>

        <button
          v-if="isAdmin && team.status === 'PENDING'"
          type="button"
          class="rounded-md border border-rose-800 bg-rose-950/50 px-3 py-2 text-sm text-rose-100 hover:bg-rose-950/80"
          @click="rejectAdmin()"
        >
          Rechazar (admin)
        </button>

        <button
          v-if="isAdmin"
          type="button"
          class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 hover:bg-zinc-900"
          @click="resetLogoAdmin()"
        >
          Reset logo (admin)
        </button>

        <button
          v-if="auth.isAuthed && team.status === 'APPROVED' && !isMember && !isCaptain"
          type="button"
          class="rounded-md bg-white px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-zinc-200"
          @click="requestJoin()"
        >
          Solicitar unirse
        </button>

        <button
          v-if="auth.isAuthed && isMember && !isCaptain && team.status !== 'DISBANDED'"
          type="button"
          class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 hover:bg-zinc-900"
          @click="leaveTeam()"
        >
          Salir del equipo
        </button>

        <button
          v-if="auth.isAuthed && isCaptain && soleCaptain && team.status !== 'DISBANDED'"
          type="button"
          class="rounded-md border border-rose-900/40 bg-rose-950/40 px-3 py-2 text-sm text-rose-100 hover:bg-rose-950/60"
          @click="disbandTeam()"
        >
          Disolver equipo
        </button>
      </div>
    </div>

    <p v-if="localError" class="text-sm text-rose-300">{{ localError }}</p>

    <section class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
      <h2 class="text-sm font-semibold text-zinc-200">Logo</h2>
      <div class="mt-4 flex flex-wrap items-center gap-4">
        <img :src="team.logoUrl" alt="logo" class="h-16 w-16 rounded-md border border-zinc-800 object-cover" />

        <div v-if="isCaptain && (team.status === 'PENDING' || team.status === 'APPROVED')" class="flex flex-col gap-2">
          <input ref="logoFile" type="file" accept=".jpg,.jpeg,.png,.webp" class="text-xs text-zinc-300" />
          <div class="flex flex-wrap gap-2">
            <button
              type="button"
              class="rounded-md bg-white px-3 py-2 text-xs font-medium text-zinc-950 hover:bg-zinc-200"
              @click="uploadLogo()"
            >
              Subir logo
            </button>
            <button
              type="button"
              class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs text-zinc-100 hover:bg-zinc-900"
              @click="resetLogoCaptain()"
            >
              Reset logo
            </button>
          </div>
          <p class="text-xs text-zinc-500">Máx 2MB. Solo JPG/PNG/WEBP.</p>
        </div>
      </div>
    </section>

    <section
      v-if="team.status === 'APPROVED'"
      class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5"
    >
      <h2 class="text-sm font-semibold text-zinc-200">Presencia pública · torneos BON e-sports</h2>
      <div class="mt-3 space-y-3 text-sm text-zinc-300">
        <div v-if="team.competitionSummaryOrNull">
          <dl class="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <dt class="text-xs text-zinc-500">Torneos con ficha aprobada</dt>
              <dd class="font-mono">{{ team.competitionSummaryOrNull.tournamentsWithApprovedEntry }}</dd>
            </div>
            <div>
              <dt class="text-xs text-zinc-500">Partidas de bracket (G / P)</dt>
              <dd class="font-mono">{{ team.competitionSummaryOrNull.bracketWins }} / {{ team.competitionSummaryOrNull.bracketLosses }}</dd>
            </div>
            <div>
              <dt class="text-xs text-zinc-500">Partidas con resultado</dt>
              <dd class="font-mono">
                {{
                  team.competitionSummaryOrNull.bracketWins + team.competitionSummaryOrNull.bracketLosses
                }}
              </dd>
            </div>
            <div>
              <dt class="text-xs text-zinc-500">Winrate (por partida)</dt>
              <dd class="font-mono">{{ team.competitionSummaryOrNull.winRatePct == null ? '—' : `${team.competitionSummaryOrNull.winRatePct}%` }}</dd>
            </div>
          </dl>
          <p class="mt-2 text-[11px] leading-relaxed text-zinc-500">
            Un mismo torneo puede sumar varias partidas en el bracket (semifinal, final, etc.). Por eso el conteo de
            torneos no tiene por qué coincidir con el número de victorias.
          </p>
        </div>

        <div
          v-if="
            collectiveStats &&
            (collectiveStats.attributedCompletedMatchesWithStats > 0 ||
              collectiveStats.valorant.playerRows > 0 ||
              collectiveStats.fortnite.playerRows > 0 ||
              collectiveStats.mlb.playerRows > 0)
          "
          class="border-t border-zinc-800 pt-3"
        >
          <p class="text-xs font-semibold text-zinc-400">Estadísticas de partidos (admin) · roster {{ collectiveStats.attributedCompletedMatchesWithStats }} partidos con archivo</p>
          <dl v-if="collectiveStats.valorant.playerRows > 0" class="mt-2 grid gap-2 text-xs sm:grid-cols-3">
            <div>
              <dt class="text-zinc-600">Valorant · filas</dt>
              <dd class="font-mono text-zinc-100">{{ collectiveStats.valorant.playerRows }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">KDA medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmtStat(collectiveStats.valorant.avgKda) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">K/D/A Σ</dt>
              <dd class="font-mono text-zinc-100">
                {{ collectiveStats.valorant.kills }} / {{ collectiveStats.valorant.deaths }} / {{ collectiveStats.valorant.assists }}
              </dd>
            </div>
            <div v-if="collectiveStats.valorant.avgHsPct != null" class="sm:col-span-3">
              <dt class="text-zinc-600">HS% medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmtStat(collectiveStats.valorant.avgHsPct) }}</dd>
            </div>
          </dl>
          <dl v-if="collectiveStats.fortnite.playerRows > 0" class="mt-2 grid gap-2 text-xs sm:grid-cols-3">
            <div>
              <dt class="text-zinc-600">Fortnite · filas</dt>
              <dd class="font-mono text-zinc-100">{{ collectiveStats.fortnite.playerRows }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">KD agreg.</dt>
              <dd class="font-mono text-zinc-100">{{ fmtStat(collectiveStats.fortnite.killsPerDeathOrNull) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">Kills Σ / Deaths Σ</dt>
              <dd class="font-mono text-zinc-100">{{ collectiveStats.fortnite.kills }} / {{ collectiveStats.fortnite.deaths }}</dd>
            </div>
            <div v-if="collectiveStats.fortnite.avgPlacementOrNull != null">
              <dt class="text-zinc-600">Placement medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmtStat(collectiveStats.fortnite.avgPlacementOrNull) }}</dd>
            </div>
          </dl>
          <dl v-if="collectiveStats.mlb.playerRows > 0" class="mt-2 grid gap-2 text-xs sm:grid-cols-3">
            <div>
              <dt class="text-zinc-600">MLB · filas</dt>
              <dd class="font-mono text-zinc-100">{{ collectiveStats.mlb.playerRows }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">AVG partido medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmtStat(collectiveStats.mlb.avgBattingAvgGame) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">HR Σ</dt>
              <dd class="font-mono text-zinc-100">{{ collectiveStats.mlb.homeRunsSum }}</dd>
            </div>
            <div v-if="collectiveStats.mlb.avgInningsPitched != null">
              <dt class="text-zinc-600">IP medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmtStat(collectiveStats.mlb.avgInningsPitched) }}</dd>
            </div>
            <div v-if="collectiveStats.mlb.avgEraGame != null">
              <dt class="text-zinc-600">ERA medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmtStat(collectiveStats.mlb.avgEraGame) }}</dd>
            </div>
          </dl>
        </div>

        <div v-if="(team.canonicalStreamUrl ?? '').trim().length > 0">
          <p class="text-xs text-zinc-500">Stream del equipo</p>
          <a
            :href="team.canonicalStreamUrl ?? '#'"
            class="break-all text-sky-400 hover:underline"
            target="_blank"
            rel="noopener noreferrer"
          >{{ team.canonicalStreamUrl }}</a>
        </div>

        <div v-if="team.sponsorLines?.length">
          <p class="text-xs text-zinc-500">Patrocinio / sponsors</p>
          <ul class="mt-1 space-y-1 text-zinc-200">
            <li v-for="(line, ix) in team.sponsorLines" :key="ix" class="text-sm">{{ line }}</li>
          </ul>
        </div>
      </div>
    </section>

    <section
      v-if="team.status === 'APPROVED' && trophies.length > 0"
      class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5"
    >
      <h2 class="text-sm font-semibold text-zinc-200">Palmarés del equipo</h2>
      <ul class="mt-3 space-y-2 text-sm text-zinc-300">
        <li
          v-for="t in trophies"
          :key="t.id"
          class="flex flex-wrap items-baseline gap-2 border-b border-zinc-800/70 pb-2 last:border-b-0 last:pb-0"
        >
          <span class="rounded bg-amber-950/50 px-1.5 py-0.5 font-mono text-[11px] text-amber-200">{{ t.game }}</span>
          <span class="text-zinc-100">{{ t.badgeLabel }}</span>
          <span class="text-zinc-500">—</span>
          <RouterLink class="text-sky-400 hover:underline" :to="`/tournaments/${t.tournamentId}`">{{ t.tournamentName }}</RouterLink>
        </li>
      </ul>
    </section>

    <section v-if="isCaptain && (team.status === 'PENDING' || team.status === 'APPROVED')" class="rounded-xl border border-amber-900/35 bg-amber-950/15 p-5">
      <h2 class="text-sm font-semibold text-amber-100">Capitán · sponsors & stream público</h2>
      <p class="mt-2 text-xs text-zinc-500">Un sponsor por línea (máx. 15). Visible una vez que el equipo quede APPROVED (salvo vista interna).</p>
      <label class="mt-3 block text-sm">
        <span class="text-zinc-400">Sponsors</span>
        <textarea
          v-model="sponsorDraft"
          rows="4"
          class="mt-1 w-full resize-y rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs outline-none focus:border-zinc-600"
        />
      </label>
      <label class="mt-3 block text-sm">
        <span class="text-zinc-400">URL stream oficial</span>
        <input
          v-model="streamDraft"
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs outline-none focus:border-zinc-600"
        />
      </label>
      <button
        type="button"
        class="mt-3 rounded-md bg-amber-500 px-4 py-2 text-xs font-semibold text-zinc-950 hover:bg-amber-400 disabled:opacity-40"
        :disabled="commercialBusy"
        @click="saveCommercial()"
      >
        Guardar
      </button>
    </section>

    <section v-if="isCaptainView" class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
      <h2 class="text-sm font-semibold text-zinc-200">Miembros</h2>
      <ul class="mt-3 space-y-2 text-sm text-zinc-200">
        <li v-for="(u, idx) in (team as TeamCaptainView).memberUsernames" :key="idx" class="font-mono text-xs text-zinc-300">
          {{ u }}
        </li>
      </ul>
    </section>

    <section v-if="isCaptain" class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
      <h2 class="text-sm font-semibold text-zinc-200">Solicitudes pendientes</h2>
      <div v-if="joinRequests.length === 0" class="mt-3 text-sm text-zinc-500">Sin solicitudes.</div>
      <div v-else class="mt-4 space-y-2">
        <div
          v-for="r in joinRequests"
          :key="r.id"
          class="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-zinc-800 bg-zinc-950/40 px-3 py-2"
        >
          <div class="text-sm text-zinc-200">
            <span class="font-mono text-xs text-zinc-400">{{ r.requesterUsername }}</span>
          </div>
          <div class="flex gap-2">
            <button
              type="button"
              class="rounded-md bg-emerald-400 px-3 py-1.5 text-xs font-medium text-zinc-950 hover:bg-emerald-300"
              @click="accept(r.id)"
            >
              Aceptar
            </button>
            <button
              type="button"
              class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-1.5 text-xs text-zinc-100 hover:bg-zinc-900"
              @click="reject(r.id)"
            >
              Rechazar
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
