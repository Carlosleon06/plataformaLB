/** Locale de interfaz para fechas / números en pantalla */
export const LOCALE_ES_MX = 'es-MX' as const

export function formatDateTimeShort(iso: string | number | Date): string {
  const d = iso instanceof Date ? iso : new Date(iso)
  return d.toLocaleString(LOCALE_ES_MX, { dateStyle: 'short', timeStyle: 'short' })
}
