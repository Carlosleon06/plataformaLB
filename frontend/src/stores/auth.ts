import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { apiFetch } from '../lib/api'

/** Same key as in `apiFetch` / localStorage; use for fallbacks when reading the session token. */
export const LEONBON_TOKEN_STORAGE_KEY = 'leonbon.token'

type MeResponse = {
  id: string
  username: string
  nickname: string | null
  status: string
  role: string
  leonCoinsBalance: number
}

type TransactionResponse = {
  id: string
  type: string
  amount: number
  balanceAfter: number
  ref: string | null
  createdAt: string
}

const TOKEN_KEY = LEONBON_TOKEN_STORAGE_KEY

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const me = ref<MeResponse | null>(null)
  const transactions = ref<TransactionResponse[]>([])
  const busy = ref(false)
  const error = ref<string | null>(null)

  const isAuthed = computed(() => Boolean(token.value))

  function setToken(next: string | null) {
    token.value = next
    if (next) localStorage.setItem(TOKEN_KEY, next)
    else localStorage.removeItem(TOKEN_KEY)
  }

  async function register(payload: { username: string; password: string; nickname?: string }) {
    busy.value = true
    error.value = null
    try {
      const data = (await apiFetch('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(payload),
      })) as { token: string }
      setToken(data.token)
      await refreshAll()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Unknown error'
      throw e
    } finally {
      busy.value = false
    }
  }

  async function login(payload: { username: string; password: string }) {
    busy.value = true
    error.value = null
    try {
      const data = (await apiFetch('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(payload),
      })) as { token: string }
      setToken(data.token)
      await refreshAll()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Unknown error'
      throw e
    } finally {
      busy.value = false
    }
  }

  function logout() {
    setToken(null)
    me.value = null
    transactions.value = []
    error.value = null
  }

  async function refreshMe() {
    if (!token.value) return
    me.value = (await apiFetch('/api/me', { method: 'GET' }, token.value)) as MeResponse
  }

  async function refreshTransactions(limit = 20) {
    if (!token.value) return
    transactions.value = (await apiFetch(
      `/api/me/transactions?limit=${encodeURIComponent(String(limit))}`,
      { method: 'GET' },
      token.value,
    )) as TransactionResponse[]
  }

  async function dailyClaim() {
    if (!token.value) return
    busy.value = true
    error.value = null
    try {
      await apiFetch('/api/me/daily-claim', { method: 'POST' }, token.value)
      await refreshAll()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Unknown error'
      throw e
    } finally {
      busy.value = false
    }
  }

  async function refreshAll() {
    await refreshMe()
    await refreshTransactions(25)
  }

  async function bootstrap() {
    if (!token.value) return
    try {
      await refreshAll()
    } catch {
      // token invalid/expired
      logout()
    }
  }

  return {
    token,
    me,
    transactions,
    busy,
    error,
    isAuthed,
    register,
    login,
    logout,
    refreshAll,
    bootstrap,
    dailyClaim,
  }
})
