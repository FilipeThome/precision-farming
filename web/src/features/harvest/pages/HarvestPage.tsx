import { HarvestPlanInspector } from '@/features/harvest/components/HarvestPlanInspector'
import {
  HarvestLogisticsGrid,
  HarvestLotsGrid,
  HarvestPlanGrid,
  HarvestStorageGrid,
  HarvestYieldGrid,
} from '@/features/harvest/components/HarvestGrids'
import { StorageLotInspector } from '@/features/harvest/components/StorageLotInspector'
import {
  useHarvestPlansQuery,
  useLogisticsLoadsQuery,
  useStorageLotsQuery,
  useStorageUnitsQuery,
  useYieldQuery,
} from '@/features/harvest/queries'
import { storageOccupancy, yieldByField } from '@/shared/charts/adapters'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { usePatchSearchParams, useSearchParam } from '@/shared/lib/useSearchParam'
import { useSelectedId } from '@/shared/lib/useSelectedId'
import { Button } from '@/shared/ui/Button'
import { CHART_COLORS, ChartCard } from '@/shared/ui/ChartCard'
import { BarChartBlock } from '@/shared/ui/charts'
import { DetailDrawer } from '@/shared/ui/DetailDrawer'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SectionTabs } from '@/shared/ui/SectionTabs'
import { useUiStore } from '@/shared/ui/uiStore'

const TABS = ['plans', 'yield', 'logistics', 'storage'] as const
type Tab = (typeof TABS)[number]
type StorageView = 'units' | 'lots'

export function HarvestPage() {
  const farmId = useUiStore((s) => s.farmId)
  const [tabParam] = useSearchParam('tab')
  const [viewParam] = useSearchParam('view')
  const patchParams = usePatchSearchParams()
  const { selectedId, setSelectedId } = useSelectedId()
  const tab: Tab = TABS.includes(tabParam as Tab) ? (tabParam as Tab) : 'plans'
  const storageView: StorageView = viewParam === 'lots' ? 'lots' : 'units'
  const { t } = useI18n()
  const { label } = useFormat()
  const plans = useHarvestPlansQuery(farmId, { enabled: tab === 'plans' })
  const yieldQ = useYieldQuery(farmId, { enabled: tab === 'yield' })
  const logistics = useLogisticsLoadsQuery(farmId, { enabled: tab === 'logistics' })
  const storage = useStorageUnitsQuery(farmId, { enabled: tab === 'storage' && storageView === 'units' })
  const lots = useStorageLotsQuery(farmId, { enabled: tab === 'storage' && storageView === 'lots' })
  const active =
    tab === 'plans' ? plans : tab === 'yield' ? yieldQ : tab === 'logistics' ? logistics : storageView === 'lots' ? lots : storage
  const err = queryError(active.error)
  const selectedPlan = (plans.data ?? []).find((row) => row.id === selectedId)
  const selectedLot = (lots.data ?? []).find((row) => row.id === selectedId)

  return (
    <section>
      <PageHeader title={t('harvest.title')} description={t('harvest.description')} />
      <SectionTabs
        active={tab}
        onChange={(next) => patchParams({ selected: null, tab: next })}
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
            onClick={() => patchParams({ selected: null, view: 'units' })}
          >
            {t('harvest.storage.units')}
          </Button>
          <Button
            variant={storageView === 'lots' ? 'primary' : 'secondary'}
            aria-pressed={storageView === 'lots'}
            onClick={() => patchParams({ selected: null, view: 'lots' })}
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
        emptyTitle={
          tab === 'plans'
            ? t('harvest.plans.emptyTitle')
            : tab === 'yield'
              ? t('harvest.yield.emptyTitle')
              : tab === 'logistics'
                ? t('harvest.logistics.emptyTitle')
                : storageView === 'lots'
                  ? t('harvest.lots.emptyTitle')
                  : t('harvest.storage.emptyTitle')
        }
        emptyDescription={
          tab === 'plans'
            ? t('harvest.plans.emptyDescription')
            : tab === 'yield'
              ? t('harvest.yield.emptyDescription')
              : tab === 'logistics'
                ? t('harvest.logistics.emptyDescription')
                : storageView === 'lots'
                  ? t('harvest.lots.emptyDescription')
                  : t('harvest.storage.emptyDescription')
        }
        onRetry={() => void active.refetch()}
      >
        {tab === 'yield' && (yieldQ.data?.length ?? 0) > 0 ? (
          <ChartCard title={t('charts.yieldByField')} className="mb-4">
            <BarChartBlock
              data={yieldByField(yieldQ.data ?? [], label)}
              xKey="name"
              bars={[{ dataKey: 'yield', name: t('charts.yield'), color: CHART_COLORS.green }]}
            />
          </ChartCard>
        ) : null}
        {tab === 'storage' && storageView === 'units' && (storage.data?.length ?? 0) > 0 ? (
          <ChartCard title={t('charts.storageOccupancy')} className="mb-4">
            <BarChartBlock
              data={storageOccupancy(storage.data ?? [], label)}
              xKey="name"
              bars={[
                { dataKey: 'used', name: t('charts.used'), color: CHART_COLORS.teal },
                { dataKey: 'capacity', name: t('charts.capacity'), color: CHART_COLORS.slate },
              ]}
            />
          </ChartCard>
        ) : null}
        {tab === 'plans' ? (
          <HarvestPlanGrid items={plans.data ?? []} selectedId={selectedId} onSelect={setSelectedId} />
        ) : null}
        {tab === 'yield' ? <HarvestYieldGrid items={yieldQ.data ?? []} /> : null}
        {tab === 'logistics' ? <HarvestLogisticsGrid items={logistics.data ?? []} /> : null}
        {tab === 'storage' && storageView === 'lots' ? (
          <HarvestLotsGrid items={lots.data ?? []} selectedId={selectedId} onSelect={setSelectedId} />
        ) : null}
        {tab === 'storage' && storageView === 'units' ? (
          <HarvestStorageGrid items={storage.data ?? []} />
        ) : null}
      </QueryPageState>
      <DetailDrawer
        open={Boolean(selectedId) && (tab === 'plans' || (tab === 'storage' && storageView === 'lots'))}
        title={
          selectedPlan
            ? label(selectedPlan.crop, selectedPlan.fieldId ?? selectedPlan.id)
            : selectedLot
              ? label(selectedLot.crop)
              : t('inspector.notFound')
        }
        subtitle={!selectedPlan && !selectedLot ? t('inspector.notFoundHint') : undefined}
        onClose={() => setSelectedId(null)}
      >
        {selectedPlan ? <HarvestPlanInspector plan={selectedPlan} farmId={farmId} /> : null}
        {selectedLot ? <StorageLotInspector lot={selectedLot} /> : null}
      </DetailDrawer>
    </section>
  )
}
