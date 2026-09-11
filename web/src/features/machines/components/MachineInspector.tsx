import { useInventoryQuery } from '@/features/inventory/queries'
import { useMachineMetricsQuery } from '@/features/machines/queries'
import { useMachineWorkSummaryQuery } from '@/features/operations/queries'
import type { Machine } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { useFormat } from '@/shared/lib/useFormat'
import { CHART_COLORS, ChartCard } from '@/shared/ui/ChartCard'
import { BarChartBlock, LineChartBlock } from '@/shared/ui/charts'
import { EntityTile } from '@/shared/ui/EntityTile'
import { FreshnessChip, InspectorKpis } from '@/shared/ui/InspectorKpis'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = {
  machine: Machine
  farmId: string | null
}

function freshness(lastSeen: string | null): 'LIVE' | 'STALE' | null {
  if (!lastSeen) return null
  const age = Date.now() - Date.parse(lastSeen)
  if (!Number.isFinite(age)) return null
  return age < 2 * 3_600_000 ? 'LIVE' : 'STALE'
}

export function MachineInspector({ machine, farmId }: Props) {
  const metrics = useMachineMetricsQuery(machine.id)
  const work = useMachineWorkSummaryQuery(machine.id)
  const inventory = useInventoryQuery(farmId)
  const { t } = useI18n()
  const { number, label, dateTime } = useFormat()
  const metricsErr = metrics.error ? queryError(metrics.error) : null
  const workErr = work.error ? queryError(work.error) : null
  const names = new Map((inventory.data ?? []).map((item) => [item.id, item.name]))
  const inputs = (work.data?.inputs ?? []).map((row) => ({
    name: names.get(row.itemId) ?? label(row.itemId),
    value: Number(row.quantity),
  }))
  const days = metrics.data?.days ?? []
  const source = freshness(metrics.data?.lastObservedAt ?? null)
  const lastSeen = metrics.data?.lastObservedAt ?? null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-3">
        <EntityTile kind="machine" machineType={machine.type} size="lg" label={label(machine.id, machine.name)} />
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge value={machine.status} />
            {source ? <FreshnessChip source={source} /> : null}
          </div>
          <p className="mt-1 text-sm text-pf-muted">
            {label(machine.type)} · {machine.manufacturer} {machine.model}
          </p>
          <p className="mt-1 text-xs text-pf-muted">
            {lastSeen ? t('inspector.lastSeen', { when: dateTime(lastSeen) }) : t('inspector.lastSeenUnknown')}
          </p>
        </div>
      </div>

      {metrics.isLoading || work.isLoading ? <p className="text-sm text-pf-muted">{t('common.loading')}</p> : null}
      {metricsErr ? (
        <p className="text-sm text-red-800" role="alert">
          {metricsErr.message}
        </p>
      ) : null}
      {workErr ? (
        <p className="text-sm text-red-800" role="alert">
          {workErr.message}
        </p>
      ) : null}

      <InspectorKpis
        items={[
          {
            label: t('machines.kpi.engineHours'),
            value: metrics.data ? `${number(metrics.data.engineHours, 1)} h` : '—',
          },
          {
            label: t('machines.kpi.areaCovered'),
            value: work.data ? `${number(Number(work.data.areaHa), 1)} ha` : '—',
          },
          {
            label: t('machines.kpi.inputsUsed'),
            value: work.data ? number(inputs.reduce((sum, row) => sum + row.value, 0), 1) : '—',
          },
        ]}
      />

      <ChartCard title={t('charts.hoursPerDay')} description={t('charts.fromTelemetry')} className="min-h-[220px]">
        <LineChartBlock
          data={days.map((row) => ({ name: row.day.slice(5), hours: row.hours }))}
          xKey="name"
          lines={[{ dataKey: 'hours', name: t('charts.hours'), color: CHART_COLORS.green }]}
        />
      </ChartCard>
      <ChartCard title={t('charts.fuel')} description={t('charts.fromTelemetry')} className="min-h-[220px]">
        <LineChartBlock
          data={days.map((row) => ({ name: row.day.slice(5), fuel: row.fuel }))}
          xKey="name"
          lines={[{ dataKey: 'fuel', name: t('charts.fuel'), color: CHART_COLORS.amber }]}
        />
      </ChartCard>
      <ChartCard title={t('charts.speed')} description={t('charts.fromTelemetry')} className="min-h-[220px]">
        <LineChartBlock
          data={days.map((row) => ({ name: row.day.slice(5), speed: row.speed }))}
          xKey="name"
          lines={[{ dataKey: 'speed', name: t('charts.speed'), color: CHART_COLORS.blue }]}
        />
      </ChartCard>
      <ChartCard title={t('charts.areaPerDay')} description={t('charts.fromLive')} className="min-h-[220px]">
        <BarChartBlock
          data={(work.data?.days ?? []).map((row) => ({ name: row.day.slice(5), area: Number(row.areaHa) }))}
          xKey="name"
          bars={[{ dataKey: 'area', name: t('charts.areaHa'), color: CHART_COLORS.teal }]}
        />
      </ChartCard>
      <ChartCard title={t('charts.inputsByProduct')} description={t('charts.fromLive')} className="min-h-[220px]">
        <BarChartBlock
          data={inputs}
          xKey="name"
          bars={[{ dataKey: 'value', name: t('charts.inputsByProduct'), color: CHART_COLORS.green }]}
        />
      </ChartCard>
    </div>
  )
}
