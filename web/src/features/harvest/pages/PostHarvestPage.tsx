import { useFieldsQuery } from '@/features/fields/queries'
import { useFinancePnlQuery } from '@/features/finance/queries'
import { HarvestFlowStrip } from '@/features/harvest/components/HarvestFlowStrip'
import { LoadsList } from '@/features/harvest/components/LoadsList'
import { LotsQualityTable } from '@/features/harvest/components/LotsQualityTable'
import { MarginByField } from '@/features/harvest/components/MarginByField'
import { PostHarvestKpis } from '@/features/harvest/components/PostHarvestKpis'
import { SiloCards } from '@/features/harvest/components/SiloCards'
import { expectedTons, flowStages, harvestedTons, loadsSummary, storageOccupancyTotal } from '@/features/harvest/model/postHarvestKpis'
import {
  useHarvestPlansQuery,
  useLogisticsLoadsQuery,
  useStorageLotsQuery,
  useStorageUnitsQuery,
  useYieldQuery,
} from '@/features/harvest/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

export function PostHarvestPage() {
  const farmId = useUiStore((s) => s.farmId)
  const { t } = useI18n()

  const plans = useHarvestPlansQuery(farmId)
  const yields = useYieldQuery(farmId)
  const loads = useLogisticsLoadsQuery(farmId)
  const units = useStorageUnitsQuery(farmId)
  const lots = useStorageLotsQuery(farmId)
  const fields = useFieldsQuery(farmId)
  const pnl = useFinancePnlQuery(farmId)

  const plansErr = queryError(plans.error)
  const planList = plans.data ?? []
  const loadList = loads.data ?? []
  const lotList = lots.data ?? []

  const kpiLoading = plans.isLoading || yields.isLoading || loads.isLoading || units.isLoading || fields.isLoading
  const allEmpty =
    !kpiLoading &&
    !lots.isLoading &&
    planList.length === 0 &&
    (yields.data?.length ?? 0) === 0 &&
    loadList.length === 0 &&
    (units.data?.length ?? 0) === 0 &&
    lotList.length === 0

  return (
    <section className="flex flex-col gap-3.5">
      <PageHeader title={t('postHarvest.title')} description={t('postHarvest.description')} />
      <QueryPageState
        isLoading={plans.isLoading}
        isError={plans.isError}
        errorMessage={plansErr.message}
        correlationId={plansErr.correlationId}
        isEmpty={allEmpty}
        emptyTitle={t('postHarvest.emptyTitle')}
        emptyDescription={t('postHarvest.emptyDescription')}
        onRetry={() => void plans.refetch()}
      >
        <HarvestFlowStrip stages={flowStages(planList, loadList, lotList)} />
        <PostHarvestKpis
          expected={expectedTons(planList, fields.data ?? [])}
          harvested={harvestedTons(yields.data ?? [])}
          loads={loadsSummary(loadList)}
          occupancy={storageOccupancyTotal(units.data ?? [])}
          isLoading={kpiLoading}
        />
        <div className="grid gap-3.5 xl:grid-cols-[1fr_1fr]">
          <LoadsList loads={loadList} farmId={farmId} isLoading={loads.isLoading} isError={loads.isError} />
          <SiloCards units={units.data ?? []} farmId={farmId} isLoading={units.isLoading} isError={units.isError} />
          <LotsQualityTable lots={lotList} isLoading={lots.isLoading} isError={lots.isError} />
          <MarginByField pnl={pnl.data ?? []} farmId={farmId} isLoading={pnl.isLoading} isError={pnl.isError} />
        </div>
      </QueryPageState>
    </section>
  )
}
