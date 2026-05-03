<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import type { Client, IMessage, StompSubscription } from '@stomp/stompjs'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { coerceBearerToken } from '../lib/api'
import { formatDateTimeShort } from '../lib/format'
import { createSockJsStompClient } from '../lib/stompSockJs'
import { LEONBON_TOKEN_STORAGE_KEY, useAuthStore } from '../stores/auth'
import { useBetsStore, type Bet } from '../stores/bets'
import { useTeamsStore, type CaptainTeamSummary, type TeamCaptainView } from '../stores/teams'
import {
  useTournamentsStore,
  entryParticipantLabel,
  rosterSizeForGame,
  type BracketMatch,
  type BracketMatchStats,
  type Tournament,
  type TournamentEntry,
} from '../stores/tournaments'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const teams = useTeamsStore()
const tournaments = useTournamentsStore()
const bets = useBetsStore()

const tournamentId = computed(() => String(route.params.tournamentId))

const tournament = ref<Tournament | null>(null)
const entries = ref<TournamentEntry[]>([])
const matches = ref<BracketMatch[]>([])
const myBets = ref<Bet[]>([])
const betAmount = ref<Record<string, number>>({})
const betBusy = ref<Record<string, boolean>>({})
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

/** Columnas de la tabla bracket: Tabla … Apuesta (+ Stats opcional público), + Admin. */
const bracketTableColSpan = computed(() => (isAdmin.value ? 10 : 9))

type MatchBetBoardWsPayload = {
  tournamentId: string
  matchId: string
  stakeOnEntryA: number
  stakeOnEntryB: number
  impliedReturnPerCoinOnA: number | null
  impliedReturnPerCoinOnB: number | null
  bettingClosesAt: string | null
  bettingWindowMinutes: number
}

const adminEntryBusy = ref<string | null>(null)
const adminBracketBusy = ref(false)
const adminBettingMatchId = ref<string | null>(null)
const statsSaveBusy = ref(false)
const statsByMatchId = ref<Record<string, BracketMatchStats | undefined>>({})
const statsEditMatchId = ref<string | null>(null)

type ValStatFormRow = {
  userId: string
  kda: string
  kills: string
  deaths: string
  assists: string
  headshotPct: string
}
type FnStatFormRow = {
  userId: string
  kills: string
  deaths: string
  placement: string
  modePlayed: string
}
type MlbStatFormRow = {
  userId: string
  battingAvgGame: string
  homeRunsGame: string
  inningsPitchedGame: string
  eraGame: string
  runsAllowedGame: string
}
type StatsFormState =
  | { kind: 'VALORANT'; rows: ValStatFormRow[] }
  | { kind: 'FORTNITE'; rows: FnStatFormRow[] }
  | { kind: 'MLB'; rows: MlbStatFormRow[] }

const statsFormState = ref<StatsFormState | null>(null)

const statsInputCls =
  'w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1 text-[11px] text-zinc-100 placeholder:text-zinc-600'

const registrationOpen = computed(() => tournament.value?.lifecycleStatus === 'REGISTRATION_OPEN')
const canBuildBracket = computed(
  () =>
    isAdmin.value &&
    tournament.value?.lifecycleStatus === 'REGISTRATION_CLOSED' &&
    !tournament.value?.bracketSize,
)

/** Misma lógica que el backend: cerrado, sin partidas y sin bracketSize. */
const canReopenRegistration = computed(
  () =>
    isAdmin.value &&
    tournament.value?.lifecycleStatus === 'REGISTRATION_CLOSED' &&
    matches.value.length === 0 &&
    !tournament.value?.bracketSize,
)

/** Solo si aún no hay calendario (evita borrar torneos en curso). */
const canDeleteTournament = computed(() => isAdmin.value && matches.value.length === 0)

const prizeLcByPlacementPreview = computed(() => {
  const t = tournament.value
  if (!t) return [] as Array<{ rank: number; lc: number }>
  const slots = t.prizeWinnerSlots ?? 0
  const arr = t.prizeLeonCoinsByPlacement ?? []
  if (slots <= 0 || !arr.length) return []
  const rows: Array<{ rank: number; lc: number }> = []
  const max = Math.min(slots, arr.length)
  for (let i = 0; i < max; i++) {
    rows.push({ rank: i + 1, lc: Math.floor(Number(arr[i] ?? 0)) })
  }
  return rows
})

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

function rosterUserIdsForBracketMatch(m: BracketMatch): string[] {
  const ea = entries.value.find((e) => e.id === m.entryIdA)
  const eb = entries.value.find((e) => e.id === m.entryIdB)
  if (!ea || !eb) return []
  const g = tournament.value?.game
  if (g === 'MLB') {
    const p = [ea.playerId, eb.playerId].filter((x): x is string => Boolean(x?.trim()))
    return [...new Set(p)]
  }
  const a = ea.selectedRosterUserIds ?? []
  const b = eb.selectedRosterUserIds ?? []
  return [...new Set([...a, ...b].filter(Boolean))]
}

function numStrFromStat(v: unknown): string {
  if (v == null) return ''
  return String(v)
}

function userDisplayForStatRow(userId: string, m: BracketMatch): string {
  const ea = entries.value.find((e) => e.id === m.entryIdA)
  const eb = entries.value.find((e) => e.id === m.entryIdB)
  const short = () => `…${userId.slice(-6)}`
  for (const e of [ea, eb]) {
    if (!e) continue
    if (e.type === 'PLAYER' && e.playerId === userId) {
      return e.playerUsername?.trim() || short()
    }
    if (e.type === 'TEAM' && (e.selectedRosterUserIds ?? []).includes(userId)) {
      const base = entryParticipantLabel(e).trim()
      return base.length ? `${base} · …${userId.slice(-6)}` : short()
    }
  }
  return short()
}

function statsUserIdsOrdered(m: BracketMatch): string[] {
  const fromRoster = rosterUserIdsForBracketMatch(m)
  if (fromRoster.length > 0) return fromRoster
  const s = statsByMatchId.value[m.id]
  if (!s) return []
  const g = tournament.value?.game
  if (g === 'MLB') return (s.mlbPlayers ?? []).map((x) => String(x.userId)).filter(Boolean)
  if (g === 'FORTNITE') return (s.fortnitePlayers ?? []).map((x) => String(x.userId)).filter(Boolean)
  return (s.valorantPlayers ?? []).map((x) => String(x.userId)).filter(Boolean)
}

function emptyValRow(userId: string): ValStatFormRow {
  return { userId, kda: '', kills: '', deaths: '', assists: '', headshotPct: '' }
}

function emptyFnRow(userId: string): FnStatFormRow {
  return { userId, kills: '', deaths: '', placement: '', modePlayed: '' }
}

function emptyMlbRow(userId: string): MlbStatFormRow {
  return { userId, battingAvgGame: '', homeRunsGame: '', inningsPitchedGame: '', eraGame: '', runsAllowedGame: '' }
}

function populateStatsFormFromMatch(m: BracketMatch): void {
  const existing = statsByMatchId.value[m.id]
  const game = tournament.value?.game ?? 'VALORANT'
  const ids = statsUserIdsOrdered(m)
  if (ids.length === 0) {
    localError.value = 'No hay jugadores/roster cargado para este partido.'
    statsEditMatchId.value = null
    statsFormState.value = null
    return
  }
  localError.value = null
  statsEditMatchId.value = m.id

  if (game === 'MLB') {
    const map = new Map<string, Record<string, unknown>>()
    for (const r of existing?.mlbPlayers ?? []) map.set(String(r.userId), r)
    statsFormState.value = {
      kind: 'MLB',
      rows: ids.map((uid) => {
        const r = map.get(uid)
        const row = emptyMlbRow(uid)
        if (!r) return row
        row.battingAvgGame = numStrFromStat(r.battingAvgGame)
        row.homeRunsGame = numStrFromStat(r.homeRunsGame)
        row.inningsPitchedGame = numStrFromStat(r.inningsPitchedGame)
        row.eraGame = numStrFromStat(r.eraGame)
        row.runsAllowedGame = numStrFromStat(r.runsAllowedGame)
        return row
      }),
    }
    return
  }
  if (game === 'FORTNITE') {
    const map = new Map<string, Record<string, unknown>>()
    for (const r of existing?.fortnitePlayers ?? []) map.set(String(r.userId), r)
    statsFormState.value = {
      kind: 'FORTNITE',
      rows: ids.map((uid) => {
        const r = map.get(uid)
        const row = emptyFnRow(uid)
        if (!r) return row
        row.kills = numStrFromStat(r.kills)
        row.deaths = numStrFromStat(r.deaths)
        row.placement = numStrFromStat(r.placement)
        row.modePlayed = r.modePlayed != null ? String(r.modePlayed) : ''
        return row
      }),
    }
    return
  }
  const map = new Map<string, Record<string, unknown>>()
  for (const r of existing?.valorantPlayers ?? []) map.set(String(r.userId), r)
  statsFormState.value = {
    kind: 'VALORANT',
    rows: ids.map((uid) => {
      const r = map.get(uid)
      const row = emptyValRow(uid)
      if (!r) return row
      row.kda = numStrFromStat(r.kda)
      row.kills = numStrFromStat(r.kills)
      row.deaths = numStrFromStat(r.deaths)
      row.assists = numStrFromStat(r.assists)
      row.headshotPct = numStrFromStat(r.headshotPct)
      return row
    }),
  }
}

async function hydrateMatchStats() {
  statsByMatchId.value = {}
  const t = tournament.value
  if (!t) return
  const tid = t.id
  await Promise.all(
    matches.value
      .filter((m) => m.status === 'COMPLETE')
      .map(async (m) => {
        try {
          const s = await tournaments.getMatchStats(tid, m.id)
          statsByMatchId.value = { ...statsByMatchId.value, [m.id]: s }
        } catch {
          /* sin stats registradas → 404 */
        }
      }),
  )
}

function openStatsEditor(m: BracketMatch) {
  populateStatsFormFromMatch(m)
}

function closeStatsEditor() {
  statsEditMatchId.value = null
  statsFormState.value = null
}

function optNullableInt(raw: string, label: string): number | null {
  const t = raw.trim()
  if (!t) return null
  const n = Number(t)
  if (!Number.isFinite(n) || !Number.isInteger(n)) {
    throw new Error(`${label}: entero inválido`)
  }
  return n
}

function optNullableDouble(raw: string, label: string): number | null {
  const t = raw.trim()
  if (!t) return null
  const n = Number(t)
  if (!Number.isFinite(n)) {
    throw new Error(`${label}: número inválido`)
  }
  return n
}

function statsPayloadFromForm(): Record<string, unknown> | null {
  const st = statsFormState.value
  if (!st) return null
  if (st.kind === 'VALORANT') {
    return {
      valorantPlayers: st.rows.map((r) => ({
        userId: r.userId.trim(),
        kda: optNullableDouble(r.kda, `Valorant · KDA (${r.userId.slice(-6)})`),
        kills: optNullableInt(r.kills, 'Bajas'),
        deaths: optNullableInt(r.deaths, 'Muertes'),
        assists: optNullableInt(r.assists, 'Asistencias'),
        headshotPct: optNullableDouble(r.headshotPct, '% headshot'),
      })),
    }
  }
  if (st.kind === 'FORTNITE') {
    return {
      fortnitePlayers: st.rows.map((r) => ({
        userId: r.userId.trim(),
        kills: optNullableInt(r.kills, 'Bajas'),
        deaths: optNullableInt(r.deaths, 'Muertes'),
        placement: optNullableInt(r.placement, 'Puesto'),
        modePlayed: r.modePlayed.trim() || null,
      })),
    }
  }
  return {
    mlbPlayers: st.rows.map((r) => ({
      userId: r.userId.trim(),
      battingAvgGame: optNullableDouble(r.battingAvgGame, 'AVG'),
      homeRunsGame: optNullableInt(r.homeRunsGame, 'HR'),
      inningsPitchedGame: optNullableDouble(r.inningsPitchedGame, 'IP'),
      eraGame: optNullableDouble(r.eraGame, 'ERA'),
      runsAllowedGame: optNullableInt(r.runsAllowedGame, 'Carreras permitidas'),
    })),
  }
}

async function saveMatchStatsDraft(m: BracketMatch) {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  let parsed: Record<string, unknown>
  try {
    const body = statsPayloadFromForm()
    if (!body) {
      localError.value = 'Formulario de estadísticas no cargado.'
      return
    }
    parsed = body
  } catch (err) {
    localError.value = err instanceof Error ? err.message : 'Revisa los números del formulario.'
    return
  }
  statsSaveBusy.value = true
  localError.value = null
  successMsg.value = null
  try {
    const out = await tournaments.upsertMatchStatsAdmin(bearer, tournament.value.id, m.id, parsed)
    const key = tournament.value.game === 'MLB' ? 'mlbPlayers' : tournament.value.game === 'FORTNITE' ? 'fortnitePlayers' : 'valorantPlayers'
    const empty = Array.isArray(parsed[key]) && (parsed[key] as unknown[]).length === 0
    if (empty) {
      successMsg.value = 'Estadísticas eliminadas.'
      statsByMatchId.value = { ...statsByMatchId.value, [m.id]: undefined }
    } else if (out) {
      successMsg.value = 'Estadísticas guardadas.'
      statsByMatchId.value = { ...statsByMatchId.value, [m.id]: out }
    } else {
      successMsg.value = 'Estadísticas guardadas.'
    }
    closeStatsEditor()
    await hydrateMatchStats()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error al guardar stats'
  } finally {
    statsSaveBusy.value = false
  }
}

async function deleteMatchStats(m: BracketMatch) {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  const g = tournament.value.game
  const body =
    g === 'MLB'
      ? { mlbPlayers: [] }
      : g === 'FORTNITE'
        ? { fortnitePlayers: [] }
        : { valorantPlayers: [] }
  statsSaveBusy.value = true
  localError.value = null
  successMsg.value = null
  try {
    await tournaments.upsertMatchStatsAdmin(bearer, tournament.value.id, m.id, body)
    successMsg.value = 'Estadísticas eliminadas.'
    statsByMatchId.value = { ...statsByMatchId.value, [m.id]: undefined }
    closeStatsEditor()
    await hydrateMatchStats()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error al borrar stats'
  } finally {
    statsSaveBusy.value = false
  }
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
      myBets.value = await bets.listMyBets(bearer, { tournamentId: id, limit: 200 })
    } catch (e) {
      myBets.value = []
      const msg = e instanceof Error ? e.message : ''
      // do not block the rest of the page for bets errors
      if (msg) localError.value = msg
    }
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
    myBets.value = []
  }

  await hydrateMatchStats()
}

const myBetByMatchId = computed(() => {
  const m = new Map<string, Bet>()
  for (const b of myBets.value) {
    if (!m.has(b.matchId)) m.set(b.matchId, b)
  }
  return m
})

function canBetOnMatch(m: BracketMatch): boolean {
  if (!auth.isAuthed || m.status !== 'READY' || !m.entryIdA || !m.entryIdB || myBetByMatchId.value.has(m.id)) {
    return false
  }
  if (!m.bettingClosesAt) {
    return false
  }
  const now = Date.now()
  const close = new Date(m.bettingClosesAt).getTime()
  return now < close && Number.isFinite(close)
}

function bettingWindowLabel(m: BracketMatch): string {
  if (m.status !== 'READY') return '—'
  const mins = m.bettingWindowMinutes ?? 5
  if (!m.bettingClosesAt) return `Cerradas · el admin debe abrir (máx. ${mins} min)`
  const closeTs = new Date(m.bettingClosesAt).getTime()
  if (!Number.isFinite(closeTs)) return '—'
  if (Date.now() >= closeTs) return `Cerradas · hasta ${fmt(m.bettingClosesAt)}`
  return `Abiertas → ${fmt(m.bettingClosesAt)} (${mins} min o cierre manual)`
}

function fmtMult(x: number | null | undefined): string {
  if (x == null || !Number.isFinite(x)) return '—'
  return x.toFixed(2) + '×'
}

/** READY pero fuera de ventana u hora sin definir — mensaje corto para la UI de apuestas */
function bettingClosedReason(m: BracketMatch): string | null {
  if (m.status !== 'READY' || !auth.isAuthed || myBetByMatchId.value.has(m.id)) {
    return null
  }
  if (!m.bettingClosesAt) return 'Esperando a que el admin abra la ventana de apuestas.'
  const now = Date.now()
  const close = new Date(m.bettingClosesAt).getTime()
  if (!Number.isFinite(close)) return 'Ventana de apuestas no disponible.'
  if (now >= close) return 'Ventana de apuestas cerrada.'
  return null
}

function bettingWindowActive(m: BracketMatch): boolean {
  if (!m.bettingClosesAt) return false
  const t = new Date(m.bettingClosesAt).getTime()
  return Number.isFinite(t) && Date.now() < t
}

/** Pago total L-Coins si gana tu bando (floor como el backend antes del residual). */
function parimutuelPayoutTotalIfWin(m: BracketMatch, userStake: number, pickedEntryId: string): number | null {
  const a = Math.max(0, Math.floor(Number(m.totalStakeEntryA ?? 0)))
  const b = Math.max(0, Math.floor(Number(m.totalStakeEntryB ?? 0)))
  const pot = a + b
  if (pot <= 0 || userStake <= 0) return null
  if (pickedEntryId === m.entryIdA) {
    if (a <= 0) return null
    return Math.floor((userStake * pot) / a)
  }
  if (pickedEntryId === m.entryIdB) {
    if (b <= 0) return null
    return Math.floor((userStake * pot) / b)
  }
  return null
}

type LockedPayoutPreview = { total: number; net: number; mult: string }

/** Solo cuando la ventana ya cerró y el pozo está fijo (apuesta aún pendiente). */
function lockedPayoutPreview(m: BracketMatch, bet: Bet): LockedPayoutPreview | null {
  if (bet.status !== 'PENDING' || m.status !== 'READY') return null
  if (bettingWindowActive(m)) return null
  const total = parimutuelPayoutTotalIfWin(m, bet.amount, bet.pickedEntryId)
  if (total == null) return null
  const net = total - bet.amount
  const mult = bet.amount > 0 ? (total / bet.amount).toFixed(2) : '—'
  return { total, net, mult }
}

async function openBetWindowAdmin(m: BracketMatch) {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  localError.value = null
  successMsg.value = null
  adminBettingMatchId.value = m.id
  try {
    const row = await tournaments.openBettingWindowAdmin(bearer, tournament.value.id, m.id)
    matches.value = matches.value.map((x) => (x.id === row.id ? { ...x, ...row } : x))
    successMsg.value = 'Ventana de apuestas abierta.'
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'No se pudo abrir apuestas'
  } finally {
    adminBettingMatchId.value = null
  }
}

async function closeBetWindowAdmin(m: BracketMatch) {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  localError.value = null
  successMsg.value = null
  adminBettingMatchId.value = m.id
  try {
    const row = await tournaments.closeBettingWindowAdmin(bearer, tournament.value.id, m.id)
    matches.value = matches.value.map((x) => (x.id === row.id ? { ...x, ...row } : x))
    successMsg.value = 'Ventana de apuestas cerrada.'
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'No se pudo cerrar apuestas'
  } finally {
    adminBettingMatchId.value = null
  }
}

let matchesPoll: ReturnType<typeof setInterval> | null = null

let betStomp: Client | null = null
let betSubs: StompSubscription[] = []

function disconnectBetStomp() {
  for (const s of betSubs) {
    try {
      s.unsubscribe()
    } catch {
      /* ignore */
    }
  }
  betSubs = []
  if (betStomp) {
    void betStomp.deactivate()
    betStomp = null
  }
}

function applyBetBoardWs(p: MatchBetBoardWsPayload) {
  matches.value = matches.value.map((row) => {
    if (row.id !== p.matchId) return row
    return {
      ...row,
      totalStakeEntryA: p.stakeOnEntryA,
      totalStakeEntryB: p.stakeOnEntryB,
      impliedReturnPerCoinOnA: p.impliedReturnPerCoinOnA ?? undefined,
      impliedReturnPerCoinOnB: p.impliedReturnPerCoinOnB ?? undefined,
      bettingClosesAt: p.bettingClosesAt ?? row.bettingClosesAt ?? undefined,
      bettingWindowMinutes: p.bettingWindowMinutes,
    }
  })
}

function syncBetStompSubs() {
  const tid = tournamentId.value
  if (!tid || !betStomp?.connected) return
  for (const s of betSubs) {
    try {
      s.unsubscribe()
    } catch {
      /* ignore */
    }
  }
  betSubs = []
  for (const m of matches.value) {
    const sub = betStomp.subscribe(`/topic/tournaments/${tid}/matches/${m.id}/bets`, (msg: IMessage) => {
      try {
        applyBetBoardWs(JSON.parse(msg.body) as MatchBetBoardWsPayload)
      } catch {
        /* ignore */
      }
    })
    betSubs.push(sub)
  }
}

function connectBetStomp() {
  disconnectBetStomp()
  if (!tournamentId.value) return
  const c = createSockJsStompClient()
  betStomp = c
  c.onConnect = () => syncBetStompSubs()
  c.activate()
}

watch(
  () =>
    `${tournamentId.value}|${matches.value
      .map((x) => x.id)
      .sort()
      .join(',')}`,
  () => syncBetStompSubs(),
)

watch(tournamentId, async () => {
  disconnectBetStomp()
  try {
    await reload()
    connectBetStomp()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
})

async function refreshMatchesOnly() {
  if (!tournamentId.value) return
  try {
    matches.value = await tournaments.listMatches(tournamentId.value)
    await hydrateMatchStats()
  } catch {
    /* ignore poll errors */
  }
}

async function placeBet(match: BracketMatch, pickedEntryId: string) {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  const amt = Number(betAmount.value[match.id] ?? 0)
  if (!Number.isFinite(amt) || amt <= 0) {
    localError.value = 'Monto inválido (mínimo 1).'
    return
  }
  localError.value = null
  successMsg.value = null
  betBusy.value = { ...betBusy.value, [match.id]: true }
  try {
    await bets.placeBet(bearer, { matchId: match.id, pickedEntryId, amount: Math.floor(amt) })
    successMsg.value = 'Apuesta registrada.'
    await auth.refreshAll()
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  } finally {
    betBusy.value = { ...betBusy.value, [match.id]: false }
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
    connectBetStomp()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
  matchesPoll = setInterval(() => {
    if (matches.value.length > 0) void refreshMatchesOnly()
  }, 6000)
})

onUnmounted(() => {
  disconnectBetStomp()
  if (matchesPoll) {
    clearInterval(matchesPoll)
    matchesPoll = null
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
  return formatDateTimeShort(iso)
}

function ordinalSportsRank(rank: number): string {
  if (rank === 1) return '1.er lugar'
  if (rank === 2) return '2.º lugar'
  if (rank === 3) return '3.er lugar'
  return `${rank}.º lugar`
}

function lcDisplayAmount(n: number): string {
  if (!Number.isFinite(n)) return '0'
  return Math.max(0, Math.floor(n)).toLocaleString('es-MX', { maximumFractionDigits: 0 })
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

async function reopenRegistration() {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  localError.value = null
  adminBracketBusy.value = true
  try {
    await tournaments.reopenRegistrationAdmin(bearer, tournament.value.id)
    successMsg.value = 'Inscripciones reabiertas (REGISTRATION_OPEN).'
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  } finally {
    adminBracketBusy.value = false
  }
}

async function deleteTournament() {
  const bearer = resolveBearer()
  if (!bearer || !tournament.value) return
  const ok = window.confirm(
    '¿Eliminar este torneo? Se borrarán también las inscripciones. No se puede deshacer. Solo está permitido si aún no hay partidas de calendario.',
  )
  if (!ok) return
  localError.value = null
  adminBracketBusy.value = true
  try {
    await tournaments.deleteTournamentAdmin(bearer, tournament.value.id)
    await router.push('/admin/tournaments')
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

      <aside
        v-if="tournament.streamUrl"
        class="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-sky-500/35 bg-gradient-to-br from-sky-950/50 to-zinc-950/80 px-4 py-5"
      >
        <div class="min-w-0 flex-1">
          <div class="text-[11px] font-bold uppercase tracking-wider text-sky-300/95">Arena en vivo — match day</div>
          <p class="mt-1 max-w-xl text-sm text-sky-100/90">
            Link destacado para ver la competencia. Ábrelo en otra pestaña mientras ves el bracket aquí.
          </p>
          <div class="mt-2 truncate font-mono text-xs text-zinc-500">{{ tournament.streamUrl }}</div>
        </div>
        <a
          :href="tournament.streamUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="shrink-0 rounded-lg bg-sky-500 px-4 py-2.5 text-sm font-semibold text-sky-950 shadow-lg hover:bg-sky-400"
        >
          Ir al stream →
        </a>
      </aside>

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
        <div v-if="tournament.maxApprovedParticipants != null" class="rounded-lg border border-zinc-800 bg-zinc-900/40 px-3 py-2">
          <dt class="text-zinc-500">Cupo máx. entradas aprobadas</dt>
          <dd class="mt-1 font-mono text-zinc-200">{{ tournament.maxApprovedParticipants }}</dd>
        </div>
        <div v-if="tournament.rulesHtml?.trim()" class="rounded-lg border border-zinc-800 bg-zinc-900/40 px-3 py-2 sm:col-span-2">
          <dt class="text-zinc-500">Reglas</dt>
          <dd class="mt-1 whitespace-pre-wrap text-zinc-300">{{ tournament.rulesHtml }}</dd>
        </div>
        <div v-if="tournament.eligibilityNotes?.trim()" class="rounded-lg border border-zinc-800 bg-zinc-900/40 px-3 py-2 sm:col-span-2">
          <dt class="text-zinc-500">Elegibilidad (referencia)</dt>
          <dd class="mt-1 whitespace-pre-wrap text-zinc-300">{{ tournament.eligibilityNotes }}</dd>
        </div>
        <div
          v-if="prizeLcByPlacementPreview.length > 0"
          class="rounded-lg border border-amber-900/35 bg-amber-950/20 px-3 py-2 sm:col-span-2"
        >
          <dt class="text-xs font-semibold text-amber-200/95">Bolsa declarada por puesto (L-Coins)</dt>
          <dd class="mt-2 space-y-1 text-sm">
            <ul class="divide-y divide-amber-900/25 rounded-md border border-amber-900/30 bg-zinc-950/40">
              <li
                v-for="row in prizeLcByPlacementPreview"
                :key="row.rank"
                class="flex items-center justify-between gap-3 px-3 py-2 font-mono text-zinc-200"
              >
                <span class="text-zinc-400">{{ ordinalSportsRank(row.rank) }}</span>
                <span>{{ lcDisplayAmount(row.lc) }} LC</span>
              </li>
            </ul>
            <p v-if="tournament.placementPrizeLedgerCompletedAt" class="text-xs text-emerald-300">
              Liquidación monetaria por puesto registrada {{ fmt(tournament.placementPrizeLedgerCompletedAt) }}.
            </p>
          </dd>
        </div>
        <div v-if="tournament.prizeNotes?.trim()" class="rounded-lg border border-zinc-800 bg-zinc-900/40 px-3 py-2 sm:col-span-2">
          <dt class="text-zinc-500">Premios</dt>
          <dd class="mt-1 whitespace-pre-wrap text-zinc-300">{{ tournament.prizeNotes }}</dd>
        </div>
      </dl>

      <section v-if="isAdmin" class="rounded-xl border border-amber-900/40 bg-amber-950/20 p-5">
        <h2 class="text-sm font-semibold text-amber-100">Admin — bracket</h2>
        <p class="mt-1 text-xs text-amber-200/80">
          Cierra inscripciones, aprueba al menos 2 entradas, luego genera el calendario. Si cerraste sin querer y aún no hay calendario,
          puedes reabrir inscripciones o eliminar el torneo y crear uno nuevo.
          <span class="font-mono">SINGLE_ELIM</span> y <span class="font-mono">DOUBLE_ELIM</span> (hasta 8 slots en cuadro de
          ganadores). <span class="font-mono">ROUND_ROBIN</span>: todos contra todos; al terminar todas las partidas el torneo pasa a
          <span class="font-mono">COMPLETED</span>. Marca ganador en cada partida <span class="font-mono">READY</span>. Las apuestas son
          parimutuel: solo durante la ventana configurada desde la hora programada del partido (véase la tabla).
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
            v-if="canReopenRegistration"
            type="button"
            class="rounded-md border border-sky-800 bg-sky-950/40 px-3 py-2 text-xs text-sky-100 hover:bg-sky-950/60 disabled:opacity-40"
            :disabled="adminBracketBusy"
            @click="reopenRegistration()"
          >
            Reabrir inscripciones
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
          <button
            v-if="canDeleteTournament"
            type="button"
            class="rounded-md border border-rose-900 bg-rose-950/30 px-3 py-2 text-xs text-rose-200 hover:bg-rose-950/50 disabled:opacity-40"
            :disabled="adminBracketBusy"
            @click="deleteTournament()"
          >
            Eliminar torneo
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
                <th class="px-3 py-2">Apuesta</th>
                <th class="px-3 py-2">Estadísticas</th>
                <th v-if="isAdmin" class="px-3 py-2 text-right">Admin</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="m in matches" :key="m.id">
              <tr class="border-t border-zinc-800 bg-zinc-950/40">
                <td class="px-3 py-2 text-zinc-400">{{ bracketPoolLabel(m.bracketPool ?? 'WB') }}</td>
                <td class="px-3 py-2 font-mono text-zinc-300">{{ m.round }}</td>
                <td class="px-3 py-2 font-mono text-zinc-400">{{ m.indexInRound }}</td>
                <td class="px-3 py-2 text-zinc-200">{{ bracketSlotLabel(m.entryIdA, m) }}</td>
                <td class="px-3 py-2 text-zinc-200">{{ bracketSlotLabel(m.entryIdB, m) }}</td>
                <td class="px-3 py-2">{{ m.status }}</td>
                <td class="px-3 py-2 text-zinc-400">{{ m.winnerEntryId ? entryDisplayForBracket(m.winnerEntryId) : '—' }}</td>
                <td class="px-3 py-2">
                  <template v-if="auth.isAuthed">
                    <template v-if="myBetByMatchId.has(m.id)">
                      <div class="text-[11px] text-zinc-300">
                        <span class="text-zinc-500">Tu apuesta:</span>
                        <span class="font-mono text-zinc-200">{{ myBetByMatchId.get(m.id)!.amount }}</span>
                        <span class="text-zinc-500">a</span>
                        <span class="text-zinc-200">{{ entryDisplayForBracket(myBetByMatchId.get(m.id)!.pickedEntryId) }}</span>
                      </div>
                      <template
                        v-for="lp in [lockedPayoutPreview(m, myBetByMatchId.get(m.id)!)]"
                        :key="`${m.id}-locked-payout`"
                      >
                        <div v-if="lp" class="mt-1 max-w-[14rem] text-[10px] leading-snug text-sky-200/90">
                          <span class="text-zinc-500">Pozo cerrado · </span>
                          si ganas: <span class="font-mono text-sky-100">~{{ lcDisplayAmount(lp.total) }}</span> L-Coins
                          <span class="text-zinc-500"> ({{ lcDisplayAmount(lp.net) }} ganancia, ~{{ lp.mult }}×)</span>
                        </div>
                      </template>
                      <div class="mt-0.5 text-[10px]" :class="myBetByMatchId.get(m.id)!.status === 'WON' ? 'text-emerald-300' : myBetByMatchId.get(m.id)!.status === 'LOST' ? 'text-rose-300' : 'text-zinc-500'">
                        {{ myBetByMatchId.get(m.id)!.status }}
                        <span v-if="myBetByMatchId.get(m.id)!.status === 'WON' && myBetByMatchId.get(m.id)!.payoutAmount != null" class="font-mono">
                          (+{{ myBetByMatchId.get(m.id)!.payoutAmount }})
                        </span>
                      </div>
                    </template>
                    <template v-else-if="canBetOnMatch(m)">
                      <div class="mb-1 space-y-0.5 text-[10px] text-zinc-400">
                        <div>{{ bettingWindowLabel(m) }}</div>
                        <div class="font-mono text-[10px]">
                          Pozo A/B: {{ m.totalStakeEntryA ?? 0 }} · {{ m.totalStakeEntryB ?? 0 }} · mult
                          {{ fmtMult(m.impliedReturnPerCoinOnA) }}/{{ fmtMult(m.impliedReturnPerCoinOnB) }}
                        </div>
                      </div>
                      <div class="flex items-center gap-1.5">
                        <input
                          v-model.number="betAmount[m.id]"
                          type="number"
                          min="1"
                          step="1"
                          placeholder="Monto"
                          class="w-20 rounded border border-zinc-800 bg-zinc-950 px-2 py-1 text-[11px] text-zinc-100"
                        />
                        <button
                          type="button"
                          class="rounded border border-sky-800 px-2 py-1 text-[11px] text-sky-200 hover:bg-sky-950/60 disabled:opacity-40"
                          :disabled="betBusy[m.id]"
                          @click="placeBet(m, m.entryIdA!)"
                        >
                          A
                        </button>
                        <button
                          type="button"
                          class="rounded border border-sky-800 px-2 py-1 text-[11px] text-sky-200 hover:bg-sky-950/60 disabled:opacity-40"
                          :disabled="betBusy[m.id]"
                          @click="placeBet(m, m.entryIdB!)"
                        >
                          B
                        </button>
                      </div>
                      <div class="mt-1 text-[10px] text-zinc-500">
                        Parimutuel (Twitch-style): proporcional al volumen cuando gana tu bando.
                      </div>
                    </template>
                    <template v-else-if="m.status === 'READY' && auth.isAuthed && bettingClosedReason(m)">
                      <div class="max-w-[12rem] text-[10px] text-zinc-500">{{ bettingClosedReason(m) }}</div>
                      <div class="mt-1 font-mono text-[10px] text-zinc-500">
                        A/B {{ m.totalStakeEntryA ?? 0 }} · {{ m.totalStakeEntryB ?? 0 }} · {{ fmtMult(m.impliedReturnPerCoinOnA) }}/{{
                          fmtMult(m.impliedReturnPerCoinOnB)
                        }}
                      </div>
                    </template>
                    <span v-else class="text-zinc-600">—</span>
                  </template>
                  <template v-else>
                    <span class="text-zinc-600">Inicia sesión</span>
                  </template>
                </td>
                <td class="max-w-[7rem] align-top px-3 py-2">
                  <template v-if="m.status === 'COMPLETE'">
                    <div class="font-mono text-[11px]" :class="statsByMatchId[m.id] ? 'text-emerald-200/90' : 'text-zinc-500'">
                      rev {{ statsByMatchId[m.id]?.revision ?? '—' }}
                    </div>
                    <button
                      v-if="isAdmin"
                      type="button"
                      class="mt-1 rounded border border-zinc-700 px-1.5 py-0.5 text-[10px] text-zinc-200 hover:bg-zinc-800"
                      @click="openStatsEditor(m)"
                    >
                      Editar stats
                    </button>
                  </template>
                  <span v-else class="text-zinc-600">—</span>
                </td>
                <td v-if="isAdmin" class="max-w-[9rem] px-3 py-2 text-right align-top">
                  <template v-if="m.status === 'READY' && m.entryIdA && m.entryIdB">
                    <div class="flex flex-col items-end gap-1">
                      <div class="flex flex-wrap justify-end gap-1">
                        <button
                          type="button"
                          class="rounded border border-emerald-800 px-1.5 py-0.5 text-emerald-300 hover:bg-emerald-950/60 disabled:opacity-40"
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
                      </div>
                      <div class="flex flex-wrap justify-end gap-1">
                        <button
                          type="button"
                          class="rounded border border-amber-800/80 bg-amber-950/30 px-1.5 py-0.5 text-[10px] text-amber-100 hover:bg-amber-950/50 disabled:opacity-40"
                          :disabled="adminBettingMatchId === m.id || adminBracketBusy"
                          @click="openBetWindowAdmin(m)"
                        >
                          Abrir apuestas
                        </button>
                        <button
                          type="button"
                          class="rounded border border-zinc-600 px-1.5 py-0.5 text-[10px] text-zinc-300 hover:bg-zinc-800 disabled:opacity-40"
                          :disabled="adminBettingMatchId === m.id || adminBracketBusy || !bettingWindowActive(m)"
                          @click="closeBetWindowAdmin(m)"
                        >
                          Cerrar
                        </button>
                      </div>
                      <span v-if="bettingWindowActive(m)" class="text-[9px] text-zinc-500">
                        Auto-cierre en {{ m.bettingWindowMinutes ?? 5 }} min si no cierras antes
                      </span>
                    </div>
                  </template>
                  <span v-else class="text-zinc-600">—</span>
                </td>
              </tr>
              <tr v-if="isAdmin && statsEditMatchId === m.id && statsFormState" class="border-t border-zinc-800 bg-zinc-950/70">
                <td :colspan="bracketTableColSpan" class="px-3 py-4">
                  <div class="text-[11px] text-zinc-400">
                    Partido terminado —
                    <span class="font-mono text-zinc-300">{{ tournament.game }}</span>
                    · completa una fila por cada jugador/roster del partido. Deja en blanco lo que no aplique.
                  </div>

                  <template v-if="statsFormState.kind === 'VALORANT'">
                    <div
                      v-for="row in statsFormState.rows"
                      :key="'v-' + row.userId"
                      class="mt-3 rounded-lg border border-zinc-800 bg-zinc-900/50 p-3"
                    >
                      <div class="text-xs font-medium text-zinc-200">{{ userDisplayForStatRow(row.userId, m) }}</div>
                      <div class="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-6">
                        <label class="flex flex-col gap-0.5">
                          <span class="text-[10px] text-zinc-500">KDA</span>
                          <input v-model="row.kda" type="text" inputmode="decimal" :class="statsInputCls" placeholder="—" />
                        </label>
                        <label class="flex flex-col gap-0.5">
                          <span class="text-[10px] text-zinc-500">Bajas</span>
                          <input v-model="row.kills" type="text" inputmode="numeric" :class="statsInputCls" placeholder="—" />
                        </label>
                        <label class="flex flex-col gap-0.5">
                          <span class="text-[10px] text-zinc-500">Muertes</span>
                          <input v-model="row.deaths" type="text" inputmode="numeric" :class="statsInputCls" placeholder="—" />
                        </label>
                        <label class="flex flex-col gap-0.5">
                          <span class="text-[10px] text-zinc-500">Asistencias</span>
                          <input v-model="row.assists" type="text" inputmode="numeric" :class="statsInputCls" placeholder="—" />
                        </label>
                        <label class="flex flex-col gap-0.5 sm:col-span-2 lg:col-span-2">
                          <span class="text-[10px] text-zinc-500">Headshot %</span>
                          <input v-model="row.headshotPct" type="text" inputmode="decimal" :class="statsInputCls" placeholder="—" />
                        </label>
                      </div>
                    </div>
                  </template>

                  <template v-else-if="statsFormState.kind === 'FORTNITE'">
                    <div
                      v-for="row in statsFormState.rows"
                      :key="'f-' + row.userId"
                      class="mt-3 rounded-lg border border-zinc-800 bg-zinc-900/50 p-3"
                    >
                      <div class="text-xs font-medium text-zinc-200">{{ userDisplayForStatRow(row.userId, m) }}</div>
                      <div class="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-4">
                        <label class="flex flex-col gap-0.5">
                          <span class="text-[10px] text-zinc-500">Bajas</span>
                          <input v-model="row.kills" type="text" inputmode="numeric" :class="statsInputCls" placeholder="—" />
                        </label>
                        <label class="flex flex-col gap-0.5">
                          <span class="text-[10px] text-zinc-500">Muertes</span>
                          <input v-model="row.deaths" type="text" inputmode="numeric" :class="statsInputCls" placeholder="—" />
                        </label>
                        <label class="flex flex-col gap-0.5">
                          <span class="text-[10px] text-zinc-500">Puesto</span>
                          <input v-model="row.placement" type="text" inputmode="numeric" :class="statsInputCls" placeholder="1 = win BR" />
                        </label>
                        <label class="flex flex-col gap-0.5">
                          <span class="text-[10px] text-zinc-500">Modo</span>
                          <input v-model="row.modePlayed" type="text" :class="statsInputCls" placeholder="Ej. Duo" maxlength="64" />
                        </label>
                      </div>
                    </div>
                  </template>

                  <template v-else-if="statsFormState.kind === 'MLB'">
                    <div
                      v-for="row in statsFormState.rows"
                      :key="'mlb-' + row.userId"
                      class="mt-3 rounded-lg border border-zinc-800 bg-zinc-900/50 p-3"
                    >
                      <div class="text-xs font-medium text-zinc-200">{{ userDisplayForStatRow(row.userId, m) }}</div>
                      <div class="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-5">
                        <label class="flex flex-col gap-0.5">
                          <span class="text-[10px] text-zinc-500">AVG (partido)</span>
                          <input v-model="row.battingAvgGame" type="text" inputmode="decimal" :class="statsInputCls" placeholder="—" />
                        </label>
                        <label class="flex flex-col gap-0.5">
                          <span class="text-[10px] text-zinc-500">Home runs</span>
                          <input v-model="row.homeRunsGame" type="text" inputmode="numeric" :class="statsInputCls" placeholder="—" />
                        </label>
                        <label class="flex flex-col gap-0.5">
                          <span class="text-[10px] text-zinc-500">Innings lanzadas</span>
                          <input v-model="row.inningsPitchedGame" type="text" inputmode="decimal" :class="statsInputCls" placeholder="Ej. 6 o 6.2" />
                        </label>
                        <label class="flex flex-col gap-0.5">
                          <span class="text-[10px] text-zinc-500">ERA (partido)</span>
                          <input v-model="row.eraGame" type="text" inputmode="decimal" :class="statsInputCls" placeholder="—" />
                        </label>
                        <label class="flex flex-col gap-0.5">
                          <span class="text-[10px] text-zinc-500">Carreras permitidas</span>
                          <input v-model="row.runsAllowedGame" type="text" inputmode="numeric" :class="statsInputCls" placeholder="—" />
                        </label>
                      </div>
                    </div>
                  </template>

                  <div class="mt-4 flex flex-wrap gap-2">
                    <button
                      type="button"
                      class="rounded-md bg-violet-500 px-3 py-1.5 text-xs font-medium text-white hover:bg-violet-400 disabled:opacity-50"
                      :disabled="statsSaveBusy"
                      @click="saveMatchStatsDraft(m)"
                    >
                      Guardar estadísticas
                    </button>
                    <button
                      type="button"
                      class="rounded-md border border-rose-900/70 bg-rose-950/30 px-3 py-1.5 text-xs text-rose-200 hover:bg-rose-950/45 disabled:opacity-50"
                      :disabled="statsSaveBusy"
                      @click="deleteMatchStats(m)"
                    >
                      Eliminar stats del partido
                    </button>
                    <button
                      type="button"
                      class="rounded-md border border-zinc-600 px-3 py-1.5 text-xs text-zinc-200 hover:bg-zinc-800 disabled:opacity-50"
                      :disabled="statsSaveBusy"
                      @click="closeStatsEditor()"
                    >
                      Cancelar
                    </button>
                  </div>
                </td>
              </tr>
              </template>
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
