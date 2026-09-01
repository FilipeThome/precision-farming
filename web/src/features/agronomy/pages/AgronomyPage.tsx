import { useState } from 'react'

import {
  useRecommendationsQuery,
  useScoutingQuery,
  useSoilSamplesQuery,
} from '@/features/agronomy/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDateTime, formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { EntityCard } from '@/shared/ui/EntityCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SectionTabs } from '@/shared/ui/SectionTabs'
import { useUiStore } from '@/shared/ui/uiStore'

type Tab = 'scouting' | 'soil' | 'recommendations'

export function AgronomyPage() {
  const farmId = useUiStore((s) => s.farmId)
  const [tab, setTab] = useState<Tab>('scouting')
  const { t } = useI18n()
  const scouting = useScoutingQuery(farmId)
  const soil = useSoilSamplesQuery(farmId)
  const recommendations = useRecommendationsQuery(farmId)

  const active =
    tab === 'scouting' ? scouting : tab === 'soil' ? soil : recommendations
  const err = queryError(active.error)

  return (
    <section>
      <PageHeader title={t('agronomy.title')} description={t('agronomy.description')} />
      <SectionTabs
        active={tab}
        onChange={setTab}
        tabs={[
          { id: 'scouting', labelKey: 'agronomy.tab.scouting' },
          { id: 'soil', labelKey: 'agronomy.tab.soil' },
          { id: 'recommendations', labelKey: 'agronomy.tab.recommendations' },
        ]}
      />
      <QueryPageState
        isLoading={active.isLoading}
        isError={active.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!active.isLoading && (active.data?.length ?? 0) === 0}
        emptyTitle={
          tab === 'scouting'
            ? t('agronomy.scouting.emptyTitle')
            : tab === 'soil'
              ? t('agronomy.soil.emptyTitle')
              : t('agronomy.recommendations.emptyTitle')
        }
        emptyDescription={
          tab === 'scouting'
            ? t('agronomy.scouting.emptyDescription')
            : tab === 'soil'
              ? t('agronomy.soil.emptyDescription')
              : t('agronomy.recommendations.emptyDescription')
        }
        onRetry={() => void active.refetch()}
      >
        <div className="grid gap-3 md:grid-cols-2">
          {tab === 'scouting'
            ? (scouting.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.pest ?? row.id}
                  subtitle={row.notes}
                  meta={`${row.severity ?? '—'} · ${formatDateTime(row.observedAt)}`}
                />
              ))
            : null}
          {tab === 'soil'
            ? (soil.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.lab ?? row.id}
                  subtitle={
                    row.ph != null
                      ? `pH ${formatNumber(Number(row.ph), 1)} · OM ${formatNumber(Number(row.organicMatterPct ?? 0), 1)}%`
                      : undefined
                  }
                  meta={formatDateTime(row.sampledAt)}
                />
              ))
            : null}
          {tab === 'recommendations'
            ? (recommendations.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.type ?? row.id}
                  subtitle={row.summary}
                  meta={`${row.priority ?? '—'} · ${formatDateTime(row.createdAt)}`}
                />
              ))
            : null}
        </div>
      </QueryPageState>
    </section>
  )
}
