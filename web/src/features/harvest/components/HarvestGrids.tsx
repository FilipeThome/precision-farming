import { DispatchLoadButton } from '@/features/harvest/components/DispatchLoadButton'
import { StorageLotsList } from '@/features/harvest/components/StorageLotsList'
import type { HarvestPlan, LogisticsLoad, StorageLot, StorageUnit, YieldRecord } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { EntityCard } from '@/shared/ui/EntityCard'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Selectable = { selectedId?: string | null; onSelect?: (id: string) => void }

export function HarvestPlanGrid({
  items,
  selectedId,
  onSelect,
}: { items: HarvestPlan[] } & Selectable) {
  const { t } = useI18n()
  const { number, dateTime, label } = useFormat()
  return (
    <div className="grid gap-3 md:grid-cols-2">
      {items.map((row) => (
        <EntityCard
          key={row.id}
          title={label(row.crop, row.fieldId ?? row.id)}
          subtitle={
            row.expectedTHa != null
              ? t('harvest.plans.expected', { value: number(Number(row.expectedTHa), 1) })
              : undefined
          }
          meta={dateTime(row.plannedStart)}
          kind="crop"
          selected={row.id === selectedId}
          onSelect={() => onSelect?.(row.id)}
        >
          {row.status ? <StatusBadge value={row.status} /> : null}
        </EntityCard>
      ))}
    </div>
  )
}

export function HarvestYieldGrid({ items }: { items: YieldRecord[] }) {
  const { t } = useI18n()
  const { number, dateTime, label } = useFormat()
  return (
    <div className="grid gap-3 md:grid-cols-2">
      {items.map((row) => (
        <EntityCard
          key={row.id}
          title={label(row.fieldId, row.id)}
          subtitle={
            row.yieldTHa != null
              ? t('harvest.yield.value', { value: number(Number(row.yieldTHa), 1) })
              : undefined
          }
          meta={
            row.moisturePct != null
              ? `${number(Number(row.moisturePct), 1)}% · ${dateTime(row.recordedAt)}`
              : dateTime(row.recordedAt)
          }
          kind="field"
        />
      ))}
    </div>
  )
}

export function HarvestLogisticsGrid({ items }: { items: LogisticsLoad[] }) {
  const { t } = useI18n()
  const { dateTime, label } = useFormat()
  return (
    <div className="grid gap-3 md:grid-cols-2">
      {items.map((row) => (
        <EntityCard
          key={row.id}
          title={label(row.destination, row.id)}
          subtitle={
            row.truckPlate ? t('harvest.logistics.truck', { plate: row.truckPlate }) : undefined
          }
          meta={dateTime(row.dispatchedAt)}
          kind="truck"
        >
          {row.status ? <StatusBadge value={row.status} /> : null}
          {row.status === 'QUEUED' ? <DispatchLoadButton loadId={row.id} /> : null}
        </EntityCard>
      ))}
    </div>
  )
}

export function HarvestStorageGrid({
  items,
  selectedId,
  onSelect,
}: { items: StorageUnit[] } & Selectable) {
  const { t } = useI18n()
  const { number, label } = useFormat()
  return (
    <div className="grid gap-3 md:grid-cols-2">
      {items.map((row) => (
        <EntityCard
          key={row.id}
          title={label(row.name, row.id)}
          subtitle={
            row.capacityT != null
              ? t('harvest.storage.occupancy', {
                  used: number(Number(row.usedT ?? 0), 1),
                  capacity: number(Number(row.capacityT), 1),
                })
              : label(row.type)
          }
          kind="storage"
          selected={row.id === selectedId}
          onSelect={() => onSelect?.(row.id)}
        />
      ))}
    </div>
  )
}

export function HarvestLotsGrid({
  items,
  selectedId,
  onSelect,
}: { items: StorageLot[] } & Selectable) {
  return <StorageLotsList items={items} selectedId={selectedId} onSelect={onSelect} />
}
