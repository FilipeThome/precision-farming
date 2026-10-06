import { usePrescriptionsQuery } from '@/features/agronomy/queries'
import { weightedYieldTHa, yieldsForField } from '@/features/harvest/yieldMath'
import { useYieldQuery } from '@/features/harvest/queries'
import { useOperationsQuery } from '@/features/operations/queries'
import type { Field } from '@/features/fields/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { EntityTile } from '@/shared/ui/EntityTile'
import { InspectorKpis } from '@/shared/ui/InspectorKpis'
import { InspectorQueryState } from '@/shared/ui/InspectorQueryState'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = { field: Field }

export function FieldInspector({ field }: Props) {
  const operations = useOperationsQuery(field.farmId)
  const prescriptions = usePrescriptionsQuery(field.farmId)
  const yields = useYieldQuery(field.farmId)
  const { t } = useI18n()
  const { number, label } = useFormat()

  const fieldOps = (operations.data ?? []).filter((op) => op.fieldId === field.id)
  const fieldRx = prescriptions.isSuccess
    ? (prescriptions.data ?? []).filter((rx) => rx.fieldId === field.id)
    : null
  const fieldYield = yields.isSuccess
    ? weightedYieldTHa(yieldsForField(yields.data ?? [], field.id))
    : null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-3">
        <EntityTile kind="field" size="lg" label={label(field.id, field.name)} />
        <div>
          <p className="text-sm text-pf-muted">
            {label(field.crop)}
            {field.variety ? ` · ${label(field.variety)}` : ''}
          </p>
        </div>
      </div>
      <InspectorQueryState
        isLoading={operations.isLoading}
        error={operations.error}
        onRetry={() => void operations.refetch()}
      >
        <InspectorKpis
          items={[
            { label: t('fields.kpi.crop'), value: label(field.crop) },
            { label: t('fields.kpi.area'), value: `${number(Number(field.areaHa), 1)} ha` },
            { label: t('fields.kpi.ops'), value: number(fieldOps.length, 0) },
            {
              label: t('fields.kpi.yield'),
              value: fieldYield == null ? '—' : `${number(fieldYield, 1)} t/ha`,
              hint: yields.isError ? t('common.loadError') : undefined,
            },
            {
              label: t('fields.kpi.prescriptions'),
              value: fieldRx == null ? '—' : number(fieldRx.length, 0),
              hint: prescriptions.isError ? t('common.loadError') : undefined,
            },
          ]}
        />
        {fieldOps.slice(0, 5).map((op) => (
          <div key={op.id} className="flex items-center justify-between gap-2 text-sm">
            <span className="text-pf-green">{label(op.type)}</span>
            <StatusBadge value={op.status} />
          </div>
        ))}
      </InspectorQueryState>
    </div>
  )
}
