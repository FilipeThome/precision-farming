import type { LoadsSummary, Occupancy, TonsCoverage } from '@/features/harvest/model/postHarvestKpis'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { Card } from '@/shared/ui/Card'

type Props = {
  expected: TonsCoverage
  harvested: TonsCoverage
  loads: LoadsSummary
  occupancy: Occupancy
  isLoading: boolean
}

function Kpi({ label, value, hint, tone }: { label: string; value: string; hint?: string; tone?: 'teal' | 'warn' }) {
  return (
    <Card className="flex flex-col gap-1">
      <span className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">{label}</span>
      <b className={`font-display text-[26px] leading-none tnum ${tone === 'teal' ? 'text-ag-t-600' : tone === 'warn' ? 'text-ag-warn' : 'text-ag-n-900'}`}>
        {value}
      </b>
      {hint ? <span className="text-xs text-ag-n-600">{hint}</span> : null}
    </Card>
  )
}

export function PostHarvestKpis({ expected, harvested, loads, occupancy, isLoading }: Props) {
  const { t } = useI18n()
  const { number } = useFormat()

  if (isLoading) {
    return (
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4" aria-busy="true" aria-live="polite">
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className="h-[92px] animate-pulse rounded-[14px] bg-ag-n-0" />
        ))}
      </div>
    )
  }

  return (
    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
      {expected.withArea > 0 ? (
        <Kpi
          label={t('postHarvest.kpi.expected')}
          value={`${number(expected.tons, 0)} t`}
          hint={t('postHarvest.kpi.plansWithArea', { n: expected.withArea, m: expected.total })}
        />
      ) : (
        <Kpi label={t('postHarvest.kpi.expected')} value="—" hint={t('postHarvest.kpi.noArea')} />
      )}
      <Kpi
        label={t('postHarvest.kpi.harvested')}
        value={harvested.withArea > 0 ? `${number(harvested.tons, 0)} t` : '—'}
        hint={
          harvested.withArea > 0
            ? t('postHarvest.kpi.recordsWithArea', { n: harvested.withArea, m: harvested.total })
            : t('postHarvest.kpi.noYield')
        }
        tone="teal"
      />
      <Kpi
        label={t('postHarvest.kpi.inTransit')}
        value={String(loads.inTransit)}
        hint={t('postHarvest.kpi.loadsHint', { queued: loads.queued, delivered: loads.delivered, tons: number(loads.tonsInTransit, 0) })}
      />
      <Kpi
        label={t('postHarvest.kpi.occupancy')}
        value={occupancy.pct == null ? '—' : `${occupancy.pct}%`}
        hint={
          occupancy.capacityT > 0
            ? t('harvest.storage.occupancy', { used: number(occupancy.usedT, 0), capacity: number(occupancy.capacityT, 0) })
            : t('postHarvest.kpi.noStorage')
        }
        tone={occupancy.pct != null && occupancy.pct >= 90 ? 'warn' : undefined}
      />
    </div>
  )
}
