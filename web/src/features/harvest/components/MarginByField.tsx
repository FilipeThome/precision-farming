import { Link } from 'react-router'

import type { FinancePnl } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { Card } from '@/shared/ui/Card'

type Props = { pnl: FinancePnl[]; farmId: string | null; isLoading: boolean; isError: boolean }

export function marginRows(pnl: FinancePnl[]): FinancePnl[] {
  return pnl.filter((row) => row.fieldId && row.margin != null).sort((a, b) => (b.margin ?? 0) - (a.margin ?? 0))
}

export function MarginByField({ pnl, farmId, isLoading, isError }: Props) {
  const { t } = useI18n()
  const { number, label } = useFormat()
  const rows = marginRows(pnl).slice(0, 8)
  const maxAbs = rows.reduce((m, r) => Math.max(m, Math.abs(r.margin ?? 0)), 0)
  const financeHref = `/finance?tab=pnl${farmId ? `&farm=${encodeURIComponent(farmId)}` : ''}`

  return (
    <Card className="flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <h2 className="font-display text-base font-bold">{t('postHarvest.margin.title')}</h2>
        <Link to={financeHref} className="text-xs font-semibold text-ag-t-700 hover:underline">
          {t('postHarvest.margin.openFinance')}
        </Link>
      </div>
      {isLoading ? (
        <div className="h-24 animate-pulse rounded-[10px] bg-ag-n-100" aria-busy="true" />
      ) : isError ? (
        <p className="text-sm text-ag-crit" role="alert">
          {t('common.loadError')}
        </p>
      ) : rows.length === 0 ? (
        <p className="text-sm text-ag-n-600" role="status">
          {t('postHarvest.margin.empty')}
        </p>
      ) : (
        <ul className="flex flex-col gap-1.5">
          {rows.map((row) => {
            const margin = row.margin ?? 0
            const width = maxAbs > 0 ? Math.max(4, Math.round((Math.abs(margin) / maxAbs) * 100)) : 0
            return (
              <li key={row.id} className="grid grid-cols-[120px_1fr_auto] items-center gap-2 text-xs">
                <span className="truncate font-semibold">{label(row.fieldId)}</span>
                <span className="h-2 overflow-hidden rounded-full bg-ag-n-100" aria-hidden>
                  <span className={`block h-full rounded-full ${margin >= 0 ? 'bg-ag-t-500' : 'bg-ag-crit'}`} style={{ width: `${width}%` }} />
                </span>
                <span className={`font-mono tnum ${margin >= 0 ? 'text-ag-n-800' : 'text-ag-crit'}`}>
                  {row.currency ? `${row.currency} ` : ''}
                  {number(margin, 0)}
                </span>
              </li>
            )
          })}
        </ul>
      )}
    </Card>
  )
}
