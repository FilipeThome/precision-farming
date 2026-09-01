import { useState } from 'react'

import { useEsgQuery, useTraceabilityQuery } from '@/features/compliance/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDateTime, formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { EntityCard } from '@/shared/ui/EntityCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SectionTabs } from '@/shared/ui/SectionTabs'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

type Tab = 'traceability' | 'esg'

export function CompliancePage() {
  const farmId = useUiStore((s) => s.farmId)
  const [tab, setTab] = useState<Tab>('traceability')
  const { t } = useI18n()
  const traceability = useTraceabilityQuery(farmId)
  const esg = useEsgQuery(farmId)
  const active = tab === 'traceability' ? traceability : esg
  const err = queryError(active.error)

  return (
    <section>
      <PageHeader title={t('compliance.title')} description={t('compliance.description')} />
      <SectionTabs
        active={tab}
        onChange={setTab}
        tabs={[
          { id: 'traceability', labelKey: 'compliance.tab.traceability' },
          { id: 'esg', labelKey: 'compliance.tab.esg' },
        ]}
      />
      <QueryPageState
        isLoading={active.isLoading}
        isError={active.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!active.isLoading && (active.data?.length ?? 0) === 0}
        emptyTitle={
          tab === 'traceability'
            ? t('compliance.traceability.emptyTitle')
            : t('compliance.esg.emptyTitle')
        }
        emptyDescription={
          tab === 'traceability'
            ? t('compliance.traceability.emptyDescription')
            : t('compliance.esg.emptyDescription')
        }
        onRetry={() => void active.refetch()}
      >
        <div className="grid gap-3 md:grid-cols-2">
          {tab === 'traceability'
            ? (traceability.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.lotCode ?? row.id}
                  subtitle={row.crop}
                  meta={formatDateTime(row.harvestedAt)}
                >
                  {row.status ? <StatusBadge value={row.status} /> : null}
                </EntityCard>
              ))
            : (esg.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.name ?? row.id}
                  subtitle={
                    row.value != null
                      ? `${formatNumber(Number(row.value), 2)} ${row.unit ?? ''}`.trim()
                      : undefined
                  }
                  meta={
                    row.score != null
                      ? `Score ${formatNumber(Number(row.score), 1)} · ${row.period ?? ''}`
                      : row.period
                  }
                />
              ))}
        </div>
      </QueryPageState>
    </section>
  )
}
