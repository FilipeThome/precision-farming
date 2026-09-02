import type { ReactNode } from 'react'

import { Card } from '@/shared/ui/Card'
import { EntityPhoto } from '@/shared/ui/EntityPhoto'

type Props = {
  title: string
  subtitle?: string
  meta?: string
  imageSrc?: string
  imageAlt?: string
  children?: ReactNode
}

export function EntityCard({ title, subtitle, meta, imageSrc, imageAlt, children }: Props) {
  return (
    <Card className={imageSrc ? 'flex gap-3 p-3' : 'flex flex-col gap-1'}>
      {imageSrc ? <EntityPhoto src={imageSrc} alt={imageAlt ?? title} /> : null}
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <h2 className="font-semibold text-pf-green">{title}</h2>
        {subtitle ? <p className="text-sm text-pf-muted">{subtitle}</p> : null}
        {meta ? <p className="text-xs text-pf-muted">{meta}</p> : null}
        {children}
      </div>
    </Card>
  )
}
