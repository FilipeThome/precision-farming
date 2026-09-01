import { useState } from 'react'

import {
  useHarvestPlansQuery,
  useLogisticsLoadsQuery,
  useStorageUnitsQuery,
  useYieldQuery,
} from '@/features/harvest/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDateTime, formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { EntityCard } from '@/shared/ui/EntityCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SectionTabs } from '@/shared/ui/SectionTabs'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

type Tab = 'plans' | 'yield' | 'logistics' | 'storage'

export function HarvestPage() {
  const farmId = useUiStore((s) => s.farmId)
  const [tab, setTab] = useState<Tab>('plans')
  const { t } = useI18n()
  const plans = useHarvestPlansQuery(farmId)
  const yieldQ = useYieldQuery(farmId)
  const logistics = useLogisticsLoadsQuery(farmId)
  const storage = useStorageUnitsQuery(farmId)

  const active =
    tab === 'plans' ? plans : tab === 'yield' ? yieldQ : tab === 'logistics' ? logistics : storage
  const err = queryError(active.error)

  const emptyTitle =
    tab === 'plans'
      ? t('harvest.plans.emptyTitle')
      : tab === 'yield'
        ? t('harvest.yield.emptyTitle')
        : tab === 'logistics'
          ? t('harvest.logistics.emptyTitle')
          : t('harvest.storage.emptyTitle')
  const emptyDescription =
    tab === 'plans'
      ? t('harvest.plans.emptyDescription')
      : tab === 'yield'
        ? t('harvest.yield.emptyDescription')
        : tab === 'logistics'
          ? t('harvest.logistics.emptyDescription')
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
        <div className="grid gap-3 md:grid-cols-2">
          {tab === 'plans'
            ? (plans.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.crop ?? row.id}
                  subtitle={
                    row.estimatedTons != null
                      ? `${formatNumber(Number(row.estimatedTons), 1)} t`
                      : undefined
                  }
                  meta={formatDateTime(row.plannedStart)}
                >
                  {row.status ? <StatusBadge value={row.status} /> : null}
                </EntityCard>
              ))
            : null}
          {tab === 'yield'
            ? (yieldQ.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.crop ?? row.id}
                  subtitle={
                    row.tons != null ? `${formatNumber(Number(row.tons), 1)} t` : undefined
                  }
                  meta={formatDateTime(row.harvestedAt)}
                />
              ))
            : null}
          {tab === 'logistics'
            ? (logistics.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.destination ?? row.id}
                  subtitle={row.origin}
                  meta={formatDateTime(row.dispatchedAt)}
                >
                  {row.status ? <StatusBadge value={row.status} /> : null}
                </EntityCard>
              ))
            : null}
          {tab === 'storage'
            ? (storage.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.name ?? row.id}
                  subtitle={
                    row.capacityTons != null
                      ? `${formatNumber(Number(row.occupiedTons ?? 0), 1)} / ${formatNumber(Number(row.capacityTons), 1)} t`
                      : row.type
                  }
                >
                  {row.status ? <StatusBadge value={row.status} /> : null}
                </EntityCard>
              ))
            : null}
        </div>
      </QueryPageState>
    </section>
  )
}
