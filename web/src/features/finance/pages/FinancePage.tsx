import { useState } from 'react'

import {
  useFinanceBudgetQuery,
  useFinanceCashflowQuery,
  useFinanceCostsQuery,
  useFinancePnlQuery,
} from '@/features/finance/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDateTime, formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { EntityCard } from '@/shared/ui/EntityCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SectionTabs } from '@/shared/ui/SectionTabs'
import { useUiStore } from '@/shared/ui/uiStore'

type Tab = 'costs' | 'pnl' | 'budget' | 'cashflow'

function money(amount?: number, currency?: string) {
  if (amount == null) return '—'
  return `${formatNumber(Number(amount), 2)} ${currency ?? ''}`.trim()
}

export function FinancePage() {
  const farmId = useUiStore((s) => s.farmId)
  const [tab, setTab] = useState<Tab>('costs')
  const { t } = useI18n()
  const costs = useFinanceCostsQuery(farmId, { enabled: tab === 'costs' })
  const pnl = useFinancePnlQuery(farmId, { enabled: tab === 'pnl' })
  const budget = useFinanceBudgetQuery(farmId, { enabled: tab === 'budget' })
  const cashflow = useFinanceCashflowQuery(farmId, { enabled: tab === 'cashflow' })

  const active =
    tab === 'costs' ? costs : tab === 'pnl' ? pnl : tab === 'budget' ? budget : cashflow
  const err = queryError(active.error)

  const emptyTitle =
    tab === 'costs'
      ? t('finance.costs.emptyTitle')
      : tab === 'pnl'
        ? t('finance.pnl.emptyTitle')
        : tab === 'budget'
          ? t('finance.budget.emptyTitle')
          : t('finance.cashflow.emptyTitle')
  const emptyDescription =
    tab === 'costs'
      ? t('finance.costs.emptyDescription')
      : tab === 'pnl'
        ? t('finance.pnl.emptyDescription')
        : tab === 'budget'
          ? t('finance.budget.emptyDescription')
          : t('finance.cashflow.emptyDescription')

  return (
    <section>
      <PageHeader title={t('finance.title')} description={t('finance.description')} />
      <SectionTabs
        active={tab}
        onChange={setTab}
        tabs={[
          { id: 'costs', labelKey: 'finance.tab.costs' },
          { id: 'pnl', labelKey: 'finance.tab.pnl' },
          { id: 'budget', labelKey: 'finance.tab.budget' },
          { id: 'cashflow', labelKey: 'finance.tab.cashflow' },
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
          {tab === 'costs'
            ? (costs.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.category ?? row.id}
                  subtitle={money(row.amount, row.currency)}
                  meta={row.period}
                />
              ))
            : null}
          {tab === 'pnl'
            ? (pnl.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.fieldId ?? row.id}
                  subtitle={`Rev ${money(row.revenue, row.currency)} · Cost ${money(row.cost, row.currency)}`}
                  meta={`Margin ${money(row.margin, row.currency)} · ${row.period ?? ''}`}
                />
              ))
            : null}
          {tab === 'budget'
            ? (budget.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.category ?? row.id}
                  subtitle={`Plan ${money(row.planned, row.currency)} · Actual ${money(row.actual, row.currency)}`}
                  meta={row.period}
                />
              ))
            : null}
          {tab === 'cashflow'
            ? (cashflow.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.description ?? row.type ?? row.id}
                  subtitle={money(row.amount, row.currency)}
                  meta={formatDateTime(row.occurredAt)}
                />
              ))
            : null}
        </div>
      </QueryPageState>
    </section>
  )
}
