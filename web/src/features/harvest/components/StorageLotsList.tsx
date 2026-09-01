import type { StorageLot } from '@/shared/api/types'
import { cropPhoto, storagePhoto } from '@/shared/demo/media'
import { formatDateTime, formatNumber } from '@/shared/lib/format'
import { EntityCard } from '@/shared/ui/EntityCard'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = {
  items: StorageLot[]
}

export function StorageLotsList({ items }: Props) {
  return (
    <div className="grid gap-3 md:grid-cols-2">
      {items.map((row) => (
        <EntityCard
          key={row.id}
          title={row.crop}
          subtitle={`${formatNumber(Number(row.tons), 1)} t`}
          meta={formatDateTime(row.receivedAt)}
          imageSrc={cropPhoto(row.crop) || storagePhoto()}
          imageAlt={row.crop}
        >
          <StatusBadge value={row.quality} />
        </EntityCard>
      ))}
    </div>
  )
}
