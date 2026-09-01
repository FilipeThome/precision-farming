import { useMemo, useState } from 'react'
import { Link } from 'react-router'

import { useEsgQuery, useTraceabilityQuery } from '@/features/compliance/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDateTime, formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { EntityCard } from '@/shared/ui/EntityCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SectionTabs } from '@/shared/ui/SectionTabs'
import { useUiStore } from '@/shared/ui/uiStore'

type Tab = 'traceability' | 'esg'

export function CompliancePage() {
  const farmId = useUiStore((s) => s.farmId)
  const [tab, setTab] = useState<Tab>('traceability')
  const { t } = useI18n()
  const traceability = useTraceabilityQuery(farmId, { enabled: tab === 'traceability' })
  const esg = useEsgQuery(farmId, { enabled: tab === 'esg' })
  const active = tab === 'traceability' ? traceability : esg
  const err = queryError(active.error)

  const lots = useMemo(() => {
    const map = new Map<string, { lotCode: string; crop: string; latestAt: string; count: number }>()
    for (const row of traceability.data ?? []) {
      const existing = map.get(row.lotCode)
      if (!existing) {
        map.set(row.lotCode, {
          lotCode: row.lotCode,
          crop: row.crop,
          latestAt: row.occurredAt,
          count: 1,
        })
        continue
      }
      existing.count += 1
      if (new Date(row.occurredAt).getTime() > new Date(existing.latestAt).getTime()) {
        existing.latestAt = row.occurredAt
        existing.crop = row.crop
      }
    }
    return [...map.values()].sort(
      (a, b) => new Date(b.latestAt).getTime() - new Date(a.latestAt).getTime(),
    )
  }, [traceability.data])

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
        isEmpty={
          !active.isLoading &&
          (tab === 'traceability' ? lots.length === 0 : (esg.data?.length ?? 0) === 0)
        }
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
            ? lots.map((lot) => (
                <EntityCard
                  key={lot.lotCode}
                  title={lot.lotCode}
                  subtitle={lot.crop}
                  meta={formatDateTime(lot.latestAt)}
                >
                  <Link
                    to={`/compliance/lots/${encodeURIComponent(lot.lotCode)}`}
                    className="mt-2 inline-flex items-center justify-center gap-2 rounded-[12px] border border-pf-border bg-white px-3 py-2 text-sm font-medium text-pf-green transition duration-150 hover:border-pf-teal"
                  >
                    {t('compliance.lot.open')} ({lot.count})
                  </Link>
                </EntityCard>
              ))
            : (esg.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.metric ?? row.id}
                  subtitle={
                    row.value != null
                      ? `${formatNumber(Number(row.value), 2)} ${row.unit ?? ''}`.trim()
                      : undefined
                  }
                  meta={
                    row.score != null
                      ? t('compliance.esg.score', {
                          score: formatNumber(Number(row.score), 1),
                          period: row.periodLabel ?? '',
                        })
                      : row.periodLabel
                  }
                />
              ))}
        </div>
      </QueryPageState>
    </section>
  )
}
