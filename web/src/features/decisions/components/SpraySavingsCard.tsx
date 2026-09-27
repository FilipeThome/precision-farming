import { useSpraySavingsQuery } from '@/features/agronomy/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { Card } from '@/shared/ui/Card'
import { InspectorQueryState } from '@/shared/ui/InspectorQueryState'
import { SimulationNotice } from '@/shared/ui/SimulationNotice'

type Props = { prescriptionId?: string | null }

export function SpraySavingsCard({ prescriptionId }: Props) {
  const { t } = useI18n()
  const { number } = useFormat()
  const query = useSpraySavingsQuery(prescriptionId)

  return (
    <Card className="flex flex-col gap-2">
      <h3 className="font-display text-base font-bold">{t('decisions.savings.title')}</h3>
      <InspectorQueryState
        isLoading={query.isLoading}
        error={query.error}
        onRetry={() => void query.refetch()}
      >
        {!query.data ? (
          <p className="text-sm text-ag-n-600" role="status">
            {t('decisions.savings.emptyTitle')}
          </p>
        ) : (
          <>
            <SimulationNotice simulation={query.data.simulation === true} />
            <ul className="flex flex-col gap-1 text-[13px]">
              <li className="flex justify-between gap-2 border-b border-ag-n-100 py-1">
                <span className="text-ag-n-600">{t('decisions.savings.fullRateHaAvoided')}</span>
                <b className="font-mono">{number(Number(query.data.fullRateHa), 2)} ha</b>
              </li>
              <li className="flex justify-between gap-2 border-b border-ag-n-100 py-1">
                <span className="text-ag-n-600">{t('decisions.savings.litersAvoided')}</span>
                <b className="font-mono">
                  {number(Number(query.data.litersAvoided), 1)} {query.data.unit}
                </b>
              </li>
              <li className="flex justify-between gap-2 py-1">
                <span className="text-ag-n-600">{t('decisions.savings.litersPerHa')}</span>
                <b className="font-mono">
                  {number(Number(query.data.litersPerHa), 2)} {query.data.unit}/ha
                </b>
              </li>
            </ul>
          </>
        )}
      </InspectorQueryState>
    </Card>
  )
}
