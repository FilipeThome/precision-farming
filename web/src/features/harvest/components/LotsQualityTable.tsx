import { qualityBreakdown } from '@/features/harvest/model/postHarvestKpis'
import type { StorageLot } from '@/features/harvest/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { Card } from '@/shared/ui/Card'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = { lots: StorageLot[]; isLoading: boolean; isError: boolean }

export function LotsQualityTable({ lots, isLoading, isError }: Props) {
  const { t } = useI18n()
  const { number, label, date } = useFormat()
  const breakdown = qualityBreakdown(lots)
  const recent = [...lots].sort((a, b) => (b.receivedAt ?? '').localeCompare(a.receivedAt ?? '')).slice(0, 6)

  return (
    <Card className="flex flex-col gap-2">
      <h2 className="font-display text-base font-bold">{t('postHarvest.lots.title')}</h2>
      {isLoading ? (
        <div className="h-24 animate-pulse rounded-[10px] bg-ag-n-100" aria-busy="true" />
      ) : isError ? (
        <p className="text-sm text-ag-crit" role="alert">
          {t('common.loadError')}
        </p>
      ) : lots.length === 0 ? (
        <p className="text-sm text-ag-n-600" role="status">
          {t('harvest.lots.emptyTitle')}
        </p>
      ) : (
        <>
          <ul className="flex flex-wrap gap-1.5">
            {breakdown.map((row) => (
              <li key={row.quality} className="flex items-center gap-1.5 rounded-full border border-ag-n-200 px-2 py-0.5 text-xs">
                <StatusBadge value={row.quality} />
                <span className="font-mono tnum">{number(row.tons, 0)} t</span>
                <span className="text-ag-n-500">· {t('postHarvest.lots.count', { n: row.count })}</span>
              </li>
            ))}
          </ul>
          <table className="w-full text-xs">
            <thead>
              <tr className="text-left text-[11px] uppercase tracking-[0.04em] text-ag-n-500">
                <th className="py-1 font-semibold">{t('postHarvest.lots.crop')}</th>
                <th className="py-1 font-semibold">{t('postHarvest.lots.unit')}</th>
                <th className="py-1 text-right font-semibold">{t('postHarvest.lots.tons')}</th>
                <th className="py-1 font-semibold">{t('postHarvest.lots.quality')}</th>
                <th className="py-1 font-semibold">{t('postHarvest.lots.received')}</th>
              </tr>
            </thead>
            <tbody>
              {recent.map((lot) => (
                <tr key={lot.id} className="border-t border-ag-n-100">
                  <td className="py-1.5 font-semibold">{label(lot.crop)}</td>
                  <td className="py-1.5 text-ag-n-700">{label(lot.unitId)}</td>
                  <td className="py-1.5 text-right font-mono tnum">{number(lot.tons, 1)}</td>
                  <td className="py-1.5">
                    <StatusBadge value={lot.quality} />
                  </td>
                  <td className="py-1.5 font-mono text-ag-n-600">{date(lot.receivedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </Card>
  )
}
