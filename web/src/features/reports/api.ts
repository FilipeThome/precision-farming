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
