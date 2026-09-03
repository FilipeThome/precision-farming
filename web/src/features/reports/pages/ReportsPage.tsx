import { useState } from 'react'
import { Download } from 'lucide-react'

import { type ReportCatalogItem, downloadReport } from '@/features/reports/api'
import { useReportsCatalogQuery } from '@/features/reports/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

export function ReportsPage() {
  const [pending, setPending] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const { t } = useI18n()
  const farmId = useUiStore((s) => s.farmId)
  const reports = useReportsCatalogQuery(farmId)
  const err = queryError(reports.error)
  const showDemoLinks = import.meta.env.DEV

  const fallbackReports: ReportCatalogItem[] = [
    { kind: 'operations', title: 'Operacoes', format: 'PDF', filename: 'operations.pdf', path: '/api/v1/reports/operations.pdf' },
    { kind: 'inventory', title: 'Estoque', format: 'PDF', filename: 'inventory.pdf', path: '/api/v1/reports/inventory.pdf' },
  ]
  const hasCatalogData = (reports.data?.length ?? 0) > 0
  const items = hasCatalogData ? reports.data! : fallbackReports

  async function download(item: ReportCatalogItem) {
    setError(null)
    setPending(item.kind)
    try {
      await downloadReport(item.path, item.filename, farmId)
    } catch (downloadError) {
      setError(queryError(downloadError).message)
    } finally {
      setPending(null)
    }
  }

  const samplePdfByKind: Record<ReportCatalogItem['kind'], string> = {
    operations: '/demo/reports/operations-demo.pdf',
    inventory: '/demo/reports/inventory-demo.pdf',
  }

  return (
    <section>
      <PageHeader title={t('reports.title')} description={t('reports.description')} />
      {error ? (
        <p className="mb-3 text-sm text-red-800" role="alert">
          {error}
        </p>
      ) : null}
      {reports.isError && !hasCatalogData ? (
        <p className="mb-3 text-sm text-amber-800" role="status">
          {err.message}
        </p>
      ) : null}
      <QueryPageState
        isLoading={reports.isLoading}
        isError={false}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={items.length === 0}
        emptyTitle={t('reports.emptyTitle')}
        emptyDescription={t('reports.emptyDescription')}
        onRetry={() => void reports.refetch()}
      >
        <div className="grid gap-4 md:grid-cols-2">
          {items.map((item) => {
            const title = item.kind === 'operations' ? t('reports.operations') : t('reports.inventory')
            return (
              <Card key={item.kind} className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div>
                  <h2 className="font-semibold text-pf-green">{title}</h2>
                  <p className="text-sm text-pf-muted">{item.path}</p>
                  {showDemoLinks ? (
                    <a
                      className="mt-1 inline-block text-sm text-pf-teal underline"
                      href={samplePdfByKind[item.kind]}
                      target="_blank"
                      rel="noreferrer"
                    >
                      PDF demo
                    </a>
                  ) : null}
                </div>
                <Button
                  variant={item.kind === 'inventory' ? 'secondary' : 'primary'}
                  disabled={pending !== null}
                  onClick={() => void download(item)}
                >
                  <Download className="h-4 w-4" aria-hidden />
                  {pending === item.kind ? t('reports.downloading') : t('reports.download')}
                </Button>
              </Card>
            )
          })}
        </div>
      </QueryPageState>
    </section>
  )
}
