export function findInFarmScope<T extends { id: string; farmId?: string }>(
  rows: readonly T[] | null | undefined,
  id: string,
  farmId: string,
): T | undefined {
  if (!rows || !id || !farmId) return undefined
  return rows.find((row) => row.id === id && row.farmId === farmId)
}
