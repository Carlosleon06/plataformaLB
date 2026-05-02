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
  registrationStartAt: string
  registrationEndAt: string
  competitionStartAt: string
  competitionEndAt: string
  streamUrl: string | null
  bracketSize?: number | null
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

  async function getTournament(tournamentId: string): Promise<Tournament> {
    return (await apiFetch(`/api/tournaments/${encodeURIComponent(tournamentId)}`, { method: 'GET' }, null)) as Tournament
  }

  async function listEntries(tournamentId: string): Promise<TournamentEntry[]> {
    return (await apiFetch(`/api/tournaments/${encodeURIComponent(tournamentId)}/entries`, { method: 'GET' }, null)) as TournamentEntry[]
  }

  async function listMatches(tournamentId: string): Promise<BracketMatch[]> {
    return (await apiFetch(`/api/tournaments/${encodeURIComponent(tournamentId)}/matches`, { method: 'GET' }, null)) as BracketMatch[]
  }

  async function closeRegistrationAdmin(token: string, tournamentId: string): Promise<Tournament> {
    return (await apiFetch(
      `/api/admin/tournaments/${encodeURIComponent(tournamentId)}/registration/close`,
      { method: 'POST' },
      token,
    )) as Tournament
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
    getTournament,
    listEntries,
    listMatches,
    closeRegistrationAdmin,
    generateBracketAdmin,
    setMatchWinnerAdmin,
    registerTeam,
    registerMlbSelf,
    approveEntryAdmin,
    rejectEntryAdmin,
    createTournamentAdmin,
  }
})
