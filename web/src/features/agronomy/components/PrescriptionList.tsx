import { useApprovePrescription } from '@/features/agronomy/queries'
import type { Prescription } from '@/features/agronomy/types'
import { useCanManageFarmOps } from '@/shared/auth/roles'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { Button } from '@/shared/ui/Button'
import { EntityCard } from '@/shared/ui/EntityCard'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = {
  items: Prescription[]
}

function modeLabel(
  mode: string | null | undefined,
  t: (key: 'decisions.prescription.mode.SPOT' | 'decisions.prescription.mode.BROADCAST') => string,
  label: (value?: string | null) => string,
): string {
  if (mode === 'SPOT') return t('decisions.prescription.mode.SPOT')
  if (mode === 'BROADCAST') return t('decisions.prescription.mode.BROADCAST')
  return label(mode)
}

export function PrescriptionList({ items }: Props) {
  const { t } = useI18n()
  const { number, dateTime, label } = useFormat()
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
            title={label(row.product)}
            subtitle={t('agronomy.prescriptions.rate', {
              rate: number(Number(row.plannedDose), 2),
              unit: row.unit,
            })}
            meta={[
              modeLabel(row.mode, t, label),
              row.moaGroup ? label(row.moaGroup) : null,
              dateTime(row.approvedAt ?? row.createdAt),
            ]
              .filter(Boolean)
              .join(' · ')}
            kind="inventory"
          >
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <StatusBadge value={row.status} />
              {row.status === 'DRAFT' && canApprove ? (
                <Button disabled={approve.isPending} onClick={() => approve.mutate(row.id)}>
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
