import type { KeyboardEvent, ReactNode } from 'react'

import { Card } from '@/shared/ui/Card'
import { EntityTile, type EntityKind, type EntityTone } from '@/shared/ui/EntityTile'

type Props = {
  title: string
  subtitle?: string
  meta?: string
  /** Icon tile kind; when omitted no tile is rendered. */
  kind?: EntityKind
  machineType?: string | null
  photoUrl?: string | null
  tone?: EntityTone
  children?: ReactNode
  selected?: boolean
  onSelect?: () => void
}

export function EntityCard({ title, subtitle, meta, kind, machineType, photoUrl, tone, children, selected, onSelect }: Props) {
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
      className={`flex gap-3 p-3 ${
        interactive ? 'cursor-pointer transition hover:border-ag-t-500 hover:shadow-[0_6px_16px_-6px_rgba(20,30,25,0.16)]' : ''
      } ${selected ? 'ring-2 ring-ag-t-500' : ''}`}
      role={interactive ? 'button' : undefined}
      tabIndex={interactive ? 0 : undefined}
      aria-pressed={interactive ? Boolean(selected) : undefined}
      onClick={onSelect}
      onKeyDown={onKeyDown}
    >
      {kind ? <EntityTile kind={kind} machineType={machineType} tone={tone} photoUrl={photoUrl} /> : null}
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <h2 className="font-display text-[13px] font-bold text-ag-n-900">{title}</h2>
        {subtitle ? <p className="text-[12px] text-ag-n-600">{subtitle}</p> : null}
        {meta ? <p className="text-[11px] text-ag-n-500">{meta}</p> : null}
        {children ? (
          <div onClick={(event) => event.stopPropagation()} onKeyDown={(event) => event.stopPropagation()}>
            {children}
          </div>
        ) : null}
      </div>
    </Card>
  )
}
