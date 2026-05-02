import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiFetch } from '../lib/api'

export type BetStatus = 'PENDING' | 'WON' | 'LOST' | 'REFUNDED'

export type Bet = {
  id: string
  tournamentId: string
  matchId: string
  pickedEntryId: string
  amount: number
  status: BetStatus
  payoutAmount: number | null
  createdAt: string
  resolvedAt: string | null
}

export const useBetsStore = defineStore('bets', () => {
  const busy = ref(false)
  const error = ref<string | null>(null)

  async function listMyBets(token: string, opts: { tournamentId?: string; limit?: number } = {}): Promise<Bet[]> {
    const params = new URLSearchParams()
    if (opts.tournamentId) params.set('tournamentId', opts.tournamentId)
    if (opts.limit != null) params.set('limit', String(opts.limit))
    const q = params.toString()
    return (await apiFetch(`/api/me/bets${q ? `?${q}` : ''}`, { method: 'GET' }, token)) as Bet[]
  }

  async function placeBet(
    token: string,
    body: { matchId: string; pickedEntryId: string; amount: number },
  ): Promise<Bet> {
    busy.value = true
    error.value = null
    try {
      return (await apiFetch('/api/me/bets', { method: 'POST', body: JSON.stringify(body) }, token)) as Bet
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Unknown error'
      throw e
    } finally {
      busy.value = false
    }
  }

  return { busy, error, listMyBets, placeBet }
})

