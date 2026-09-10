import type { KeyboardEvent, ReactNode } from 'react'

import { Card } from '@/shared/ui/Card'
import { EntityPhoto } from '@/shared/ui/EntityPhoto'

type Props = {
  title: string
  subtitle?: string
  meta?: string
  imageSrc?: string
  imageAlt?: string
  children?: ReactNode
  selected?: boolean
  onSelect?: () => void
}

export function EntityCard({
  title,
  subtitle,
  meta,
  imageSrc,
  imageAlt,
  children,
  selected,
  onSelect,
}: Props) {
  const interactive = Boolean(onSelect)

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (!onSelect) return
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      onSelect()
    }
  }

  return (
    <Card
      className={`${imageSrc ? 'flex gap-3 p-3' : 'flex flex-col gap-1'} ${
        interactive ? 'cursor-pointer transition hover:border-pf-teal' : ''
      } ${selected ? 'ring-2 ring-pf-teal' : ''}`}
      role={interactive ? 'button' : undefined}
      tabIndex={interactive ? 0 : undefined}
      aria-pressed={interactive ? Boolean(selected) : undefined}
      onClick={onSelect}
      onKeyDown={onKeyDown}
    >
      {imageSrc ? <EntityPhoto src={imageSrc} alt={imageAlt ?? title} /> : null}
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <h2 className="font-semibold text-pf-green">{title}</h2>
        {subtitle ? <p className="text-sm text-pf-muted">{subtitle}</p> : null}
        {meta ? <p className="text-xs text-pf-muted">{meta}</p> : null}
        {children ? (
          <div
            onClick={(event) => event.stopPropagation()}
            onKeyDown={(event) => event.stopPropagation()}
          >
            {children}
          </div>
        ) : null}
      </div>
    </Card>
  )
}
