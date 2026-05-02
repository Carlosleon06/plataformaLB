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
  createdAt: string
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
    registerTeam,
    registerMlbSelf,
    approveEntryAdmin,
    rejectEntryAdmin,
    createTournamentAdmin,
  }
})
