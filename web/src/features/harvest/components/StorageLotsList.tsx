import type { StorageLot } from '@/shared/api/types'
import { cropPhoto, storagePhoto } from '@/shared/demo/media'
import { useFormat } from '@/shared/lib/useFormat'
import { EntityCard } from '@/shared/ui/EntityCard'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = {
  items: StorageLot[]
}

export function StorageLotsList({ items }: Props) {
  const { number, dateTime, label } = useFormat()
  return (
    <div className="grid gap-3 md:grid-cols-2">
      {items.map((row) => (
        <EntityCard
          key={row.id}
          title={label(row.crop)}
          subtitle={`${number(Number(row.tons), 1)} t`}
          meta={dateTime(row.receivedAt)}
          imageSrc={cropPhoto(row.crop) || storagePhoto()}
          imageAlt={label(row.crop)}
        >
          <StatusBadge value={row.quality} />
        </EntityCard>
      ))}
    </div>
  )
}
