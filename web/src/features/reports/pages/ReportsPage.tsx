import { useState } from 'react'
import { Download } from 'lucide-react'

import { downloadInventoryReport, downloadOperationsReport } from '@/features/reports/api'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'

export function ReportsPage() {
  const [pending, setPending] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const { t } = useI18n()

  async function download(kind: 'operations' | 'inventory') {
    setError(null)
    setPending(kind)
    try {
      if (kind === 'operations') await downloadOperationsReport()
      else await downloadInventoryReport()
    } catch (err) {
      setError(queryError(err).message)
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
      <div className="grid gap-4 md:grid-cols-2">
        <Card className="flex items-center justify-between gap-3">
          <div>
            <h2 className="font-semibold text-pf-green">Operações</h2>
            <p className="text-sm text-pf-muted">/api/v1/reports/operations.csv</p>
          </div>
          <Button disabled={pending !== null} onClick={() => void download('operations')}>
            <Download className="h-4 w-4" aria-hidden />
            {pending === 'operations' ? 'Baixando…' : 'Baixar'}
          </Button>
        </Card>
        <Card className="flex items-center justify-between gap-3">
          <div>
            <h2 className="font-semibold text-pf-green">Estoque</h2>
            <p className="text-sm text-pf-muted">/api/v1/reports/inventory.csv</p>
          </div>
          <Button variant="secondary" disabled={pending !== null} onClick={() => void download('inventory')}>
            <Download className="h-4 w-4" aria-hidden />
            {pending === 'inventory' ? 'Baixando…' : 'Baixar'}
          </Button>
        </Card>
      </div>
    </section>
  )
}
