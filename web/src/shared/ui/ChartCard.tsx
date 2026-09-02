import type { ReactNode } from 'react'

import { Card } from '@/shared/ui/Card'

type ChartCardProps = {
  title: string
  description?: string
  children: ReactNode
  className?: string
}

export function ChartCard({ title, description, children, className = '' }: ChartCardProps) {
  return (
    <Card className={`min-h-[280px] ${className}`}>
      <h3 className="text-sm font-semibold text-pf-green">{title}</h3>
      {description ? <p className="mt-1 text-xs text-pf-muted">{description}</p> : null}
      <div className="mt-3 h-56 w-full">{children}</div>
    </Card>
  )
}

export const CHART_COLORS = {
  green: '#1b5e3b',
  teal: '#0d9488',
  muted: '#5c6b63',
  amber: '#d97706',
  red: '#b91c1c',
  blue: '#1d4ed8',
  slate: '#64748b',
} as const

export const CHART_SERIES = [
  CHART_COLORS.green,
  CHART_COLORS.teal,
  CHART_COLORS.amber,
  CHART_COLORS.blue,
  CHART_COLORS.red,
  CHART_COLORS.slate,
]
