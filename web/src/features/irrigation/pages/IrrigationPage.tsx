import { useState } from 'react'

import {
  useIrrigationAssetsQuery,
  useIrrigationRecommendationsQuery,
} from '@/features/irrigation/queries'
import { irrigationPhoto } from '@/shared/demo/media'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { EntityCard } from '@/shared/ui/EntityCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SectionTabs } from '@/shared/ui/SectionTabs'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

type Tab = 'assets' | 'recommendations'

export function IrrigationPage() {
  const farmId = useUiStore((s) => s.farmId)
  const [tab, setTab] = useState<Tab>('assets')
  const { t } = useI18n()
  const { number, dateTime, label } = useFormat()
  const assets = useIrrigationAssetsQuery(farmId, { enabled: tab === 'assets' })
  const recommendations = useIrrigationRecommendationsQuery(farmId, {
    enabled: tab === 'recommendations',
  })
  const active = tab === 'assets' ? assets : recommendations
  const err = queryError(active.error)

  return (
    <section>
      <PageHeader title={t('irrigation.title')} description={t('irrigation.description')} />
      <SectionTabs
        active={tab}
        onChange={setTab}
        tabs={[
          { id: 'assets', labelKey: 'irrigation.tab.assets' },
          { id: 'recommendations', labelKey: 'irrigation.tab.recommendations' },
        ]}
      />
      <QueryPageState
        isLoading={active.isLoading}
        isError={active.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!active.isLoading && (active.data?.length ?? 0) === 0}
        emptyTitle={
          tab === 'assets'
            ? t('irrigation.assets.emptyTitle')
            : t('irrigation.recommendations.emptyTitle')
        }
        emptyDescription={
          tab === 'assets'
            ? t('irrigation.assets.emptyDescription')
            : t('irrigation.recommendations.emptyDescription')
        }
        onRetry={() => void active.refetch()}
      >
        <div className="grid gap-3 md:grid-cols-2">
          {tab === 'assets'
            ? (assets.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={label(row.name, row.id)}
                  subtitle={label(row.type)}
                  imageSrc={irrigationPhoto()}
                  imageAlt={label(row.name, row.type)}
                >
                  {row.status ? <StatusBadge value={row.status} /> : null}
                </EntityCard>
              ))
            : (recommendations.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={label(row.reason, row.fieldId ?? row.id)}
                  subtitle={
                    row.volumeMm != null ? `${number(Number(row.volumeMm), 1)} mm` : undefined
                  }
                  meta={`${label(row.priority)} · ${dateTime(row.recommendedAt)}`}
                  imageSrc={irrigationPhoto()}
                  imageAlt={label(row.reason)}
                />
              ))}
        </div>
      </QueryPageState>
    </section>
  )
}
