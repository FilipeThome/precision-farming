import type {
  Alert,
  FinanceBudget,
  FinanceCashflow,
  FinanceCost,
  FinancePnl,
  InventoryItem,
  Machine,
  MarketExposure,
  Operation,
  StorageUnit,
  WeatherForecast,
  YieldRecord,
} from '@/shared/api/types'

type Label = (value?: string | null) => string

export function groupCostsByCategory(costs: FinanceCost[], label: Label = (v) => v ?? '') {
  const map = new Map<string, number>()
  for (const row of costs) {
    if (!row.category || row.category === 'REVENUE') continue
    const name = label(row.category)
    map.set(name, (map.get(name) ?? 0) + Number(row.amount ?? 0))
  }
  return [...map.entries()].map(([name, value]) => ({ name, value }))
}

export function pnlChartRows(rows: FinancePnl[], farmName: Label = shortId) {
  return rows.map((row) => ({
    name: farmName(row.farmId ?? row.id),
    revenue: Number(row.revenue ?? 0),
    cost: Number(row.cost ?? 0),
    margin: Number(row.margin ?? 0),
  }))
}

export function budgetChartRows(rows: FinanceBudget[], label: Label = (v) => v ?? '') {
  return rows.map((row) => ({
    name: label(row.category) || shortId(row.id),
    planned: Number(row.planned ?? 0),
    actual: Number(row.actual ?? 0),
  }))
}

export function cashflowChartRows(rows: FinanceCashflow[]) {
  return [...rows]
    .sort((a, b) => String(a.dueAt ?? '').localeCompare(String(b.dueAt ?? '')))
    .map((row) => {
      const amount = Number(row.amount ?? 0)
      const signed = row.direction === 'OUT' ? -amount : amount
      return {
        name: formatShortDate(row.dueAt),
        amount: signed,
        in: row.direction === 'IN' ? amount : 0,
        out: row.direction === 'OUT' ? amount : 0,
      }
    })
}

export function alertSeverityPie(alerts: Alert[], label: Label = (v) => v ?? '') {
  const map = new Map<string, number>()
  for (const a of alerts) {
    const name = label(a.severity)
    map.set(name, (map.get(name) ?? 0) + 1)
  }
  return [...map.entries()].map(([name, value]) => ({ name, value }))
}

export function opsStatusBars(ops: Operation[], label: Label = (v) => v ?? '') {
  const map = new Map<string, number>()
  for (const op of ops) {
    const name = label(op.status)
    map.set(name, (map.get(name) ?? 0) + 1)
  }
  return [...map.entries()].map(([name, value]) => ({ name, value }))
}

export function fleetStatusBars(machines: Machine[], label: Label = (v) => v ?? '') {
  const map = new Map<string, number>()
  for (const m of machines) {
    const name = label(m.status)
    map.set(name, (map.get(name) ?? 0) + 1)
  }
  return [...map.entries()].map(([name, value]) => ({ name, value }))
}

export function yieldByField(rows: YieldRecord[], fieldName: Label = shortId) {
  return rows.map((row) => ({
    name: fieldName(row.fieldId ?? row.id),
    yield: Number(row.yieldTHa ?? 0),
  }))
}

export function storageOccupancy(units: StorageUnit[], label: Label = (v) => v ?? '') {
  return units.map((u) => ({
    name: label(u.name) || shortId(u.id),
    used: Number(u.usedT ?? 0),
    capacity: Number(u.capacityT ?? 0),
  }))
}

export function weatherSeries(rows: WeatherForecast[]) {
  return [...rows]
    .sort((a, b) => a.forecastAt.localeCompare(b.forecastAt))
    .map((d) => ({
      name: formatShortDate(d.forecastAt),
      tMin: Number(d.temperatureMin),
      tMax: Number(d.temperatureMax),
      rain: Number(d.rainMm),
    }))
}

export function inventoryStockBars(items: InventoryItem[], label: Label = (v) => v ?? '') {
  return items.map((item) => ({
    name: label(item.name),
    stock: Number(item.quantity),
    reserved: Number(item.reserved),
  }))
}

export function exposureBars(rows: MarketExposure[], label: Label = (v) => v ?? '') {
  return rows.map((row) => ({
    name: label(row.commodity) || shortId(row.id),
    open: Number(row.openT ?? 0),
    hedged: Number(row.hedgedT ?? 0),
    risk: Number(row.riskScore ?? 0),
  }))
}

function shortId(id?: string | null) {
  if (!id) return '—'
  return id.length > 8 ? id.slice(0, 8) : id
}

function formatShortDate(iso?: string | null) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso.slice(0, 10)
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}
