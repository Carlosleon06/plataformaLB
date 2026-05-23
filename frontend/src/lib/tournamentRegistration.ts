import type { Tournament } from '../stores/tournaments'

export function isRegistrationAcceptingEntries(t: Tournament, nowMs = Date.now()): boolean {
  if (t.lifecycleStatus !== 'REGISTRATION_OPEN') return false
  const end = new Date(t.registrationEndAt).getTime()
  if (!Number.isNaN(end) && nowMs > end) return false
  if (t.registrationManuallyOpened) return true
  const start = new Date(t.registrationStartAt).getTime()
  if (!Number.isNaN(start) && nowMs < start) return false
  return true
}

export function lifecycleDisplayLabel(t: Tournament): string {
  switch (t.lifecycleStatus) {
    case 'REGISTRATION_SCHEDULED':
      return 'Inscripción programada'
    case 'REGISTRATION_OPEN':
      return isRegistrationAcceptingEntries(t) ? 'Inscripción abierta' : 'Inscripción abierta (fuera de ventana)'
    case 'REGISTRATION_CLOSED':
      return 'Inscripción cerrada'
    case 'LIVE':
      return 'En curso'
    case 'COMPLETED':
      return 'Finalizado'
    default:
      return t.lifecycleStatus
  }
}

export function lifecycleBadgeClass(t: Tournament): string {
  if (isRegistrationAcceptingEntries(t)) {
    return 'border-emerald-800/60 bg-emerald-950/40 text-emerald-200'
  }
  switch (t.lifecycleStatus) {
    case 'REGISTRATION_SCHEDULED':
      return 'border-violet-800/50 bg-violet-950/35 text-violet-200'
    case 'REGISTRATION_OPEN':
      return 'border-amber-800/50 bg-amber-950/30 text-amber-100'
    case 'REGISTRATION_CLOSED':
      return 'border-amber-800/50 bg-amber-950/30 text-amber-100'
    case 'LIVE':
      return 'border-sky-800/50 bg-sky-950/40 text-sky-200'
    case 'COMPLETED':
      return 'border-zinc-700 bg-zinc-900 text-zinc-400'
    default:
      return 'border-zinc-700 bg-zinc-900 text-zinc-400'
  }
}
