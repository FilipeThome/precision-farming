const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

export type AgeUnit = 's' | 'min' | 'h' | 'd'
export type Age = { value: number; unit: AgeUnit }

/** Age of a timestamp as a coarse value + unit. Returns null for invalid input. */
export function ageOf(timestamp: string | number | Date | null | undefined, now: number = Date.now()): Age | null {
  if (timestamp == null || timestamp === '') return null
  const at = timestamp instanceof Date ? timestamp.getTime() : new Date(timestamp).getTime()
  if (Number.isNaN(at)) return null
  const ms = Math.max(0, now - at)
  return ageFromMs(ms)
}

export function ageFromMs(ms: number): Age {
  const safe = Math.max(0, ms)
  if (safe < MINUTE) return { value: Math.floor(safe / 1000), unit: 's' }
  if (safe < HOUR) return { value: Math.floor(safe / MINUTE), unit: 'min' }
  if (safe < 3 * DAY) return { value: Math.floor(safe / HOUR), unit: 'h' }
  return { value: Math.floor(safe / DAY), unit: 'd' }
}

/** Compact mono label: "45s", "4 min", "2h", "36h", "12d". */
export function formatAge(age: Age): string {
  if (age.unit === 'min') return `${age.value} min`
  return `${age.value}${age.unit}`
}

export function ageMs(timestamp: string | number | Date | null | undefined, now: number = Date.now()): number | null {
  if (timestamp == null || timestamp === '') return null
  const at = timestamp instanceof Date ? timestamp.getTime() : new Date(timestamp).getTime()
  if (Number.isNaN(at)) return null
  return Math.max(0, now - at)
}

export const DEFAULT_STALE_MS = DAY
