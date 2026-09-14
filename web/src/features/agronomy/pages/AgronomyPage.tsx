import { useState } from 'react'

import { PrescriptionList } from '@/features/agronomy/components/PrescriptionList'
import {
  usePrescriptionsQuery,
  useRecommendationsQuery,
  useScoutingQuery,
  useSoilSamplesQuery,
} from '@/features/agronomy/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { EntityCard } from '@/shared/ui/EntityCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SectionTabs } from '@/shared/ui/SectionTabs'
import { useUiStore } from '@/shared/ui/uiStore'

type Tab = 'scouting' | 'soil' | 'recommendations' | 'prescriptions'

export function AgronomyPage() {
  const farmId = useUiStore((s) => s.farmId)
  const [tab, setTab] = useState<Tab>('scouting')
  const { t } = useI18n()
  const { dateTime, number, label } = useFormat()
  const scouting = useScoutingQuery(farmId, { enabled: tab === 'scouting' })
  const soil = useSoilSamplesQuery(farmId, { enabled: tab === 'soil' })
  const recommendations = useRecommendationsQuery(farmId, { enabled: tab === 'recommendations' })
  const prescriptions = usePrescriptionsQuery(farmId, { enabled: tab === 'prescriptions' })

  const active =
    tab === 'scouting'
      ? scouting
      : tab === 'soil'
        ? soil
        : tab === 'recommendations'
          ? recommendations
          : prescriptions
  const err = queryError(active.error)

  const emptyTitle =
    tab === 'scouting'
      ? t('agronomy.scouting.emptyTitle')
      : tab === 'soil'
        ? t('agronomy.soil.emptyTitle')
        : tab === 'recommendations'
          ? t('agronomy.recommendations.emptyTitle')
          : t('agronomy.prescriptions.emptyTitle')
  const emptyDescription =
    tab === 'scouting'
      ? t('agronomy.scouting.emptyDescription')
      : tab === 'soil'
        ? t('agronomy.soil.emptyDescription')
        : tab === 'recommendations'
          ? t('agronomy.recommendations.emptyDescription')
          : t('agronomy.prescriptions.emptyDescription')

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
          { id: 'prescriptions', labelKey: 'agronomy.tab.prescriptions' },
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
        {tab === 'prescriptions' ? (
          <PrescriptionList items={prescriptions.data ?? []} />
        ) : (
          <div className="grid gap-3 md:grid-cols-2">
            {tab === 'scouting'
              ? (scouting.data ?? []).map((row) => (
                  <EntityCard
                    key={row.id}
                    title={label(row.pest, row.fieldId ?? row.id)}
                    subtitle={row.notes ? label(row.notes) : undefined}
                    meta={`${label(row.severity)} · ${dateTime(row.observedAt)}`}
                    kind="scouting"
                  />
                ))
              : null}
            {tab === 'soil'
              ? (soil.data ?? []).map((row) => (
                  <EntityCard
                    key={row.id}
                    title={label(row.lab, row.fieldId ?? row.id)}
                    subtitle={
                      row.ph != null
                        ? t('agronomy.soil.phOm', {
                            ph: number(Number(row.ph), 1),
                            om: number(Number(row.organicMatterPct ?? 0), 1),
                          })
                        : undefined
                    }
                    meta={dateTime(row.sampledAt)}
                    kind="soil"
                  />
                ))
              : null}
            {tab === 'recommendations'
              ? (recommendations.data ?? []).map((row) => (
                  <EntityCard
                    key={row.id}
                    title={label(row.title ?? row.kind, row.id)}
                    subtitle={row.summary ? label(row.summary) : undefined}
                    meta={`${label(row.priority)} · ${dateTime(row.createdAt)}`}
                    kind="recommendation"
                  />
                ))
              : null}
          </div>
        )}
      </QueryPageState>
    </section>
  )
}
