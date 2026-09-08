import { useAlertsQuery } from '@/features/alerts/queries'
import { useFieldsQuery } from '@/features/fields/queries'
import { useFinancePnlQuery } from '@/features/finance/queries'
import { useOperationsQuery } from '@/features/operations/queries'
import type { Farm } from '@/shared/api/types'
import { farmPhoto } from '@/shared/demo/media'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { EntityPhoto } from '@/shared/ui/EntityPhoto'
import { InspectorKpis } from '@/shared/ui/InspectorKpis'

type Props = { farm: Farm }

export function FarmInspector({ farm }: Props) {
  const fields = useFieldsQuery(farm.id)
  const operations = useOperationsQuery(farm.id)
  const alerts = useAlertsQuery(farm.id)
  const pnl = useFinancePnlQuery(farm.id)
  const { t } = useI18n()
  const { number, label } = useFormat()
  const margin = (pnl.data ?? []).reduce((sum, row) => sum + Number(row.margin ?? 0), 0)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-3">
        <EntityPhoto src={farmPhoto(farm.id)} alt={label(farm.id, farm.name)} />
        <div>
          <p className="text-sm text-pf-muted">{farm.location}</p>
          <p className="text-xs text-pf-muted">{farm.timezone}</p>
        </div>
      </div>
      <InspectorKpis
        items={[
          { label: t('farms.kpi.area'), value: `${number(Number(farm.areaHa), 1)} ha` },
          { label: t('farms.kpi.fields'), value: number(fields.data?.length ?? 0, 0) },
          { label: t('farms.kpi.ops'), value: number(operations.data?.length ?? 0, 0) },
          { label: t('farms.kpi.alerts'), value: number(alerts.data?.length ?? 0, 0) },
          { label: t('farms.kpi.pnl'), value: number(margin, 0) },
        ]}
      />
    </div>
  )
}
