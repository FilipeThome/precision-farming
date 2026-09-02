import { useState } from 'react'

import { DispatchLoadButton } from '@/features/harvest/components/DispatchLoadButton'
import { StorageLotsList } from '@/features/harvest/components/StorageLotsList'
import {
  useHarvestPlansQuery,
  useLogisticsLoadsQuery,
  useStorageLotsQuery,
  useStorageUnitsQuery,
  useYieldQuery,
} from '@/features/harvest/queries'
import { storageOccupancy, yieldByField } from '@/shared/charts/adapters'
import { cropPhoto, logisticsPhoto, storagePhoto } from '@/shared/demo/media'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDateTime, formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { Button } from '@/shared/ui/Button'
import { CHART_COLORS, ChartCard } from '@/shared/ui/ChartCard'
import { BarChartBlock } from '@/shared/ui/charts'
import { EntityCard } from '@/shared/ui/EntityCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SectionTabs } from '@/shared/ui/SectionTabs'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

type Tab = 'plans' | 'yield' | 'logistics' | 'storage'
type StorageView = 'units' | 'lots'

export function HarvestPage() {
  const farmId = useUiStore((s) => s.farmId)
  const [tab, setTab] = useState<Tab>('plans')
  const [storageView, setStorageView] = useState<StorageView>('units')
  const { t } = useI18n()
  const plans = useHarvestPlansQuery(farmId, { enabled: tab === 'plans' })
  const yieldQ = useYieldQuery(farmId, { enabled: tab === 'yield' })
  const logistics = useLogisticsLoadsQuery(farmId, { enabled: tab === 'logistics' })
  const storage = useStorageUnitsQuery(farmId, {
    enabled: tab === 'storage' && storageView === 'units',
  })
  const lots = useStorageLotsQuery(farmId, {
    enabled: tab === 'storage' && storageView === 'lots',
  })

  const active =
    tab === 'plans'
      ? plans
      : tab === 'yield'
        ? yieldQ
        : tab === 'logistics'
          ? logistics
          : storageView === 'lots'
            ? lots
            : storage
  const err = queryError(active.error)

  const emptyTitle =
    tab === 'plans'
      ? t('harvest.plans.emptyTitle')
      : tab === 'yield'
        ? t('harvest.yield.emptyTitle')
        : tab === 'logistics'
          ? t('harvest.logistics.emptyTitle')
          : storageView === 'lots'
            ? t('harvest.lots.emptyTitle')
            : t('harvest.storage.emptyTitle')
  const emptyDescription =
    tab === 'plans'
      ? t('harvest.plans.emptyDescription')
      : tab === 'yield'
        ? t('harvest.yield.emptyDescription')
        : tab === 'logistics'
          ? t('harvest.logistics.emptyDescription')
          : storageView === 'lots'
            ? t('harvest.lots.emptyDescription')
            : t('harvest.storage.emptyDescription')

  return (
    <section>
      <PageHeader title={t('harvest.title')} description={t('harvest.description')} />
      <SectionTabs
        active={tab}
        onChange={setTab}
        tabs={[
          { id: 'plans', labelKey: 'harvest.tab.plans' },
          { id: 'yield', labelKey: 'harvest.tab.yield' },
          { id: 'logistics', labelKey: 'harvest.tab.logistics' },
          { id: 'storage', labelKey: 'harvest.tab.storage' },
        ]}
      />
      {tab === 'storage' ? (
        <div className="mb-4 flex flex-wrap gap-2">
          <Button
            variant={storageView === 'units' ? 'primary' : 'secondary'}
            aria-pressed={storageView === 'units'}
            onClick={() => setStorageView('units')}
          >
            {t('harvest.storage.units')}
          </Button>
          <Button
            variant={storageView === 'lots' ? 'primary' : 'secondary'}
            aria-pressed={storageView === 'lots'}
            onClick={() => setStorageView('lots')}
          >
            {t('harvest.storage.lots')}
          </Button>
        </div>
      ) : null}
      <QueryPageState
        isLoading={active.isLoading}
        isError={active.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!active.isLoading && (active.data?.length ?? 0) === 0}
        emptyTitle={emptyTitle}
        emptyDescription={emptyDescription}
        onRetry={() => void active.refetch()}
      >
        {tab === 'yield' && (yieldQ.data?.length ?? 0) > 0 ? (
          <ChartCard title={t('charts.yieldByField')} className="mb-4">
            <BarChartBlock
              data={yieldByField(yieldQ.data ?? [])}
              xKey="name"
              bars={[{ dataKey: 'yield', name: t('charts.yield'), color: CHART_COLORS.green }]}
            />
          </ChartCard>
        ) : null}
        {tab === 'storage' && storageView === 'units' && (storage.data?.length ?? 0) > 0 ? (
          <ChartCard title={t('charts.storageOccupancy')} className="mb-4">
            <BarChartBlock
              data={storageOccupancy(storage.data ?? [])}
              xKey="name"
              bars={[
                { dataKey: 'used', name: t('charts.used'), color: CHART_COLORS.teal },
                { dataKey: 'capacity', name: t('charts.capacity'), color: CHART_COLORS.slate },
              ]}
            />
          </ChartCard>
        ) : null}
        {tab === 'storage' && storageView === 'lots' ? (
          <StorageLotsList items={lots.data ?? []} />
        ) : (
          <div className="grid gap-3 md:grid-cols-2">
            {tab === 'plans'
              ? (plans.data ?? []).map((row) => (
                  <EntityCard
                    key={row.id}
                    title={row.crop ?? row.id}
                    subtitle={
                      row.expectedTHa != null
                        ? t('harvest.plans.expected', {
                            value: formatNumber(Number(row.expectedTHa), 1),
                          })
                        : undefined
                    }
                    meta={formatDateTime(row.plannedStart)}
                    imageSrc={cropPhoto(row.crop)}
                    imageAlt={row.crop ?? 'harvest'}
                  >
                    {row.status ? <StatusBadge value={row.status} /> : null}
                  </EntityCard>
                ))
              : null}
            {tab === 'yield'
              ? (yieldQ.data ?? []).map((row) => (
                  <EntityCard
                    key={row.id}
                    title={
                      row.yieldTHa != null
                        ? t('harvest.yield.value', {
                            value: formatNumber(Number(row.yieldTHa), 1),
                          })
                        : row.id
                    }
                    subtitle={
                      row.moisturePct != null
                        ? `${formatNumber(Number(row.moisturePct), 1)}%`
                        : undefined
                    }
                    meta={formatDateTime(row.recordedAt)}
                    imageSrc={cropPhoto()}
                    imageAlt="yield"
                  />
                ))
              : null}
            {tab === 'logistics'
              ? (logistics.data ?? []).map((row) => (
                  <EntityCard
                    key={row.id}
                    title={row.destination ?? row.id}
                    subtitle={
                      row.truckPlate
                        ? t('harvest.logistics.truck', { plate: row.truckPlate })
                        : undefined
                    }
                    meta={formatDateTime(row.dispatchedAt)}
                    imageSrc={logisticsPhoto()}
                    imageAlt={row.destination ?? 'logistics'}
                  >
                    {row.status ? <StatusBadge value={row.status} /> : null}
                    {row.status === 'QUEUED' ? <DispatchLoadButton loadId={row.id} /> : null}
                  </EntityCard>
                ))
              : null}
            {tab === 'storage' && storageView === 'units'
              ? (storage.data ?? []).map((row) => (
                  <EntityCard
                    key={row.id}
                    title={row.name ?? row.id}
                    subtitle={
                      row.capacityT != null
                        ? t('harvest.storage.occupancy', {
                            used: formatNumber(Number(row.usedT ?? 0), 1),
                            capacity: formatNumber(Number(row.capacityT), 1),
                          })
                        : row.type
                    }
                    imageSrc={storagePhoto(row.type)}
                    imageAlt={row.name ?? 'storage'}
                  />
                ))
              : null}
          </div>
        )}
      </QueryPageState>
    </section>
  )
}
