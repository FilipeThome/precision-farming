import { AlertTriangle, CheckCircle2, CirclePause, Clock, Info, Wrench } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

import { useFormat } from '@/shared/lib/useFormat'

const tone: Record<string, { className: string; icon: LucideIcon }> = {
  OPERATING: { className: 'bg-teal-50 text-pf-teal', icon: CheckCircle2 },
  IDLE: { className: 'bg-slate-100 text-slate-700', icon: Clock },
  MAINTENANCE: { className: 'bg-amber-50 text-amber-800', icon: Wrench },
  PLANNED: { className: 'bg-slate-100 text-slate-700', icon: Clock },
  IN_PROGRESS: { className: 'bg-teal-50 text-pf-teal', icon: CheckCircle2 },
  PAUSED: { className: 'bg-amber-50 text-amber-800', icon: CirclePause },
  COMPLETED: { className: 'bg-emerald-50 text-pf-green', icon: CheckCircle2 },
  OPEN: { className: 'bg-red-50 text-red-800', icon: AlertTriangle },
  ACKED: { className: 'bg-emerald-50 text-pf-green', icon: CheckCircle2 },
  CRITICAL: { className: 'bg-red-50 text-red-800', icon: AlertTriangle },
  WARNING: { className: 'bg-amber-50 text-amber-800', icon: AlertTriangle },
  INFO: { className: 'bg-sky-50 text-sky-800', icon: Info },
  DRAFT: { className: 'bg-slate-100 text-slate-700', icon: Clock },
  APPROVED: { className: 'bg-emerald-50 text-pf-green', icon: CheckCircle2 },
  QUEUED: { className: 'bg-slate-100 text-slate-700', icon: Clock },
  DISPATCHED: { className: 'bg-teal-50 text-pf-teal', icon: CheckCircle2 },
  DELIVERED: { className: 'bg-emerald-50 text-pf-green', icon: CheckCircle2 },
  DEMO: { className: 'bg-sky-50 text-sky-800', icon: Info },
  FAVORABLE: { className: 'bg-emerald-50 text-pf-green', icon: CheckCircle2 },
  UNFAVORABLE: { className: 'bg-amber-50 text-amber-800', icon: AlertTriangle },
  MARGINAL: { className: 'bg-amber-50 text-amber-800', icon: AlertTriangle },
  ACTIVE: { className: 'bg-teal-50 text-pf-teal', icon: CheckCircle2 },
  RUNNING: { className: 'bg-teal-50 text-pf-teal', icon: CheckCircle2 },
  HEDGED: { className: 'bg-emerald-50 text-pf-green', icon: CheckCircle2 },
  STANDARD: { className: 'bg-slate-100 text-slate-700', icon: Info },
  PREMIUM: { className: 'bg-emerald-50 text-pf-green', icon: CheckCircle2 },
  LOW: { className: 'bg-slate-100 text-slate-700', icon: Info },
  MEDIUM: { className: 'bg-amber-50 text-amber-800', icon: AlertTriangle },
  HIGH: { className: 'bg-amber-50 text-amber-800', icon: AlertTriangle },
}

type StatusBadgeProps = {
  value: string
}

export function StatusBadge({ value }: StatusBadgeProps) {
  const { label } = useFormat()
  const style = tone[value] ?? { className: 'bg-slate-100 text-slate-700', icon: Info }
  const Icon = style.icon
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${style.className}`}
    >
      <Icon className="h-3.5 w-3.5" aria-hidden />
      {label(value)}
    </span>
  )
}
