import type { LucideIcon } from 'lucide-react'

import { Card } from './Card'

type KpiCardProps = {
  label: string
  value: string | number
  icon: LucideIcon
  hint?: string
}

export function KpiCard({ label, value, icon: Icon, hint }: KpiCardProps) {
  return (
    <Card className="flex items-start gap-3">
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
}
