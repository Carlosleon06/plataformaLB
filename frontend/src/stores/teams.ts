import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiFetch } from '../lib/api'

export type TeamPublic = {
  id: string
  name: string
  tag: string
  regionServer: string
  logoUrl: string
  status: string
  memberCount: number
  createdAt: string
}

export type TeamCaptainView = TeamPublic & {
  captainUserId: string
  captainUsername: string
  coachUserIds: string[]
  coachUsernames: string[]
  memberUserIds: string[]
  memberUsernames: string[]
}

export type CaptainTeamSummary = {
  id: string
  name: string
  tag: string
  regionServer: string
  logoUrl: string
  memberUserIds: string[]
}

export type PendingTeamAdminRow = {
  id: string
  name: string
  tag: string
  status: string
  regionServer: string
  captainUsername: string
  memberCount: number
  createdAt: string
}

export type JoinRequest = {
  id: string
  teamId: string
  requesterUserId: string
  requesterUsername: string
  status: string
  createdAt: string
  updatedAt: string
}

export const useTeamsStore = defineStore('teams', () => {
  const publicTeams = ref<TeamPublic[]>([])
  const busy = ref(false)
  const error = ref<string | null>(null)

  async function loadPublicTeams(token?: string | null) {
    busy.value = true
    error.value = null
    try {
      publicTeams.value = (await apiFetch('/api/teams/public', { method: 'GET' }, token ?? null)) as TeamPublic[]
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Unknown error'
      throw e
    } finally {
      busy.value = false
    }
  }

  async function createTeam(
    token: string,
    payload: { name: string; tag: string; regionServer: string },
  ): Promise<TeamPublic> {
    busy.value = true
    error.value = null
    try {
      return (await apiFetch('/api/teams', { method: 'POST', body: JSON.stringify(payload) }, token)) as TeamPublic
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Unknown error'
      throw e
    } finally {
      busy.value = false
    }
  }

  async function getTeam(token: string | null, teamId: string): Promise<TeamPublic | TeamCaptainView> {
    return (await apiFetch(`/api/teams/${encodeURIComponent(teamId)}`, { method: 'GET' }, token)) as
      | TeamPublic
      | TeamCaptainView
  }

  async function listMyCaptainTeams(token: string): Promise<CaptainTeamSummary[]> {
    return (await apiFetch('/api/me/captain-teams', { method: 'GET' }, token)) as CaptainTeamSummary[]
  }

  async function requestJoin(token: string, teamId: string): Promise<JoinRequest> {
    return (await apiFetch(`/api/teams/${encodeURIComponent(teamId)}/join-requests`, { method: 'POST' }, token)) as JoinRequest
  }

  async function listJoinRequests(token: string, teamId: string): Promise<JoinRequest[]> {
    return (await apiFetch(`/api/teams/${encodeURIComponent(teamId)}/join-requests`, { method: 'GET' }, token)) as JoinRequest[]
  }

  async function acceptJoin(token: string, teamId: string, requestId: string): Promise<JoinRequest> {
    return (await apiFetch(
      `/api/teams/${encodeURIComponent(teamId)}/join-requests/${encodeURIComponent(requestId)}/accept`,
      { method: 'POST' },
      token,
    )) as JoinRequest
  }

  async function rejectJoin(token: string, teamId: string, requestId: string): Promise<JoinRequest> {
    return (await apiFetch(
      `/api/teams/${encodeURIComponent(teamId)}/join-requests/${encodeURIComponent(requestId)}/reject`,
      { method: 'POST' },
      token,
    )) as JoinRequest
  }

  async function approveTeamAdmin(token: string, teamId: string): Promise<void> {
    await apiFetch(`/api/admin/teams/${encodeURIComponent(teamId)}/approve`, { method: 'POST' }, token)
  }

  async function rejectTeamAdmin(token: string, teamId: string): Promise<void> {
    await apiFetch(`/api/admin/teams/${encodeURIComponent(teamId)}/reject`, { method: 'POST' }, token)
  }

  async function listPendingTeamsAdmin(token: string): Promise<PendingTeamAdminRow[]> {
    return (await apiFetch('/api/admin/teams/pending', { method: 'GET' }, token)) as PendingTeamAdminRow[]
  }

  async function resetLogoAdmin(token: string, teamId: string): Promise<void> {
    await apiFetch(`/api/admin/teams/${encodeURIComponent(teamId)}/logo/reset`, { method: 'POST' }, token)
  }

  async function uploadTeamLogo(token: string, teamId: string, file: File): Promise<TeamCaptainView> {
    const fd = new FormData()
    fd.append('file', file)
    return (await apiFetch(`/api/teams/${encodeURIComponent(teamId)}/logo`, { method: 'POST', body: fd }, token)) as TeamCaptainView
  }

  async function resetTeamLogoCaptain(token: string, teamId: string): Promise<TeamCaptainView> {
    return (await apiFetch(`/api/teams/${encodeURIComponent(teamId)}/logo/reset`, { method: 'POST' }, token)) as TeamCaptainView
  }

  async function leaveTeam(token: string, teamId: string): Promise<TeamCaptainView> {
    return (await apiFetch(`/api/teams/${encodeURIComponent(teamId)}/leave`, { method: 'POST' }, token)) as TeamCaptainView
  }

  async function disbandTeam(token: string, teamId: string): Promise<TeamCaptainView> {
    return (await apiFetch(`/api/teams/${encodeURIComponent(teamId)}/disband`, { method: 'POST' }, token)) as TeamCaptainView
  }

  return {
    publicTeams,
    busy,
    error,
    loadPublicTeams,
    createTeam,
    getTeam,
    listMyCaptainTeams,
    requestJoin,
    listJoinRequests,
    acceptJoin,
    rejectJoin,
    approveTeamAdmin,
    rejectTeamAdmin,
    listPendingTeamsAdmin,
    resetLogoAdmin,
    uploadTeamLogo,
    resetTeamLogoCaptain,
    leaveTeam,
    disbandTeam,
  }
})
