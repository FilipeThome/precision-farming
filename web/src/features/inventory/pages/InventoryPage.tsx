import { useInventoryQuery } from '@/features/inventory/queries'
import { inventoryStockBars } from '@/shared/charts/adapters'
import { inventoryPhoto } from '@/shared/demo/media'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { CHART_COLORS, ChartCard } from '@/shared/ui/ChartCard'
import { BarChartBlock } from '@/shared/ui/charts'
import { EntityCard } from '@/shared/ui/EntityCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

export function InventoryPage() {
  const farmId = useUiStore((s) => s.farmId)
  const inventory = useInventoryQuery(farmId)
  const err = queryError(inventory.error)
  const { t } = useI18n()
  const { number, label } = useFormat()

  return (
    <section>
      <PageHeader title={t('inventory.title')} description={t('inventory.description')} />
      <QueryPageState
        isLoading={inventory.isLoading}
        isError={inventory.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!inventory.isLoading && (inventory.data?.length ?? 0) === 0}
        emptyTitle={t('inventory.emptyTitle')}
        emptyDescription={t('inventory.emptyDescription')}
        onRetry={() => void inventory.refetch()}
      >
        {(inventory.data?.length ?? 0) > 0 ? (
          <ChartCard title={t('charts.inventoryStock')} className="mb-4">
            <BarChartBlock
              data={inventoryStockBars(inventory.data ?? [], label)}
              xKey="name"
              bars={[
                { dataKey: 'stock', name: t('charts.stock'), color: CHART_COLORS.green },
                { dataKey: 'reserved', name: t('charts.reserved'), color: CHART_COLORS.amber },
              ]}
            />
          </ChartCard>
        ) : null}
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {(inventory.data ?? []).map((item) => (
            <EntityCard
              key={item.id}
              title={label(item.name)}
              subtitle={label(item.category)}
              meta={`${number(Number(item.quantity), 1)} ${item.unit} · ${t('charts.reserved')} ${number(Number(item.reserved), 1)}`}
              imageSrc={inventoryPhoto()}
              imageAlt={label(item.name)}
            />
          ))}
        </div>
      </QueryPageState>
    </section>
  )
}
