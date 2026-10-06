import type { Operation, WeatherWindow } from '@/shared/api/types'

export type DayRange = { start: number; end: number }

export type Segment = { leftPct: number; widthPct: number }

export type TimelineBand = Segment & { rating: string }

export type TimelineRow = {
  operation: Operation
  planned?: Segment
  executed?: Segment
}

/** Browser-local calendar day containing `now` (00:00 → next local midnight). */
export function todayRange(now: Date = new Date()): DayRange {
  const year = now.getFullYear()
  const month = now.getMonth()
  const date = now.getDate()
  const start = new Date(year, month, date).getTime()
  return { start, end: new Date(year, month, date + 1).getTime() }
}

function parse(value: string | null | undefined): number | null {
  if (!value) return null
  const at = Date.parse(value)
  return Number.isNaN(at) ? null : at
}

function intersects(start: number | null, end: number | null, range: DayRange): boolean {
  if (start == null && end == null) return false
  const s = start ?? end!
  const e = end ?? start!
  return s < range.end && e > range.start
}

/** Operations whose planned window (or actual execution) intersects the day. */
export function todayOperations(operations: Operation[], range: DayRange): Operation[] {
  return operations
    .filter(
      (op) =>
        intersects(parse(op.plannedStart), parse(op.plannedEnd), range) ||
        intersects(parse(op.actualStart), parse(op.actualEnd), range),
    )
    .sort((a, b) => (a.plannedStart ?? a.actualStart ?? '').localeCompare(b.plannedStart ?? b.actualStart ?? ''))
}

/** Clamped percentage segment of [start,end] within the range; `end` defaults to `rangeEnd` when missing (ongoing). */
export function toSegment(start: number | null, end: number | null, range: DayRange): Segment | undefined {
  if (start == null && end == null) return undefined
  const total = range.end - range.start
  const s = Math.max(range.start, start ?? range.start)
  const e = Math.min(range.end, end ?? range.end)
  if (e <= s) return undefined
  return {
    leftPct: round(((s - range.start) / total) * 100),
    widthPct: round(((e - s) / total) * 100),
  }
}

function round(value: number): number {
  return Math.round(value * 100) / 100
}

export function timelineRows(operations: Operation[], range: DayRange, now: number = Date.now()): TimelineRow[] {
  return todayOperations(operations, range).map((operation) => ({
    operation,
    planned: toSegment(parse(operation.plannedStart), parse(operation.plannedEnd), range),
    executed: operation.actualStart
      ? toSegment(parse(operation.actualStart), parse(operation.actualEnd) ?? Math.min(now, range.end), range)
      : undefined,
  }))
}

/** Weather window bands intersecting the day, as percentage segments with their rating. */
export function windowBands(windows: WeatherWindow[], range: DayRange): TimelineBand[] {
  return windows
    .map((w) => ({ rating: w.rating, seg: toSegment(parse(w.startAt), parse(w.endAt), range) }))
    .filter((x): x is { rating: string; seg: Segment } => Boolean(x.seg))
    .map((x) => ({ rating: x.rating, ...x.seg }))
}

/** Hour tick labels every 4h. */
export const HOUR_TICKS = [0, 4, 8, 12, 16, 20]
