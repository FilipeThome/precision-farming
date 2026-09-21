export const INVENTORY_UNITS = ['G', 'KG', 'T', 'TON', 'TONNE', 'ML', 'L', 'M3', 'UN', 'UNIT', 'PC'] as const

export type InventoryUnit = (typeof INVENTORY_UNITS)[number]

export function inventoryUnitOptions(current?: string): Array<{ value: string; label: string }> {
  const allowed: readonly string[] = INVENTORY_UNITS
  const units = current && !allowed.includes(current) ? [...INVENTORY_UNITS, current] : [...INVENTORY_UNITS]
  return units.map((value) => ({ value, label: value }))
}
