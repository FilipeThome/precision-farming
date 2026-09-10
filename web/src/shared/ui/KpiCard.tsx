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
    <Card
      className={`flex items-start gap-3 ${to ? 'transition hover:border-pf-teal' : ''}`}
    >
      <span className="rounded-[12px] bg-pf-green/10 p-2 text-pf-green">
        <Icon className="h-5 w-5" aria-hidden />
      </span>
      <div>
        <p className="text-sm text-pf-muted">{label}</p>
        <p className="text-2xl font-semibold text-pf-green">{value}</p>
        {hint ? <p className="text-xs text-pf-muted">{hint}</p> : null}
      </div>
    </Card>
  )

  if (!to) return card
  return (
    <Link
      to={to}
      aria-label={`${label}: ${value}`}
      className="block rounded-[12px] focus-visible:outline focus-visible:ring-2 focus-visible:ring-pf-teal"
    >
      {card}
    </Link>
  )
}
