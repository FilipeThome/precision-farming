import { useState } from 'react'

import {
  useMarketContractsQuery,
  useMarketExposureQuery,
  useMarketQuotesQuery,
} from '@/features/market/queries'
import { exposureBars } from '@/shared/charts/adapters'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDateTime, formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { CHART_COLORS, ChartCard } from '@/shared/ui/ChartCard'
import { BarChartBlock } from '@/shared/ui/charts'
import { EntityCard } from '@/shared/ui/EntityCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SectionTabs } from '@/shared/ui/SectionTabs'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

type Tab = 'quotes' | 'contracts' | 'exposure'

export function MarketPage() {
  const farmId = useUiStore((s) => s.farmId)
  const [tab, setTab] = useState<Tab>('quotes')
  const { t } = useI18n()
  const quotes = useMarketQuotesQuery(farmId, { enabled: tab === 'quotes' })
  const contracts = useMarketContractsQuery(farmId, { enabled: tab === 'contracts' })
  const exposure = useMarketExposureQuery(farmId, { enabled: tab === 'exposure' })
  const active = tab === 'quotes' ? quotes : tab === 'contracts' ? contracts : exposure
  const err = queryError(active.error)

  return (
    <section>
      <PageHeader title={t('market.title')} description={t('market.description')} />
      <SectionTabs
        active={tab}
        onChange={setTab}
        tabs={[
          { id: 'quotes', labelKey: 'market.tab.quotes' },
          { id: 'contracts', labelKey: 'market.tab.contracts' },
          { id: 'exposure', labelKey: 'market.tab.exposure' },
        ]}
      />
      <QueryPageState
        isLoading={active.isLoading}
        isError={active.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!active.isLoading && (active.data?.length ?? 0) === 0}
        emptyTitle={
          tab === 'quotes'
            ? t('market.quotes.emptyTitle')
            : tab === 'contracts'
              ? t('market.contracts.emptyTitle')
              : t('market.exposure.emptyTitle')
        }
        emptyDescription={
          tab === 'quotes'
            ? t('market.quotes.emptyDescription')
            : tab === 'contracts'
              ? t('market.contracts.emptyDescription')
              : t('market.exposure.emptyDescription')
        }
        onRetry={() => void active.refetch()}
      >
        {tab === 'exposure' && (exposure.data?.length ?? 0) > 0 ? (
          <ChartCard title={t('charts.marketExposure')} className="mb-4">
            <BarChartBlock
              data={exposureBars(exposure.data ?? [])}
              xKey="name"
              bars={[
                { dataKey: 'open', name: t('charts.openTons'), color: CHART_COLORS.amber },
                { dataKey: 'hedged', name: t('charts.hedgedTons'), color: CHART_COLORS.green },
              ]}
            />
          </ChartCard>
        ) : null}
        <div className="grid gap-3 md:grid-cols-2">
          {tab === 'quotes'
            ? (quotes.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.commodity ?? row.id}
                  subtitle={
                    row.price != null
                      ? `${formatNumber(Number(row.price), 2)} ${row.currency ?? ''} / ${row.unit ?? ''}`.trim()
                      : undefined
                  }
                  meta={`${row.market ?? row.exchange ?? '—'} · ${formatDateTime(row.quotedAt)}`}
                />
              ))
            : null}
          {tab === 'contracts'
            ? (contracts.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.commodity ?? row.id}
                  subtitle={
                    (row.volumeTons ?? row.volumeT) != null
                      ? `${formatNumber(Number(row.volumeTons ?? row.volumeT), 1)} t`
                      : undefined
                  }
                  meta={
                    row.price != null
                      ? `${formatNumber(Number(row.price), 2)} ${row.currency ?? ''} · ${formatDateTime(row.deliveryAt)}`
                      : formatDateTime(row.deliveryAt)
                  }
                >
                  {row.status ? <StatusBadge value={row.status} /> : null}
                </EntityCard>
              ))
            : null}
          {tab === 'exposure'
            ? (exposure.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.commodity ?? row.id}
                  subtitle={
                    row.openT != null
                      ? `${formatNumber(Number(row.openT), 1)} t open · ${formatNumber(Number(row.hedgedT ?? 0), 1)} t hedged`
                      : row.netTons != null
                        ? `${formatNumber(Number(row.netTons), 1)} t net`
                        : undefined
                  }
                  meta={
                    row.riskScore != null
                      ? `${t('charts.risk')}: ${formatNumber(Number(row.riskScore), 1)}`
                      : formatDateTime(row.asOf)
                  }
                />
              ))
            : null}
        </div>
      </QueryPageState>
    </section>
  )
}
