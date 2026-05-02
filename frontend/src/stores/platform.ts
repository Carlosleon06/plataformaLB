import { defineStore } from 'pinia'
import { apiFetch } from '../lib/api'

export type PlayerSocialLinks = {
  twitchUrl: string | null
  youtubeUrl: string | null
  xUrl: string | null
  instagramUrl: string | null
  discord: string | null
}

export type TeamAffiliation = {
  teamId: string
  name: string
  tag: string
  captain: boolean
  regionServer: string
  logoUrl: string
}

export type PublicPlayerSheet = {
  leonPlayerNumber: number | null
  publicFullNameOrNull: string | null
  country: string | null
  socialLinks: PlayerSocialLinks
  preferredGame: string | null
  rankLabelsByGame: Record<string, string>
  approvedTeamAffiliations: TeamAffiliation[]
}

export type GamePlatformRollup = {
  game: string
  tournamentsEnteredApproved: number
  bracketMatchWins: number
  bracketMatchLosses: number
  bracketWinRatePctApprox: number | null
  valorantStatsSamples?: number | null
  avgValorantKda?: number | null
  avgValorantHeadshotPct?: number | null
  fortniteStatsSamples?: number | null
  avgFortniteKillsPerMatch?: number | null
  avgFortniteKd?: number | null
  avgFortnitePlacement?: number | null
  fortniteRoyaleVictoryMatches?: number | null
  fortniteTop10Matches?: number | null
  fortniteDominantModePlayed?: string | null
  mlbStatsSamples?: number | null
  avgMlbBattingAvgGame?: number | null
  avgMlbHomeRunsGame?: number | null
  avgMlbInningsPitchedGame?: number | null
  avgMlbEraGame?: number | null
  avgMlbRunsAllowedGame?: number | null
}

export type UserPlatformSnapshot = {
  userId: string
  username: string
  nickname: string
  publicSheet: PublicPlayerSheet
  games: GamePlatformRollup[]
}

export type TrophyAward = {
  id: string
  tournamentId: string
  tournamentName: string
  game: string
  tournamentFormat: string
  placement: number
  badgeLabel: string
  tournamentEntryId: string
  entryType: string
  teamId: string | null
  playerId: string | null
  awardedAt: string
}

export type LeaderboardRow = {
  userId: string
  username: string
  nickname: string
  bracketMatchWins: number
}

export const usePlatformStore = defineStore('platform', () => {
  async function fetchUserSnapshot(userId: string): Promise<UserPlatformSnapshot> {
    const data = await apiFetch(`/api/platform/users/${encodeURIComponent(userId)}/snapshot`, { method: 'GET' })
    return data as UserPlatformSnapshot
  }

  async function fetchLeaderboard(game: string, limit = 15): Promise<LeaderboardRow[]> {
    const data = await apiFetch(
      `/api/platform/leaderboards/${encodeURIComponent(game)}?limit=${encodeURIComponent(String(limit))}`,
      { method: 'GET' },
    )
    return Array.isArray(data) ? (data as LeaderboardRow[]) : []
  }

  async function fetchUserTrophies(userId: string): Promise<TrophyAward[]> {
    const data = await apiFetch(
      `/api/platform/users/${encodeURIComponent(userId)}/trophies`,
      { method: 'GET' },
    )
    return Array.isArray(data) ? (data as TrophyAward[]) : []
  }

  return { fetchUserSnapshot, fetchLeaderboard, fetchUserTrophies }
})
