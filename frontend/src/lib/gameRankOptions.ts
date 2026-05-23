export type ProfileGameKey = 'VALORANT' | 'FORTNITE' | 'MLB'

export const VALORANT_RANK_OPTIONS = [
  'Hierro 1',
  'Hierro 2',
  'Hierro 3',
  'Bronce 1',
  'Bronce 2',
  'Bronce 3',
  'Plata 1',
  'Plata 2',
  'Plata 3',
  'Oro 1',
  'Oro 2',
  'Oro 3',
  'Platino 1',
  'Platino 2',
  'Platino 3',
  'Diamante 1',
  'Diamante 2',
  'Diamante 3',
  'Ascendente 1',
  'Ascendente 2',
  'Ascendente 3',
  'Inmortal 1',
  'Inmortal 2',
  'Inmortal 3',
  'Radiante',
] as const

export const FORTNITE_RANK_OPTIONS = [
  'Bronce I',
  'Bronce II',
  'Bronce III',
  'Plata I',
  'Plata II',
  'Plata III',
  'Oro I',
  'Oro II',
  'Oro III',
  'Platino I',
  'Platino II',
  'Platino III',
  'Diamante I',
  'Diamante II',
  'Diamante III',
  'Élite',
  'Campeón',
  'Unreal',
] as const

export const MLB_RANK_OPTIONS = [
  'Menos de 700',
  '700 – 799',
  '800 – 849',
  '850 – 899',
  '900+',
] as const

const OPTIONS_BY_GAME: Record<ProfileGameKey, readonly string[]> = {
  VALORANT: VALORANT_RANK_OPTIONS,
  FORTNITE: FORTNITE_RANK_OPTIONS,
  MLB: MLB_RANK_OPTIONS,
}

function rankKey(value: string): string {
  return value.trim().toLocaleLowerCase('es')
}

/** Alinea un valor guardado (p. ej. texto libre antiguo) con la etiqueta canónica del listado. */
export function normalizeStoredRank(game: ProfileGameKey, stored: string): string {
  const trimmed = stored.trim()
  if (!trimmed) return ''
  const opts = OPTIONS_BY_GAME[game]
  const key = rankKey(trimmed)
  const match = opts.find((o) => rankKey(o) === key)
  return match ?? trimmed
}

/** Opciones del select, incluyendo el valor actual si ya no está en el catálogo. */
export function rankSelectOptions(game: ProfileGameKey, current: string): string[] {
  const normalized = normalizeStoredRank(game, current)
  const opts = [...OPTIONS_BY_GAME[game]]
  if (normalized && !opts.some((o) => rankKey(o) === rankKey(normalized))) {
    return [normalized, ...opts]
  }
  return opts
}
