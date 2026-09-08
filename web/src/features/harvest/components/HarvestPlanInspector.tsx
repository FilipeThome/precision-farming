import { weightedYieldTHa, yieldsForPlan } from '@/features/harvest/yieldMath'
import { useYieldQuery } from '@/features/harvest/queries'
import type { HarvestPlan } from '@/shared/api/types'
import { cropPhoto } from '@/shared/demo/media'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { EntityPhoto } from '@/shared/ui/EntityPhoto'
import { InspectorKpis } from '@/shared/ui/InspectorKpis'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = { plan: HarvestPlan; farmId: string | null }

export function HarvestPlanInspector({ plan, farmId }: Props) {
  const yields = useYieldQuery(farmId)
  const { t } = useI18n()
  const { number, label, dateTime } = useFormat()
  const actual = weightedYieldTHa(yieldsForPlan(yields.data ?? [], plan))
  const expected = Number(plan.expectedTHa ?? 0)
  const ratio = expected === 0 ? 0 : Math.round((actual / expected) * 100)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-3">
        <EntityPhoto src={cropPhoto(plan.crop)} alt={label(plan.crop)} />
        {plan.status ? <StatusBadge value={plan.status} /> : null}
      </div>
      <p className="text-xs text-pf-muted">{dateTime(plan.plannedStart)}</p>
      <InspectorKpis
        items={[
          { label: t('harvest.kpi.expected'), value: `${number(expected, 1)} t/ha` },
          { label: t('harvest.kpi.actual'), value: `${number(actual, 1)} t/ha` },
          { label: t('harvest.kpi.yieldVsPlan'), value: `${ratio}%` },
        ]}
      />
    </div>
  )
}
