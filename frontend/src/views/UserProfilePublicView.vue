<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { usePlatformStore } from '../stores/platform'
import type { TrophyAward, UserPlatformSnapshot } from '../stores/platform'

const route = useRoute()
const platform = usePlatformStore()
const snapshot = ref<UserPlatformSnapshot | null>(null)
const trophies = ref<TrophyAward[]>([])
const err = ref<string | null>(null)

const userId = computed(() => String(route.params.userId))

const sheet = computed(() => snapshot.value?.publicSheet ?? null)

onMounted(async () => {
  err.value = null
  try {
    const [snap, tr] = await Promise.all([
      platform.fetchUserSnapshot(userId.value),
      platform.fetchUserTrophies(userId.value),
    ])
    snapshot.value = snap
    trophies.value = tr
  } catch (e) {
    err.value = e instanceof Error ? e.message : 'No se pudo cargar'
  }
})

function fmt(n: number | null | undefined): string {
  if (n == null || Number.isNaN(n)) return '—'
  return Number.isInteger(n) ? String(n) : n.toFixed(2)
}

function rankEntries(): string[] {
  const m = sheet.value?.rankLabelsByGame ?? {}
  return Object.entries(m).map(([k, v]) => `${k}: ${v}`)
}
</script>

<template>
  <div class="space-y-6">
    <RouterLink to="/leaderboards" class="text-sm text-zinc-400 hover:text-zinc-200">← Rankings por juego</RouterLink>

    <p v-if="err" class="text-sm text-rose-300">{{ err }}</p>

    <div v-if="snapshot" class="space-y-6">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight text-zinc-100">
          {{ snapshot.nickname?.trim() ? snapshot.nickname : snapshot.username }}
        </h1>
        <p class="mt-1 font-mono text-sm text-zinc-500">@{{ snapshot.username }}</p>
        <p v-if="sheet?.leonPlayerNumber != null" class="mt-1 text-xs text-zinc-500">
          Jugador <span class="font-mono text-zinc-400">#{{ sheet.leonPlayerNumber }}</span>
        </p>
      </div>

      <section v-if="sheet" class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
        <h2 class="text-sm font-semibold text-zinc-200">Ficha pública</h2>
        <dl class="mt-4 grid gap-3 text-sm sm:grid-cols-2">
          <div v-if="sheet.country">
            <dt class="text-zinc-500">País / región</dt>
            <dd class="mt-1 text-zinc-100">{{ sheet.country }}</dd>
          </div>
          <div v-if="sheet.publicFullNameOrNull">
            <dt class="text-zinc-500">Nombre</dt>
            <dd class="mt-1 text-zinc-100">{{ sheet.publicFullNameOrNull }}</dd>
          </div>
          <div v-if="sheet.preferredGame">
            <dt class="text-zinc-500">Juego principal</dt>
            <dd class="mt-1 font-mono text-zinc-100">{{ sheet.preferredGame }}</dd>
          </div>
        </dl>

        <div v-if="rankEntries().length > 0" class="mt-4 border-t border-zinc-800 pt-4">
          <div class="text-xs font-semibold uppercase tracking-wide text-zinc-400">Rangos declarados</div>
          <ul class="mt-2 space-y-1 font-mono text-xs text-emerald-200/95">
            <li v-for="r in rankEntries()" :key="r">{{ r }}</li>
          </ul>
        </div>

        <div class="mt-4 flex flex-wrap gap-3 border-t border-zinc-800 pt-4">
          <a
            v-if="sheet.socialLinks.twitchUrl"
            :href="sheet.socialLinks.twitchUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="text-xs text-sky-300 hover:underline"
            >Twitch</a
          >
          <a
            v-if="sheet.socialLinks.youtubeUrl"
            :href="sheet.socialLinks.youtubeUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="text-xs text-sky-300 hover:underline"
            >YouTube</a
          >
          <a
            v-if="sheet.socialLinks.xUrl"
            :href="sheet.socialLinks.xUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="text-xs text-sky-300 hover:underline"
            >X</a
          >
          <a
            v-if="sheet.socialLinks.instagramUrl"
            :href="sheet.socialLinks.instagramUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="text-xs text-sky-300 hover:underline"
            >Instagram</a
          >
          <span v-if="sheet.socialLinks.discord" class="text-xs text-zinc-400">Discord: {{ sheet.socialLinks.discord }}</span>
        </div>

        <div v-if="sheet.approvedTeamAffiliations.length > 0" class="mt-4 border-t border-zinc-800 pt-4">
          <div class="text-xs font-semibold uppercase tracking-wide text-zinc-400">Equipos</div>
          <ul class="mt-2 space-y-2 text-sm">
            <li v-for="t in sheet.approvedTeamAffiliations" :key="t.teamId" class="flex flex-wrap items-baseline gap-2">
              <RouterLink class="font-medium text-sky-300 hover:underline" :to="`/teams/${t.teamId}`">{{ t.name }} [{{ t.tag }}]</RouterLink>
              <span v-if="t.captain" class="rounded bg-amber-950/50 px-1.5 py-0.5 text-[10px] font-semibold uppercase text-amber-200">Capitán</span>
            </li>
          </ul>
        </div>
      </section>

      <section v-if="trophies.length > 0" class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
        <h2 class="text-sm font-semibold text-zinc-200">Palmarés (torneos cerrados)</h2>
        <ul class="mt-3 space-y-2 text-sm">
          <li v-for="t in trophies" :key="t.id" class="flex flex-wrap items-baseline gap-2 border-b border-zinc-800/70 pb-2 last:border-b-0 last:pb-0">
            <span class="rounded bg-amber-950/50 px-1.5 py-0.5 font-mono text-[11px] text-amber-200">{{ t.game }}</span>
            <span class="text-zinc-100">{{ t.badgeLabel }}</span>
            <span class="text-zinc-500">—</span>
            <RouterLink class="text-sky-300 hover:underline" :to="`/tournaments/${t.tournamentId}`">{{ t.tournamentName }}</RouterLink>
          </li>
        </ul>
      </section>

      <p class="max-w-2xl text-sm text-zinc-400">
        Torneos y estadísticas cargadas en BON e-sports (bracket oficial + números por partida que registra el administrador).
      </p>

      <section v-for="g in snapshot.games" :key="g.game" class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
        <h2 class="text-sm font-semibold text-zinc-200">{{ g.game }}</h2>
        <dl class="mt-4 grid gap-3 text-sm sm:grid-cols-2">
          <div>
            <dt class="text-zinc-500">Torneos (aprobadas)</dt>
            <dd class="mt-1 font-mono text-zinc-100">{{ g.tournamentsEnteredApproved }}</dd>
          </div>
          <div>
            <dt class="text-zinc-500">W / L (bracket)</dt>
            <dd class="mt-1 font-mono text-zinc-100">{{ g.bracketMatchWins }} / {{ g.bracketMatchLosses }}</dd>
          </div>
          <div v-if="g.bracketWinRatePctApprox != null">
            <dt class="text-zinc-500">Winrate bracket (approx)</dt>
            <dd class="mt-1 font-mono text-zinc-100">{{ fmt(g.bracketWinRatePctApprox) }}%</dd>
          </div>
        </dl>

        <div v-if="g.game === 'VALORANT'" class="mt-4 border-t border-zinc-800 pt-4 text-sm">
          <p class="text-xs text-zinc-500">Stats de partido (admin)</p>
          <dl class="mt-2 grid gap-2 sm:grid-cols-2">
            <div>
              <dt class="text-zinc-600">Filas muestra</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.valorantStatsSamples ?? null) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">KDA medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.avgValorantKda ?? null) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">HS%</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.avgValorantHeadshotPct ?? null) }}</dd>
            </div>
          </dl>
        </div>

        <div v-else-if="g.game === 'FORTNITE'" class="mt-4 border-t border-zinc-800 pt-4 text-sm">
          <dl class="mt-2 grid gap-2 sm:grid-cols-2">
            <div>
              <dt class="text-zinc-600">Filas muestra</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.fortniteStatsSamples ?? null) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">Kills / partido</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.avgFortniteKillsPerMatch ?? null) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">KD proxy medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.avgFortniteKd ?? null) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">Placement medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.avgFortnitePlacement ?? null) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">Victorias (placement 1)</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.fortniteRoyaleVictoryMatches ?? null) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">Top 10 estimado</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.fortniteTop10Matches ?? null) }}</dd>
            </div>
            <div v-if="g.fortniteDominantModePlayed" class="sm:col-span-2">
              <dt class="text-zinc-600">Modo con más partidas</dt>
              <dd class="font-mono text-zinc-100">{{ g.fortniteDominantModePlayed }}</dd>
            </div>
          </dl>
        </div>

        <div v-else-if="g.game === 'MLB'" class="mt-4 border-t border-zinc-800 pt-4 text-sm">
          <dl class="mt-2 grid gap-2 sm:grid-cols-2">
            <div>
              <dt class="text-zinc-600">Filas muestra</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.mlbStatsSamples ?? null) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">AVG partido medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.avgMlbBattingAvgGame ?? null) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">HR / partido medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.avgMlbHomeRunsGame ?? null) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">IP medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.avgMlbInningsPitchedGame ?? null) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">ERA medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.avgMlbEraGame ?? null) }}</dd>
            </div>
            <div>
              <dt class="text-zinc-600">Runs allowed medio</dt>
              <dd class="font-mono text-zinc-100">{{ fmt(g.avgMlbRunsAllowedGame ?? null) }}</dd>
            </div>
          </dl>
        </div>
      </section>
    </div>
  </div>
</template>
