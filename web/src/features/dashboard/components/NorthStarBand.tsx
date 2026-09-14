import type { Ratio } from '@/features/dashboard/model/northStar'
import { useI18n } from '@/shared/i18n/useI18n'
import { Card } from '@/shared/ui/Card'

type Props = {
  withinWindow: Ratio
  traceability: Ratio
  criticalAlerts: number
  fleet: Ratio
  isLoading: boolean
}

function pctText(r: Ratio): string {
  return r.pct == null ? '—' : `${r.pct}%`
}

function Kpi({ label, value, hint, dark = false }: { label: string; value: string; hint: string; dark?: boolean }) {
  return (
    <div className="flex flex-col gap-0.5 px-3.5 py-3">
      <span className={`text-xs font-medium ${dark ? 'text-[#a8c4b3]' : 'text-ag-n-600'}`}>{label}</span>
      <span
        className={`font-display font-extrabold leading-none tracking-[-0.03em] tnum ${
          dark ? 'text-[34px] text-white' : 'text-[26px] text-ag-g-900'
        }`}
      >
        {value}
      </span>
      <span className={`text-[11px] ${dark ? 'text-[#a8c4b3]' : 'text-ag-n-600'}`}>{hint}</span>
    </div>
  )
}

/** North Star band: single ratio with n/d text — no trend, no target, no sparkline (no backend for them). */
export function NorthStarBand({ withinWindow, traceability, criticalAlerts, fleet, isLoading }: Props) {
  const { t } = useI18n()

  if (isLoading) {
    return (
      <div className="grid gap-3.5 md:grid-cols-2 xl:grid-cols-[1.6fr_1fr_1fr_1fr]" aria-busy="true">
        <div className="h-[92px] animate-pulse rounded-[14px] bg-ag-g-100" />
        <div className="h-[92px] animate-pulse rounded-[14px] bg-ag-n-0" />
        <div className="h-[92px] animate-pulse rounded-[14px] bg-ag-n-0" />
        <div className="h-[92px] animate-pulse rounded-[14px] bg-ag-n-0" />
      </div>
    )
  }

  return (
    <div className="grid gap-3.5 md:grid-cols-2 xl:grid-cols-[1.6fr_1fr_1fr_1fr]">
      <Card className="border-transparent bg-[linear-gradient(135deg,var(--color-ag-g-800),var(--color-ag-g-950))] p-0 text-[#eaf2ec]">
        <Kpi
          dark
          label={t('tower.northStar.label')}
          value={pctText(withinWindow)}
          hint={
            withinWindow.denominator === 0
              ? t('tower.northStar.emptyHint')
              : t('tower.ratioHint', { n: withinWindow.numerator, m: withinWindow.denominator })
          }
        />
      </Card>
      <Card className="p-0">
        <Kpi
          label={t('tower.kpi.traceability')}
          value={pctText(traceability)}
          hint={
            traceability.denominator === 0
              ? t('tower.kpi.noOps')
              : t('tower.kpi.traceabilityHint', { n: traceability.numerator, m: traceability.denominator })
          }
        />
      </Card>
      <Card className="p-0">
        <Kpi label={t('tower.kpi.criticalAlerts')} value={String(criticalAlerts)} hint={t('tower.kpi.criticalAlertsHint')} />
      </Card>
      <Card className="p-0">
        <Kpi
          label={t('tower.kpi.fleet')}
          value={pctText(fleet)}
          hint={fleet.denominator === 0 ? t('tower.kpi.noMachines') : t('tower.kpi.fleetHint', { n: fleet.numerator, m: fleet.denominator })}
        />
      </Card>
    </div>
  )
}
