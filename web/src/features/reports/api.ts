import { apiDownload } from '@/shared/api/client'

export function reportsCsvPath(kind: 'operations' | 'inventory', farmId?: string | null): string {
  const q = farmId ? `?farmId=${encodeURIComponent(farmId)}` : ''
  return `/api/v1/reports/${kind}.csv${q}`
}

export async function downloadOperationsReport(farmId?: string | null): Promise<void> {
  return apiDownload(reportsCsvPath('operations', farmId), 'operations.csv')
}

export async function downloadInventoryReport(farmId?: string | null): Promise<void> {
  return apiDownload(reportsCsvPath('inventory', farmId), 'inventory.csv')
}
