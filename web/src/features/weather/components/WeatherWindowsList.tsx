import type { WeatherWindow } from '@/shared/api/types'
import { weatherPhoto } from '@/shared/demo/media'
import { formatDateTime } from '@/shared/lib/format'
import { EntityCard } from '@/shared/ui/EntityCard'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = {
  items: WeatherWindow[]
}

export function WeatherWindowsList({ items }: Props) {
  return (
    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      {items.map((row) => (
        <EntityCard
          key={row.id}
          title={row.windowType}
          subtitle={row.notes ?? undefined}
          meta={`${formatDateTime(row.startAt)} → ${formatDateTime(row.endAt)}`}
          imageSrc={weatherPhoto()}
          imageAlt={row.windowType}
        >
          <StatusBadge value={row.rating} />
        </EntityCard>
      ))}
    </div>
  )
}
