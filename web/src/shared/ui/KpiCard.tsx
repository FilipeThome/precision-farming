import type { LucideIcon } from 'lucide-react'
import { Link } from 'react-router'

import { Card } from './Card'

type KpiCardProps = {
  label: string
  value: string | number
  icon: LucideIcon
  hint?: string
  to?: string
}

export function KpiCard({ label, value, icon: Icon, hint, to }: KpiCardProps) {
  const card = (
    <Card className={`flex items-start gap-3 ${to ? 'transition hover:border-ag-t-500' : ''}`}>
      <span className="rounded-[10px] bg-ag-g-100 p-2 text-ag-g-700">
        <Icon className="h-5 w-5" aria-hidden />
      </span>
      <div className="min-w-0">
        <p className="text-xs font-medium text-ag-n-600">{label}</p>
        <p className="font-display text-[26px] font-extrabold leading-none tracking-[-0.03em] text-ag-g-900 tnum">{value}</p>
        {hint ? <p className="mt-1 text-[11px] text-ag-n-600">{hint}</p> : null}
      </div>
    </Card>
  )

  if (!to) return card
  return (
    <Link to={to} aria-label={`${label}: ${value}`} className="block rounded-[14px]">
      {card}
    </Link>
  )
}
