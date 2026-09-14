import { AlertTriangle, CheckCircle2, CirclePause, Clock, Info, Wrench } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

import { useFormat } from '@/shared/lib/useFormat'

export type TagTone = 'crit' | 'warn' | 'ok' | 'info' | 'neutral' | 'teal' | 'outline'

export const TAG_TONE: Record<TagTone, string> = {
  crit: 'bg-ag-crit-bg text-ag-crit',
  warn: 'bg-ag-warn-bg text-ag-warn',
  ok: 'bg-ag-ok-bg text-ag-ok',
  info: 'bg-ag-info-bg text-ag-info',
  neutral: 'bg-ag-n-100 text-ag-n-700',
  teal: 'bg-ag-t-100 text-ag-t-700',
  outline: 'border border-ag-n-300 text-ag-n-700',
}

const tone: Record<string, { tone: TagTone; icon: LucideIcon }> = {
  OPERATING: { tone: 'ok', icon: CheckCircle2 },
  IDLE: { tone: 'neutral', icon: Clock },
  MAINTENANCE: { tone: 'warn', icon: Wrench },
  PLANNED: { tone: 'neutral', icon: Clock },
  IN_PROGRESS: { tone: 'teal', icon: CheckCircle2 },
  PAUSED: { tone: 'warn', icon: CirclePause },
  COMPLETED: { tone: 'ok', icon: CheckCircle2 },
  OPEN: { tone: 'crit', icon: AlertTriangle },
  ACKED: { tone: 'ok', icon: CheckCircle2 },
  CRITICAL: { tone: 'crit', icon: AlertTriangle },
  WARNING: { tone: 'warn', icon: AlertTriangle },
  INFO: { tone: 'info', icon: Info },
  DRAFT: { tone: 'info', icon: Clock },
  PENDING: { tone: 'info', icon: Clock },
  RECOMMENDED: { tone: 'info', icon: Clock },
  APPROVED: { tone: 'ok', icon: CheckCircle2 },
  REJECTED: { tone: 'crit', icon: AlertTriangle },
  EXECUTED: { tone: 'ok', icon: CheckCircle2 },
  QUEUED: { tone: 'neutral', icon: Clock },
  DISPATCHED: { tone: 'teal', icon: CheckCircle2 },
  DELIVERED: { tone: 'ok', icon: CheckCircle2 },
  FAVORABLE: { tone: 'ok', icon: CheckCircle2 },
  UNFAVORABLE: { tone: 'crit', icon: AlertTriangle },
  MARGINAL: { tone: 'warn', icon: AlertTriangle },
  ACTIVE: { tone: 'teal', icon: CheckCircle2 },
  RUNNING: { tone: 'teal', icon: CheckCircle2 },
  HEDGED: { tone: 'ok', icon: CheckCircle2 },
  STANDARD: { tone: 'neutral', icon: Info },
  PREMIUM: { tone: 'ok', icon: CheckCircle2 },
  LOW: { tone: 'neutral', icon: Info },
  MEDIUM: { tone: 'warn', icon: AlertTriangle },
  HIGH: { tone: 'crit', icon: AlertTriangle },
  UNKNOWN: { tone: 'outline', icon: Info },
}

type StatusBadgeProps = {
  value: string
  /** Override the automatic tone (e.g. for non-status labels). */
  tone?: TagTone
  className?: string
}

/** Status tag (restyled to Terra tag tones). Label comes from the domain dictionary. */
export function StatusBadge({ value, tone: override, className = '' }: StatusBadgeProps) {
  const { label } = useFormat()
  const style = tone[value] ?? { tone: 'neutral' as TagTone, icon: Info }
  const Icon = style.icon
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold leading-4 ${TAG_TONE[override ?? style.tone]} ${className}`}
      data-tone={override ?? style.tone}
    >
      <Icon className="h-3 w-3" aria-hidden />
      {label(value)}
    </span>
  )
}
