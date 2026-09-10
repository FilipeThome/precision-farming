import type { StorageLot } from '@/shared/api/types'
import { cropPhoto, storagePhoto } from '@/shared/demo/media'
import { useFormat } from '@/shared/lib/useFormat'
import { EntityCard } from '@/shared/ui/EntityCard'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = {
  items: StorageLot[]
  selectedId?: string | null
  onSelect?: (id: string) => void
}

export function StorageLotsList({ items, selectedId, onSelect }: Props) {
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
          selected={row.id === selectedId}
          onSelect={onSelect ? () => onSelect(row.id) : undefined}
        >
          <StatusBadge value={row.quality} />
        </EntityCard>
      ))}
    </div>
  )
}
