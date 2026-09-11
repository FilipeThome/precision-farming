import { Clock } from 'lucide-react'

import { ageMs, ageOf, DEFAULT_STALE_MS, formatAge } from '@/shared/lib/age'

type FreshnessChipProps = {
  /** ISO timestamp of the data; renders nothing when missing/invalid. */
  at?: string | null
  /** Age above which the chip switches to the stale tone. Default 24h. */
  staleAfterMs?: number
  /** Optional source label ("OEM", "Edge") shown before the age. */
  source?: string
  /** Test seam; defaults to Date.now(). */
  now?: number
  className?: string
  title?: string
}

/** Mono chip with the age of a timestamp ("2h", "36h", "4 min"); stale tone when older than the threshold. */
export function FreshnessChip({ at, staleAfterMs = DEFAULT_STALE_MS, source, now, className = '', title }: FreshnessChipProps) {
  const resolvedNow = now ?? Date.now()
  const age = ageOf(at, resolvedNow)
  if (!age) return null
  const ms = ageMs(at, resolvedNow) ?? 0
  const stale = ms > staleAfterMs
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-[6px] border px-1.5 py-px font-mono text-[11px] leading-4 ${
        stale ? 'border-[#f2c48f] bg-ag-warn-bg text-ag-warn' : 'border-ag-n-200 bg-ag-n-50 text-ag-n-700'
      } ${className}`}
      data-stale={stale ? 'true' : 'false'}
      title={title ?? (at ?? undefined)}
    >
      <Clock className="h-3 w-3" aria-hidden />
      {source ? <span>{source} ·</span> : null}
      {formatAge(age)}
    </span>
  )
}
