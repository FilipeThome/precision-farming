import { useState } from 'react'

import {
  useMarketContractsQuery,
  useMarketExposureQuery,
  useMarketQuotesQuery,
} from '@/features/market/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDateTime, formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
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
                  meta={`${row.market ?? '—'} · ${formatDateTime(row.quotedAt)}`}
                />
              ))
            : null}
          {tab === 'contracts'
            ? (contracts.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={row.commodity ?? row.id}
                  subtitle={row.counterparty}
                  meta={
                    row.volumeTons != null
                      ? `${formatNumber(Number(row.volumeTons), 1)} t · ${formatNumber(Number(row.price ?? 0), 2)} ${row.currency ?? ''}`
                      : undefined
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
                    row.netTons != null ? `${formatNumber(Number(row.netTons), 1)} t net` : undefined
                  }
                  meta={
                    row.markToMarket != null
                      ? `${formatNumber(Number(row.markToMarket), 2)} ${row.currency ?? ''} · ${formatDateTime(row.asOf)}`
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
