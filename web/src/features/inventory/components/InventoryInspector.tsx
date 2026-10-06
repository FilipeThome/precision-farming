import { useInventoryMovementsQuery } from '@/features/inventory/queries'
import type { InventoryItem } from '@/features/inventory/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { useFormat } from '@/shared/lib/useFormat'
import { CHART_COLORS, ChartCard } from '@/shared/ui/ChartCard'
import { BarChartBlock } from '@/shared/ui/charts'
import { EntityTile } from '@/shared/ui/EntityTile'
import { InspectorKpis } from '@/shared/ui/InspectorKpis'

type Props = { item: InventoryItem }

export function InventoryInspector({ item }: Props) {
  const movements = useInventoryMovementsQuery(item.id)
  const { t } = useI18n()
  const { number, label } = useFormat()
  const available = Number(item.quantity) - Number(item.reserved)
  const err = movements.error ? queryError(movements.error) : null
  const consumed = (movements.data ?? [])
    .filter((row) => row.type === 'CONSUME')
    .map((row) => ({ name: row.occurredAt.slice(5, 10), used: Number(row.quantity) }))

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-3">
        <EntityTile kind="inventory" size="lg" label={label(item.name)} />
        <p className="text-sm text-pf-muted">{label(item.category)}</p>
      </div>
      <InspectorKpis
        items={[
          { label: t('inventory.kpi.stock'), value: `${number(Number(item.quantity), 1)} ${item.unit}` },
          { label: t('inventory.kpi.reserved'), value: `${number(Number(item.reserved), 1)} ${item.unit}` },
          { label: t('inventory.kpi.available'), value: `${number(available, 1)} ${item.unit}` },
        ]}
      />
      {movements.isLoading ? <p className="text-sm text-pf-muted">{t('common.loading')}</p> : null}
      {err ? (
        <p className="text-sm text-red-800" role="alert">
          {err.message}
        </p>
      ) : null}
      <ChartCard title={t('inventory.charts.consumption')} description={t('charts.fromLive')} className="min-h-[220px]">
        <BarChartBlock
          data={consumed}
          xKey="name"
          bars={[{ dataKey: 'used', name: t('charts.used'), color: CHART_COLORS.amber }]}
        />
      </ChartCard>
    </div>
  )
}
