import { apiDownload, apiGet } from '@/shared/api/client'

export type ReportKind = 'operations' | 'inventory'

export type ReportCatalogItem = {
  kind: ReportKind
  title: string
  format: 'PDF'
  filename: string
  path: string
}

export function reportsPdfPath(kind: ReportKind, farmId?: string | null): string {
  const q = farmId ? `?farmId=${encodeURIComponent(farmId)}` : ''
  return `/api/v1/reports/${kind}.pdf${q}`
}

export async function fetchReportsCatalog(): Promise<ReportCatalogItem[]> {
  return apiGet<ReportCatalogItem[]>('/api/v1/reports')
}

/** Real gateway paths used when the catalog endpoint is down — not demo files. */
export const GATEWAY_REPORTS: ReportCatalogItem[] = [
  {
    kind: 'operations',
    title: 'operations',
    format: 'PDF',
    filename: 'operations.pdf',
    path: '/api/v1/reports/operations.pdf',
  },
  {
    kind: 'inventory',
    title: 'inventory',
    format: 'PDF',
    filename: 'inventory.pdf',
    path: '/api/v1/reports/inventory.pdf',
  },
]

export function reportDownloadPath(path: string, farmId?: string | null): string {
  if (!path.startsWith('/api/v1/reports/')) {
    throw new Error('Invalid report download path')
  }
  if (!farmId) return path
  const join = path.includes('?') ? '&' : '?'
  return `${path}${join}farmId=${encodeURIComponent(farmId)}`
}

export async function downloadReport(path: string, filename: string, farmId?: string | null): Promise<void> {
  return apiDownload(reportDownloadPath(path, farmId), filename)
}
