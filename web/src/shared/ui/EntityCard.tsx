import type { ReactNode } from 'react'

import { Card } from '@/shared/ui/Card'

type Props = {
  title: string
  subtitle?: string
  meta?: string
  children?: ReactNode
}

export function EntityCard({ title, subtitle, meta, children }: Props) {
  return (
    <Card className="flex flex-col gap-1">
      <h2 className="font-semibold text-pf-green">{title}</h2>
      {subtitle ? <p className="text-sm text-pf-muted">{subtitle}</p> : null}
      {meta ? <p className="text-xs text-pf-muted">{meta}</p> : null}
      {children}
    </Card>
  )
}
