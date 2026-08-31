import { apiDownload } from '@/shared/api/client'

export async function downloadOperationsReport(): Promise<void> {
  return apiDownload('/api/v1/reports/operations.csv', 'operations.csv')
}

export async function downloadInventoryReport(): Promise<void> {
  return apiDownload('/api/v1/reports/inventory.csv', 'inventory.csv')
}
