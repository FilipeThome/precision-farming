import type { TraceabilityLot } from '@/shared/api/types'
import { formatDateTime } from '@/shared/lib/format'
import { EntityCard } from '@/shared/ui/EntityCard'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = {
  events: TraceabilityLot[]
}

export function TraceabilityChain({ events }: Props) {
  const ordered = [...events].sort(
    (a, b) => new Date(a.occurredAt).getTime() - new Date(b.occurredAt).getTime(),
  )

  return (
    <ol className="relative flex flex-col gap-3 border-l border-pf-border pl-4">
      {ordered.map((event) => (
        <li key={event.id} className="relative">
          <span className="absolute -left-[1.35rem] top-3 h-2.5 w-2.5 rounded-full bg-pf-teal" />
          <EntityCard
            title={event.eventType}
            subtitle={event.summary}
            meta={formatDateTime(event.occurredAt)}
          >
            <StatusBadge value={event.crop} />
          </EntityCard>
        </li>
      ))}
    </ol>
  )
}
