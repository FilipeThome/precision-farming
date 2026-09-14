import { useMemo } from 'react'
import { Clock, CloudSun } from 'lucide-react'
import { useNavigate } from 'react-router'

import { useAlertsQuery } from '@/features/alerts/queries'
import { ActionQueue } from '@/features/dashboard/components/ActionQueue'
import { FieldStatusMap } from '@/features/dashboard/components/FieldStatusMap'
import { FleetList } from '@/features/dashboard/components/FleetList'
import { NorthStarBand } from '@/features/dashboard/components/NorthStarBand'
import { TodayTimeline } from '@/features/dashboard/components/TodayTimeline'
import { fieldStates as deriveFieldStates } from '@/features/dashboard/model/fieldStatus'
import {
  favorableWindowUntil,
  fleetAvailability,
  openCriticalAlerts,
  traceabilityRatio,
  withinWindowRatio,
} from '@/features/dashboard/model/northStar'
import { timelineRows, todayRange, windowBands } from '@/features/dashboard/model/todayTimeline'
import { useDecisionSources } from '@/features/decisions/queries'
import { useFarmsQuery } from '@/features/farms/queries'
import { useFieldsQuery } from '@/features/fields/queries'
import { useMachinesQuery } from '@/features/machines/queries'
import { useOperationsQuery } from '@/features/operations/queries'
import { useWeatherWindowsQuery } from '@/features/weather/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { inspectHref } from '@/shared/lib/useFarmFromSearch'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { TAG_TONE } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

export function ControlTowerPage() {
  const farmId = useUiStore((s) => s.farmId)
  const navigate = useNavigate()
  const { t } = useI18n()
  const { dateTime } = useFormat()

  // All hooks unconditionally at page level; only `farms` gates the page.
  const farms = useFarmsQuery()
  const fields = useFieldsQuery(farmId)
  const machines = useMachinesQuery(farmId)
  const operations = useOperationsQuery(farmId)
  const alerts = useAlertsQuery(farmId)
  const windows = useWeatherWindowsQuery(farmId)
  const decisions = useDecisionSources(farmId)

  const farmsErr = queryError(farms.error)
  const ops = operations.data ?? []
  const now = Date.now()

  const withinWindow = useMemo(() => withinWindowRatio(ops), [ops])
  const traceability = useMemo(() => traceabilityRatio(ops), [ops])
  const critical = openCriticalAlerts(alerts.data ?? [])
  const fleet = fleetAvailability(machines.data ?? [])
  const favorableUntil = favorableWindowUntil(windows.data ?? [], now)

  const states = useMemo(() => deriveFieldStates(fields.data ?? [], ops), [fields.data, ops])
  const range = useMemo(() => todayRange(new Date(now)), [now])
  const rows = useMemo(() => timelineRows(ops, range, now), [ops, range, now])
  const bands = useMemo(() => windowBands(windows.data ?? [], range), [windows.data, range])

  const kpiLoading = operations.isLoading || alerts.isLoading || machines.isLoading

  return (
    <section className="flex min-h-full flex-col gap-3.5">
      <PageHeader
        title={t('tower.title')}
        meta={
          <>
            <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold ${TAG_TONE.neutral}`}>
              <Clock className="h-3 w-3" aria-hidden />
              {dateTime(new Date(now).toISOString())}
            </span>
            {favorableUntil ? (
              <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold ${TAG_TONE.teal}`}>
                <CloudSun className="h-3 w-3" aria-hidden />
                {t('tower.favorableUntil', { when: dateTime(favorableUntil) })}
              </span>
            ) : null}
          </>
        }
      />
      <QueryPageState
        isLoading={farms.isLoading}
        isError={farms.isError}
        errorMessage={farmsErr.message}
        correlationId={farmsErr.correlationId}
        isEmpty={!farms.isLoading && (farms.data?.length ?? 0) === 0}
        emptyTitle={t('tower.emptyTitle')}
        emptyDescription={t('tower.emptyDescription')}
        onRetry={() => void farms.refetch()}
      >
        <NorthStarBand
          withinWindow={withinWindow}
          traceability={traceability}
          criticalAlerts={critical}
          fleet={fleet}
          isLoading={kpiLoading}
        />
        <div className="grid gap-3.5 xl:grid-cols-[330px_1fr_300px]">
          <ActionQueue
            decisions={decisions.items}
            alerts={alerts.data ?? []}
            isLoading={decisions.isLoading || alerts.isLoading}
            isError={decisions.isError && alerts.isError}
            errorMessage={queryError(decisions.error ?? alerts.error).message}
            onRetry={() => {
              decisions.refetch()
              void alerts.refetch()
            }}
          />
          <div className="flex min-h-0 flex-col gap-3.5">
            <FieldStatusMap
              fields={fields.data ?? []}
              fieldStates={states}
              isLoading={fields.isLoading}
              isError={fields.isError}
              errorMessage={queryError(fields.error).message}
              correlationId={queryError(fields.error).correlationId}
              onRetry={() => void fields.refetch()}
              onFieldClick={(fieldId) => navigate(inspectHref('/fields', fieldId, farmId))}
            />
            <TodayTimeline
              rows={rows}
              bands={bands}
              isLoading={operations.isLoading}
              isError={operations.isError}
              errorMessage={queryError(operations.error).message}
              onRetry={() => void operations.refetch()}
            />
          </div>
          <FleetList
            machines={machines.data ?? []}
            farmId={farmId}
            isLoading={machines.isLoading}
            isError={machines.isError}
            errorMessage={queryError(machines.error).message}
            onRetry={() => void machines.refetch()}
          />
        </div>
      </QueryPageState>
    </section>
  )
}
