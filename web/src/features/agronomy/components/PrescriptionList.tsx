import { useApprovePrescription } from '@/features/agronomy/queries'
import type { Prescription } from '@/shared/api/types'
import { useCanManageFarmOps } from '@/shared/auth/roles'
import { cropPhoto } from '@/shared/demo/media'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDateTime, formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { Button } from '@/shared/ui/Button'
import { EntityCard } from '@/shared/ui/EntityCard'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = {
  items: Prescription[]
}

export function PrescriptionList({ items }: Props) {
  const { t } = useI18n()
  const canApprove = useCanManageFarmOps()
  const approve = useApprovePrescription()
  const err = approve.error ? queryError(approve.error) : null

  return (
    <div className="flex flex-col gap-3">
      {err ? (
        <p className="text-sm text-red-800" role="alert">
          {err.message}
          {err.correlationId ? ` · ${t('common.correlationId')} ${err.correlationId}` : ''}
        </p>
      ) : null}
      <div className="grid gap-3 md:grid-cols-2">
        {items.map((row) => (
          <EntityCard
            key={row.id}
            title={row.product}
            subtitle={t('agronomy.prescriptions.rate', {
              rate: formatNumber(Number(row.rate), 2),
              unit: row.unit,
            })}
            meta={formatDateTime(row.approvedAt ?? row.createdAt)}
            imageSrc={cropPhoto()}
            imageAlt={row.product}
          >
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <StatusBadge value={row.status} />
              {row.status === 'DRAFT' && canApprove ? (
                <Button
                  disabled={approve.isPending}
                  onClick={() => approve.mutate(row.id)}
                >
                  {t('agronomy.prescriptions.approve')}
                </Button>
              ) : null}
            </div>
          </EntityCard>
        ))}
      </div>
    </div>
  )
}
