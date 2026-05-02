import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { apiFetch, coerceBearerToken } from '../lib/api'

export const CURSOR_KEY = 'leonbon.notifications.cursorISO'

export type UserNotificationPayload = {
  id: string
  category: string
  title: string
  summary: string | null
  teamIdRef: string | null
  tournamentIdRef: string | null
  tournamentEntryIdRef: string | null
  createdAt: string
}

type Toast = UserNotificationPayload & { _toastKey: number }

function maxCreatedAt(rows: UserNotificationPayload[]): string {
  let max = ''
  for (const r of rows) {
    if (r.createdAt > max) max = r.createdAt
  }
  return max
}

export const useNotificationsStore = defineStore('notifications', () => {
  const toasts = ref<Toast[]>([])
  let _seq = 0
  const sessionShownIds =
    typeof sessionStorage !== 'undefined'
      ? new Set<string>(JSON.parse(sessionStorage.getItem('leonbon.notifications.shown') ?? '[]') as string)
      : new Set<string>()

  function persistShown() {
    if (typeof sessionStorage === 'undefined') return
    try {
      sessionStorage.setItem(
        'leonbon.notifications.shown',
        JSON.stringify(Array.from(sessionShownIds).slice(-200)),
      )
    } catch {
      /* ignore */
    }
  }

  const activeToasts = computed(() => toasts.value)

  function enqueueToast(payload: UserNotificationPayload) {
    if (sessionShownIds.has(payload.id)) return
    sessionShownIds.add(payload.id)
    persistShown()

    _seq += 1
    const t: Toast = { ...payload, _toastKey: _seq }
    toasts.value = [...toasts.value, t]

    window.setTimeout(() => {
      toasts.value = toasts.value.filter((row) => row._toastKey !== t._toastKey)
    }, 5800)
  }

  function dismissToast(key: number) {
    toasts.value = toasts.value.filter((row) => row._toastKey !== key)
  }

  async function poll(authToken: string | null | undefined) {
    const bearer = coerceBearerToken(authToken)
    if (!bearer) return

    const prevIso =
      typeof localStorage !== 'undefined' ? (localStorage.getItem(CURSOR_KEY) ?? '') : ''
    const qs = prevIso.trim().length > 0 ? `&after=${encodeURIComponent(prevIso.trim())}` : ''

    const list = (await apiFetch(`/api/me/notifications?limit=35${qs}`, { method: 'GET' }, bearer)) as unknown
    const rowsRaw: UserNotificationPayload[] = Array.isArray(list) ? (list as UserNotificationPayload[]) : []

    const ascending = [...rowsRaw].sort((a, b) => a.createdAt.localeCompare(b.createdAt))

    /* Primera visita sin cursor: marca agua alta y no inundar la UI con historia. */
    if (prevIso.trim().length === 0 && typeof localStorage !== 'undefined') {
      const mark = maxCreatedAt(ascending) || new Date().toISOString()
      localStorage.setItem(CURSOR_KEY, mark)
      return
    }

    for (const row of ascending) {
      if (row.createdAt > prevIso.trim()) enqueueToast(row)
    }

    const maxFetched = ascending.length === 0 ? prevIso.trim() : maxCreatedAt(ascending)
    const merged =
      maxFetched.trim().length === 0
        ? new Date().toISOString()
        : maxFetched > prevIso.trim()
          ? maxFetched
          : prevIso.trim()
    if (typeof localStorage !== 'undefined') localStorage.setItem(CURSOR_KEY, merged)
  }

  /** Evita repetir toast al hacer poll después de push WebSocket. */
  function bumpNotificationCursor(iso: string) {
    if (typeof localStorage === 'undefined' || typeof iso !== 'string' || iso.trim().length === 0) return
    const prev = localStorage.getItem(CURSOR_KEY) ?? ''
    if (iso > prev) localStorage.setItem(CURSOR_KEY, iso)
  }

  return { activeToasts, poll, enqueueToast, dismissToast, bumpNotificationCursor }
})
