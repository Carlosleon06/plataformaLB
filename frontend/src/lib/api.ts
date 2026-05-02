export function apiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
}

/** Endpoint SockJS/STOMP (`/ws`); mismo host que REST. */

export function stompSockJsUrl(): string {
  return `${apiBaseUrl().replace(/\/$/, '')}/ws`
}

/** Accepts a raw string or a Pinia/Vue ref-like `{ value: string }` so Authorization is never malformed. */
export function coerceBearerToken(token: unknown): string | null {
  if (token == null) return null
  if (typeof token === 'string') return token.length > 0 ? token : null
  if (typeof token === 'object' && token !== null && 'value' in token) {
    const v = (token as { value: unknown }).value
    if (typeof v === 'string' && v.length > 0) return v
  }
  return null
}

export async function apiFetch(path: string, init: RequestInit = {}, token?: string | null | unknown) {
  const headers = new Headers(init.headers)
  const isFormData = typeof FormData !== 'undefined' && init.body instanceof FormData
  if (!headers.has('Content-Type') && init.body && !isFormData) {
    headers.set('Content-Type', 'application/json')
  }
  const bearer = coerceBearerToken(token)
  if (bearer) {
    headers.set('Authorization', `Bearer ${bearer}`)
  }

  const res = await fetch(`${apiBaseUrl()}${path}`, { ...init, headers })
  const text = await res.text()
  const data = text ? safeJson(text) : null

  if (!res.ok) {
    const message =
      data && typeof data === 'object' && data && 'message' in data
        ? String((data as { message: unknown }).message)
        : `HTTP ${res.status}`
    throw new Error(message)
  }

  return data
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}
