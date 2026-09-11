import { useState } from 'react'
import { Download } from 'lucide-react'

import { type ReportCatalogItem, GATEWAY_REPORTS, downloadReport } from '@/features/reports/api'
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

  const catalogFailed = reports.isError
  const items = reports.data && reports.data.length > 0 ? reports.data : catalogFailed ? GATEWAY_REPORTS : (reports.data ?? [])

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

  return (
    <section>
      <PageHeader title={t('reports.title')} description={t('reports.description')} />
      {error ? (
        <p className="mb-3 text-sm text-red-800" role="alert">
          {error}
        </p>
      ) : null}
      {catalogFailed ? (
        <p className="mb-3 text-sm text-amber-800" role="status">
          {t('reports.catalogFallback')}
        </p>
      ) : null}
      <QueryPageState
        isLoading={reports.isLoading}
        isError={false}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!reports.isLoading && items.length === 0}
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
                </div>
                <Button className="shrink-0" disabled={pending !== null} onClick={() => void download(item)}>
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
