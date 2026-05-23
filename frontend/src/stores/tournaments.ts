import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiFetch } from '../lib/api'

export type Tournament = {
  id: string
  name: string
  organizers: string
  game: string
  format: string
  lifecycleStatus: string
  registrationManuallyOpened?: boolean
  registrationStartAt: string
  registrationEndAt: string
  competitionStartAt: string
  competitionEndAt: string
  streamUrl: string | null
  rulesHtml?: string | null
  eligibilityNotes?: string | null
  prizeNotes?: string | null
  /** Cuántos puestos clasificados reciben L-Coins al cerrar torneo (0 o null = tabla no usada). */
  prizeWinnerSlots?: number | null
  prizeLeonCoinsByPlacement?: number[] | null
  maxApprovedParticipants?: number | null
  bracketSize?: number | null
  /** Si viene del backend: ya se ejecutó liquidación monetaria por puestos. */
  placementPrizeLedgerCompletedAt?: string | null
  createdAt: string
}

export type BracketMatch = {
  id: string
  tournamentId: string
  /** WB = ganadores, LB = perdedores, GF = final doble, RR = todos vs todos */
  bracketPool?: 'WB' | 'LB' | 'GF' | 'RR'
  round: number
  indexInRound: number
  entryIdA: string | null
  entryIdB: string | null
  winnerEntryId: string | null
  status: string
  scheduledStartAt?: string | null
  bettingWindowMinutes?: number
  bettingClosesAt?: string | null
  totalStakeEntryA?: number
  totalStakeEntryB?: number
  impliedReturnPerCoinOnA?: number | null
  impliedReturnPerCoinOnB?: number | null
}

export type TournamentEntry = {
  id: string
  tournamentId: string
  type: string
  teamId: string | null
  playerId: string | null
  status: string
  selectedRosterUserIds: string[]
  createdAt: string
  /** Present for TEAM entries when backend could resolve the team. */
  teamName?: string | null
  teamTag?: string | null
  /** Present for PLAYER entries when backend could resolve the user. */
  playerUsername?: string | null
}

/** Stats por partido (MVP 2); solo uno de los arrays está poblado según game. */
export type BracketMatchStats = {
  matchId: string
  tournamentId: string
  game: string
  revision: number
  recordedByAdminUserId: string
  recordedAt: string
  valorantPlayers: Array<Record<string, unknown>>
  fortnitePlayers: Array<Record<string, unknown>>
  mlbPlayers: Array<Record<string, unknown>>
}

export type CreateTournamentPayload = {
  name: string
  organizers: string
  game: 'VALORANT' | 'FORTNITE' | 'MLB'
  format: 'SINGLE_ELIM' | 'DOUBLE_ELIM' | 'ROUND_ROBIN'
  registrationStartAt: string
  registrationEndAt: string
  competitionStartAt: string
  competitionEndAt: string
  streamUrl?: string | null
  rulesHtml?: string | null
  eligibilityNotes?: string | null
  prizeNotes?: string | null
  maxApprovedParticipants?: number | null
  /** 0 sin premios monetarios declarados por puesto. */
  prizeWinnerSlots?: number | null
  prizeLeonCoinsByPlacement?: number[] | null
}

/** Human-readable row for tournament entry lists (team name/tag or player username). */
export function entryParticipantLabel(e: TournamentEntry): string {
  if (e.type === 'TEAM') {
    const name = e.teamName?.trim()
    const tag = e.teamTag?.trim()
    if (name && tag) return `${name} [${tag}]`
    if (name) return name
    if (tag) return `[${tag}]`
    return e.teamId ?? '—'
  }
  if (e.type === 'PLAYER') {
    const u = e.playerUsername?.trim()
    if (u) return u
    return e.playerId ?? '—'
  }
  return '—'
}

/** Defaults match backend `application.yml` (dev). */
export function rosterSizeForGame(game: string): number {
  switch (game) {
    case 'VALORANT':
      return 5
    case 'FORTNITE':
      return 4
    case 'MLB':
      return 1
    default:
      return 5
  }
}

export const useTournamentsStore = defineStore('tournaments', () => {
  const busy = ref(false)
  const error = ref<string | null>(null)

  async function listTournaments(): Promise<Tournament[]> {
    busy.value = true
    error.value = null
    try {
      return (await apiFetch('/api/tournaments', { method: 'GET' }, null)) as Tournament[]
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Unknown error'
      throw e
    } finally {
      busy.value = false
    }
  }

  /** Admin: hasta 200 torneos, orden por última actualización (útil tras cerrar inscripciones). */
  async function listTournamentsAdmin(token: string): Promise<Tournament[]> {
    return (await apiFetch('/api/admin/tournaments', { method: 'GET' }, token)) as Tournament[]
  }

  async function getTournament(tournamentId: string): Promise<Tournament> {
    return (await apiFetch(`/api/tournaments/${encodeURIComponent(tournamentId)}`, { method: 'GET' }, null)) as Tournament
  }

  async function listEntries(tournamentId: string): Promise<TournamentEntry[]> {
    return (await apiFetch(`/api/tournaments/${encodeURIComponent(tournamentId)}/entries`, { method: 'GET' }, null)) as TournamentEntry[]
  }

  async function listMatches(tournamentId: string): Promise<BracketMatch[]> {
    return (await apiFetch(`/api/tournaments/${encodeURIComponent(tournamentId)}/matches`, { method: 'GET' }, null)) as BracketMatch[]
  }

  async function getMatchStats(tournamentId: string, matchId: string): Promise<BracketMatchStats> {
    return (await apiFetch(
      `/api/tournaments/${encodeURIComponent(tournamentId)}/matches/${encodeURIComponent(matchId)}/stats`,
      { method: 'GET' },
      null,
    )) as BracketMatchStats
  }

  async function closeRegistrationAdmin(token: string, tournamentId: string): Promise<Tournament> {
    return (await apiFetch(
      `/api/admin/tournaments/${encodeURIComponent(tournamentId)}/registration/close`,
      { method: 'POST' },
      token,
    )) as Tournament
  }

  async function openRegistrationAdmin(token: string, tournamentId: string): Promise<Tournament> {
    return (await apiFetch(
      `/api/admin/tournaments/${encodeURIComponent(tournamentId)}/registration/open`,
      { method: 'POST' },
      token,
    )) as Tournament
  }

  async function reopenRegistrationAdmin(token: string, tournamentId: string): Promise<Tournament> {
    return (await apiFetch(
      `/api/admin/tournaments/${encodeURIComponent(tournamentId)}/registration/reopen`,
      { method: 'POST' },
      token,
    )) as Tournament
  }

  async function deleteTournamentAdmin(token: string, tournamentId: string): Promise<void> {
    await apiFetch(`/api/admin/tournaments/${encodeURIComponent(tournamentId)}`, { method: 'DELETE' }, token)
  }

  async function generateBracketAdmin(token: string, tournamentId: string): Promise<Tournament> {
    return (await apiFetch(
      `/api/admin/tournaments/${encodeURIComponent(tournamentId)}/bracket/generate`,
      { method: 'POST' },
      token,
    )) as Tournament
  }

  async function setMatchWinnerAdmin(token: string, tournamentId: string, matchId: string, winnerEntryId: string): Promise<BracketMatch> {
    return (await apiFetch(
      `/api/admin/tournaments/${encodeURIComponent(tournamentId)}/matches/${encodeURIComponent(matchId)}/winner`,
      { method: 'POST', body: JSON.stringify({ winnerEntryId }) },
      token,
    )) as BracketMatch
  }

  async function openBettingWindowAdmin(token: string, tournamentId: string, matchId: string): Promise<BracketMatch> {
    return (await apiFetch(
      `/api/admin/tournaments/${encodeURIComponent(tournamentId)}/matches/${encodeURIComponent(matchId)}/betting/open`,
      { method: 'POST' },
      token,
    )) as BracketMatch
  }

  async function closeBettingWindowAdmin(token: string, tournamentId: string, matchId: string): Promise<BracketMatch> {
    return (await apiFetch(
      `/api/admin/tournaments/${encodeURIComponent(tournamentId)}/matches/${encodeURIComponent(matchId)}/betting/close`,
      { method: 'POST' },
      token,
    )) as BracketMatch
  }

  /** 200 con JSON o 204 sin cuerpo (borrando stats vacíos). */
  async function upsertMatchStatsAdmin(
    token: string,
    tournamentId: string,
    matchId: string,
    body: Record<string, unknown>,
  ): Promise<BracketMatchStats | null> {
    return (await apiFetch(
      `/api/admin/tournaments/${encodeURIComponent(tournamentId)}/matches/${encodeURIComponent(matchId)}/stats`,
      { method: 'PUT', body: JSON.stringify(body) },
      token,
    )) as BracketMatchStats | null
  }

  async function registerTeam(
    token: string,
    tournamentId: string,
    body: { teamId: string; selectedRosterUserIds: string[] },
  ): Promise<TournamentEntry> {
    busy.value = true
    error.value = null
    try {
      return (await apiFetch(
        `/api/tournaments/${encodeURIComponent(tournamentId)}/entries/team`,
        { method: 'POST', body: JSON.stringify(body) },
        token,
      )) as TournamentEntry
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Unknown error'
      throw e
    } finally {
      busy.value = false
    }
  }

  async function registerMlbSelf(token: string, tournamentId: string): Promise<TournamentEntry> {
    busy.value = true
    error.value = null
    try {
      return (await apiFetch(
        `/api/tournaments/${encodeURIComponent(tournamentId)}/entries/me`,
        { method: 'POST' },
        token,
      )) as TournamentEntry
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Unknown error'
      throw e
    } finally {
      busy.value = false
    }
  }

  async function approveEntryAdmin(token: string, tournamentId: string, entryId: string): Promise<TournamentEntry> {
    return (await apiFetch(
      `/api/admin/tournaments/${encodeURIComponent(tournamentId)}/entries/${encodeURIComponent(entryId)}/approve`,
      { method: 'POST' },
      token,
    )) as TournamentEntry
  }

  async function rejectEntryAdmin(token: string, tournamentId: string, entryId: string): Promise<TournamentEntry> {
    return (await apiFetch(
      `/api/admin/tournaments/${encodeURIComponent(tournamentId)}/entries/${encodeURIComponent(entryId)}/reject`,
      { method: 'POST' },
      token,
    )) as TournamentEntry
  }

  async function createTournamentAdmin(token: string, body: CreateTournamentPayload): Promise<Tournament> {
    busy.value = true
    error.value = null
    try {
      const payload = { ...body, streamUrl: body.streamUrl?.trim() || null }
      return (await apiFetch('/api/admin/tournaments', { method: 'POST', body: JSON.stringify(payload) }, token)) as Tournament
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Unknown error'
      throw e
    } finally {
      busy.value = false
    }
  }

  return {
    busy,
    error,
    listTournaments,
    listTournamentsAdmin,
    getTournament,
    listEntries,
    listMatches,
    getMatchStats,
    closeRegistrationAdmin,
    openRegistrationAdmin,
    reopenRegistrationAdmin,
    deleteTournamentAdmin,
    generateBracketAdmin,
    setMatchWinnerAdmin,
    openBettingWindowAdmin,
    closeBettingWindowAdmin,
    upsertMatchStatsAdmin,
    registerTeam,
    registerMlbSelf,
    approveEntryAdmin,
    rejectEntryAdmin,
    createTournamentAdmin,
  }
})
