import { useState } from 'react'

import { PrescriptionList } from '@/features/agronomy/components/PrescriptionList'
import {
  usePrescriptionsQuery,
  useRecommendationsQuery,
  useScoutingQuery,
  useSoilSamplesQuery,
} from '@/features/agronomy/queries'
import { cropPhoto, scoutingPhoto, soilPhoto } from '@/shared/demo/media'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDateTime, formatNumber } from '@/shared/lib/format'
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
                    title={row.pest ?? row.id}
                    subtitle={row.notes}
                    meta={`${row.severity ?? '—'} · ${formatDateTime(row.observedAt)}`}
                    imageSrc={scoutingPhoto()}
                    imageAlt={row.pest ?? 'scouting'}
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
                        ? t('agronomy.soil.phOm', {
                            ph: formatNumber(Number(row.ph), 1),
                            om: formatNumber(Number(row.organicMatterPct ?? 0), 1),
                          })
                        : undefined
                    }
                    meta={formatDateTime(row.sampledAt)}
                    imageSrc={soilPhoto()}
                    imageAlt={row.lab ?? 'soil'}
                  />
                ))
              : null}
            {tab === 'recommendations'
              ? (recommendations.data ?? []).map((row) => (
                  <EntityCard
                    key={row.id}
                    title={row.title ?? row.kind ?? row.id}
                    subtitle={row.summary}
                    meta={`${row.priority ?? '—'} · ${formatDateTime(row.createdAt)}`}
                    imageSrc={cropPhoto()}
                    imageAlt={row.title ?? 'recommendation'}
                  />
                ))
              : null}
          </div>
        )}
      </QueryPageState>
    </section>
  )
}
