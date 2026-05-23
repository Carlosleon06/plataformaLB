/** Valor compatible con input datetime-local y Date del navegador. */
export function formatDatetimeLocal(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function parseDatetimeLocal(value: string): Date | null {
  if (!value) return null
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? null : d
}

/** Siguiente hora en punto (apertura típica de inscripción). */
export function defaultRegistrationStartLocal(): string {
  const d = new Date()
  d.setSeconds(0, 0)
  d.setMinutes(0)
  d.setHours(d.getHours() + 1)
  return formatDatetimeLocal(d)
}

export function addMsToLocal(baseLocal: string, ms: number): string {
  const base = parseDatetimeLocal(baseLocal)
  if (!base) return ''
  return formatDatetimeLocal(new Date(base.getTime() + ms))
}
