import { useState } from 'react'

import {
  useFinanceBudgetQuery,
  useFinanceCashflowQuery,
  useFinanceCostsQuery,
  useFinancePnlQuery,
} from '@/features/finance/queries'
import {
  budgetChartRows,
  cashflowChartRows,
  groupCostsByCategory,
  pnlChartRows,
} from '@/shared/charts/adapters'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { CHART_COLORS, ChartCard } from '@/shared/ui/ChartCard'
import { BarChartBlock } from '@/shared/ui/charts'
import { EntityCard } from '@/shared/ui/EntityCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SectionTabs } from '@/shared/ui/SectionTabs'
import { useUiStore } from '@/shared/ui/uiStore'

type Tab = 'costs' | 'pnl' | 'budget' | 'cashflow'

export function FinancePage() {
  const farmId = useUiStore((s) => s.farmId)
  const [tab, setTab] = useState<Tab>('costs')
  const { t } = useI18n()
  const { number, dateTime, label } = useFormat()
  const costs = useFinanceCostsQuery(farmId, { enabled: tab === 'costs' })
  const pnl = useFinancePnlQuery(farmId, { enabled: tab === 'pnl' })
  const budget = useFinanceBudgetQuery(farmId, { enabled: tab === 'budget' })
  const cashflow = useFinanceCashflowQuery(farmId, { enabled: tab === 'cashflow' })

  const active =
    tab === 'costs' ? costs : tab === 'pnl' ? pnl : tab === 'budget' ? budget : cashflow
  const err = queryError(active.error)

  function money(amount?: number, currency?: string) {
    if (amount == null) return '—'
    return `${number(Number(amount), 2)} ${currency ?? 'BRL'}`.trim()
  }

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
        {tab === 'costs' ? (
          <ChartCard title={t('charts.costsByCategory')} className="mb-4">
            <BarChartBlock
              data={groupCostsByCategory(costs.data ?? [], label)}
              xKey="name"
              bars={[{ dataKey: 'value', name: t('charts.amount'), color: CHART_COLORS.amber }]}
            />
          </ChartCard>
        ) : null}
        {tab === 'pnl' ? (
          <ChartCard title={t('charts.pnlSummary')} description={t('charts.pnlHint')} className="mb-4">
            <BarChartBlock
              data={pnlChartRows(pnl.data ?? [], label)}
              xKey="name"
              bars={[
                { dataKey: 'revenue', name: t('charts.revenue'), color: CHART_COLORS.green },
                { dataKey: 'cost', name: t('charts.cost'), color: CHART_COLORS.amber },
                { dataKey: 'margin', name: t('charts.margin'), color: CHART_COLORS.teal },
              ]}
            />
          </ChartCard>
        ) : null}
        {tab === 'budget' ? (
          <ChartCard title={t('charts.budgetPlanVsActual')} className="mb-4">
            <BarChartBlock
              data={budgetChartRows(budget.data ?? [], label)}
              xKey="name"
              bars={[
                { dataKey: 'planned', name: t('charts.planned'), color: CHART_COLORS.blue },
                { dataKey: 'actual', name: t('charts.actual'), color: CHART_COLORS.green },
              ]}
            />
          </ChartCard>
        ) : null}
        {tab === 'cashflow' ? (
          <ChartCard title={t('charts.cashflowTimeline')} description={t('charts.cashflowHint')} className="mb-4">
            <BarChartBlock
              data={cashflowChartRows(cashflow.data ?? [])}
              xKey="name"
              bars={[
                { dataKey: 'in', name: t('charts.inflow'), color: CHART_COLORS.green },
                { dataKey: 'out', name: t('charts.outflow'), color: CHART_COLORS.red },
              ]}
            />
          </ChartCard>
        ) : null}

        <div className="grid gap-3 md:grid-cols-2">
          {tab === 'costs'
            ? (costs.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={label(row.category, row.id)}
                  subtitle={money(row.amount, row.currency)}
                  meta={`${label(row.description)} · ${dateTime(row.occurredAt)}`.replace(/^— · /, '')}
                />
              ))
            : null}
          {tab === 'pnl'
            ? (pnl.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={label(row.farmId, row.id)}
                  subtitle={t('finance.revCost', {
                    revenue: money(row.revenue, row.currency),
                    cost: money(row.cost, row.currency),
                  })}
                  meta={t('finance.marginPeriod', {
                    margin: money(row.margin, row.currency),
                    period: label(row.period ?? 'YTD'),
                  })}
                />
              ))
            : null}
          {tab === 'budget'
            ? (budget.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={label(row.category, row.id)}
                  subtitle={t('finance.planActual', {
                    planned: money(row.planned),
                    actual: money(row.actual),
                  })}
                  meta={label(row.seasonLabel)}
                />
              ))
            : null}
          {tab === 'cashflow'
            ? (cashflow.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={label(row.label, row.id)}
                  subtitle={`${label(row.direction)} ${money(row.amount)}`.trim()}
                  meta={dateTime(row.dueAt)}
                />
              ))
            : null}
        </div>
      </QueryPageState>
    </section>
  )
}
