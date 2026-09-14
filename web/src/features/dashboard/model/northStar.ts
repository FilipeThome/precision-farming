import type { Alert, Machine, Operation, WeatherWindow } from '@/shared/api/types'

export type Ratio = {
  numerator: number
  denominator: number
  /** 0–100 rounded, or null when the denominator is 0 (render "—"). */
  pct: number | null
}

export function ratio(numerator: number, denominator: number): Ratio {
  return {
    numerator,
    denominator,
    pct: denominator === 0 ? null : Math.round((numerator / denominator) * 100),
  }
}

function parse(value: string | null | undefined): number | null {
  if (!value) return null
  const at = Date.parse(value)
  return Number.isNaN(at) ? null : at
}

/** North Star: completed operations finished within their planned window / completed operations with both dates. */
export function withinWindowRatio(operations: Operation[]): Ratio {
  const completed = operations.filter((op) => op.status === 'COMPLETED')
  const measurable = completed.filter((op) => parse(op.actualEnd) != null && parse(op.plannedEnd) != null)
  const onTime = measurable.filter((op) => (parse(op.actualEnd) as number) <= (parse(op.plannedEnd) as number))
  return ratio(onTime.length, measurable.length)
}

/** Traceability proxy: operations with both a machine and an input item linked. */
export function traceabilityRatio(operations: Operation[]): Ratio {
  const linked = operations.filter((op) => Boolean(op.machineId) && Boolean(op.itemId))
  return ratio(linked.length, operations.length)
}

export function openCriticalAlerts(alerts: Alert[]): number {
  return alerts.filter((a) => a.status === 'OPEN' && a.severity === 'CRITICAL').length
}

/** Machines available for work (OPERATING or IDLE) / total. */
export function fleetAvailability(machines: Machine[]): Ratio {
  const available = machines.filter((m) => m.status === 'OPERATING' || m.status === 'IDLE')
  return ratio(available.length, machines.length)
}

/** End of a FAVORABLE window covering `now`, or null when none does. */
export function favorableWindowUntil(windows: WeatherWindow[], now: number = Date.now()): string | null {
  const covering = windows
    .filter((w) => w.rating === 'FAVORABLE')
    .filter((w) => {
      const start = parse(w.startAt)
      const end = parse(w.endAt)
      return start != null && end != null && start <= now && now < end
    })
    .sort((a, b) => b.endAt.localeCompare(a.endAt))
  return covering[0]?.endAt ?? null
}
