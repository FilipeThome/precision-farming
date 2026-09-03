import type { WeatherWindow } from '@/shared/api/types'
import { weatherPhoto } from '@/shared/demo/media'
import { useFormat } from '@/shared/lib/useFormat'
import { EntityCard } from '@/shared/ui/EntityCard'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = {
  items: WeatherWindow[]
}

export function WeatherWindowsList({ items }: Props) {
  const { dateTime, label } = useFormat()
  return (
    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      {items.map((row) => (
        <EntityCard
          key={row.id}
          title={label(row.windowType)}
          subtitle={row.notes ? label(row.notes) : undefined}
          meta={`${dateTime(row.startAt)} → ${dateTime(row.endAt)}`}
          imageSrc={weatherPhoto()}
          imageAlt={label(row.windowType)}
        >
          <StatusBadge value={row.rating} />
        </EntityCard>
      ))}
    </div>
  )
}
