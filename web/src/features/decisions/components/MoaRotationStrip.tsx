import { useMoaRotationQuery } from '@/features/agronomy/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { Card } from '@/shared/ui/Card'
import { InspectorQueryState } from '@/shared/ui/InspectorQueryState'
import { SimulationNotice } from '@/shared/ui/SimulationNotice'

type Props = { fieldId?: string | null }

export function MoaRotationStrip({ fieldId }: Props) {
  const { t } = useI18n()
  const { label } = useFormat()
  const query = useMoaRotationQuery(fieldId)

  if (!fieldId) return null

  return (
    <Card className="flex flex-col gap-2">
      <h3 className="font-display text-base font-bold">{t('agronomy.tab.moa')}</h3>
      <InspectorQueryState
        isLoading={query.isLoading}
        error={query.error}
        onRetry={() => void query.refetch()}
      >
        {query.data ? (
          <>
            <SimulationNotice simulation={query.data.simulation === true} />
            <p className="text-[13px]">
              <span className="text-ag-n-600">{t('decisions.prescription.moaGroup')}: </span>
              <b>{label(query.data.moaGroup)}</b>
            </p>
            {query.data.warning ? (
              <p className="rounded-[8px] bg-ag-warn-bg px-2.5 py-1.5 text-[12px] font-medium text-ag-warn" role="status">
                {t('agronomy.moa.warning')}
              </p>
            ) : null}
          </>
        ) : null}
      </InspectorQueryState>
    </Card>
  )
}
