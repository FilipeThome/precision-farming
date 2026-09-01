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

export function groupCostsByCategory(costs: FinanceCost[]) {
  const map = new Map<string, number>()
  for (const row of costs) {
    if (!row.category || row.category === 'REVENUE') continue
    map.set(row.category, (map.get(row.category) ?? 0) + Number(row.amount ?? 0))
  }
  return [...map.entries()].map(([name, value]) => ({ name, value }))
}

export function pnlChartRows(rows: FinancePnl[]) {
  return rows.map((row) => ({
    name: shortId(row.farmId ?? row.id),
    revenue: Number(row.revenue ?? 0),
    cost: Number(row.cost ?? 0),
    margin: Number(row.margin ?? 0),
  }))
}

export function budgetChartRows(rows: FinanceBudget[]) {
  return rows.map((row) => ({
    name: row.category ?? shortId(row.id),
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

export function alertSeverityPie(alerts: Alert[]) {
  const map = new Map<string, number>()
  for (const a of alerts) {
    map.set(a.severity, (map.get(a.severity) ?? 0) + 1)
  }
  return [...map.entries()].map(([name, value]) => ({ name, value }))
}

export function opsStatusBars(ops: Operation[]) {
  const map = new Map<string, number>()
  for (const op of ops) {
    map.set(op.status, (map.get(op.status) ?? 0) + 1)
  }
  return [...map.entries()].map(([name, value]) => ({ name, value }))
}

export function fleetStatusBars(machines: Machine[]) {
  const map = new Map<string, number>()
  for (const m of machines) {
    map.set(m.status, (map.get(m.status) ?? 0) + 1)
  }
  return [...map.entries()].map(([name, value]) => ({ name, value }))
}

export function yieldByField(rows: YieldRecord[]) {
  return rows.map((row) => ({
    name: shortId(row.fieldId ?? row.id),
    yield: Number(row.yieldTHa ?? 0),
  }))
}

export function storageOccupancy(units: StorageUnit[]) {
  return units.map((u) => ({
    name: u.name ?? shortId(u.id),
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

export function inventoryStockBars(items: InventoryItem[]) {
  return items.map((item) => ({
    name: item.name,
    stock: Number(item.quantity),
    reserved: Number(item.reserved),
  }))
}

export function exposureBars(rows: MarketExposure[]) {
  return rows.map((row) => ({
    name: row.commodity ?? shortId(row.id),
    open: Number(row.openT ?? 0),
    hedged: Number(row.hedgedT ?? 0),
    risk: Number(row.riskScore ?? 0),
  }))
}

function shortId(id: string) {
  return id.length > 8 ? id.slice(0, 8) : id
}

function formatShortDate(iso?: string | null) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso.slice(0, 10)
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}
