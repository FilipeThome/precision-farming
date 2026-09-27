import { usePlantingGateQuery } from '@/features/weather/queries'
import type { Season } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import type { MessageKey } from '@/shared/i18n/useI18n'
import { Card } from '@/shared/ui/Card'
import { InspectorQueryState } from '@/shared/ui/InspectorQueryState'
import { SimulationNotice } from '@/shared/ui/SimulationNotice'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = { season: Season }

function toGateDate(startDate?: string | null): string | null {
  if (!startDate) return null
  // Accept ISO timestamps; gate API wants YYYY-MM-DD.
  return startDate.slice(0, 10)
}

function reasonKey(reason: string | null | undefined): MessageKey | null {
  if (reason === 'ZARC_OUT_OF_WINDOW') return 'seasons.plantingGate.reason.ZARC_OUT_OF_WINDOW'
  if (reason === 'SANITARY_VOID') return 'seasons.plantingGate.reason.SANITARY_VOID'
  return null
}

/** Informational ZARC / sanitary void gate — never creates a planting operation. */
export function PlantingGateCard({ season }: Props) {
  const { t } = useI18n()
  const date = toGateDate(season.startDate)
  const farmId = season.farmId
  const crop = season.crop && season.crop.trim() !== '' ? season.crop : 'SOY'
  const query = usePlantingGateQuery(farmId, date, crop, {
    enabled: Boolean(farmId) && Boolean(date),
  })

  if (!date) {
    return (
      <Card className="flex flex-col gap-2">
        <h3 className="font-display text-base font-bold">{t('seasons.plantingGate.title')}</h3>
        <p className="text-sm text-ag-n-600" role="status">
          {t('seasons.plantingGate.empty')}
        </p>
      </Card>
    )
  }

  const reason = reasonKey(query.data?.reason)
  const blocked = query.data?.decision === 'BLOCKED'

  return (
    <Card className="flex flex-col gap-2">
      <h3 className="font-display text-base font-bold">{t('seasons.plantingGate.title')}</h3>
      <InspectorQueryState
        isLoading={query.isLoading}
        error={query.error}
        onRetry={() => void query.refetch()}
      >
        {query.data ? (
          <>
            <SimulationNotice simulation={query.data.simulation === true} farm={query.data.farmId} />
            <div className="flex flex-wrap items-center gap-2">
              <StatusBadge value={query.data.decision} />
              <span className="text-[13px] font-semibold">
                {blocked ? t('seasons.plantingGate.blocked') : t('seasons.plantingGate.allowed')}
              </span>
            </div>
            {reason ? <p className="text-sm text-ag-n-700">{t(reason)}</p> : null}
            <p className="text-[11px] text-ag-n-500">{t('seasons.plantingGate.infoOnly')}</p>
          </>
        ) : null}
      </InspectorQueryState>
    </Card>
  )
}
