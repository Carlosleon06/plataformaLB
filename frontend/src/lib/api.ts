export function apiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
}

export async function apiFetch(path: string, init: RequestInit = {}, token?: string | null) {
  const headers = new Headers(init.headers)
  const isFormData = typeof FormData !== 'undefined' && init.body instanceof FormData
  if (!headers.has('Content-Type') && init.body && !isFormData) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
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
