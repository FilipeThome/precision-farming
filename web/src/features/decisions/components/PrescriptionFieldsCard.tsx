import { useApprovePrescription, usePrescriptionQuery } from '@/features/agronomy/queries'
import type { Prescription } from '@/shared/api/types'
import { useCanManageFarmOps } from '@/shared/auth/roles'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { InspectorQueryState } from '@/shared/ui/InspectorQueryState'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = {
  /** Prescription id from the decision queue (`rawId`). */
  prescriptionId?: string | null
  /** Fixture / preloaded DTO — skips the network fetch when set. */
  prescription?: Prescription
  showApprove?: boolean
}

function modeKey(mode: string | null | undefined): 'decisions.prescription.mode.SPOT' | 'decisions.prescription.mode.BROADCAST' | null {
  if (mode === 'SPOT') return 'decisions.prescription.mode.SPOT'
  if (mode === 'BROADCAST') return 'decisions.prescription.mode.BROADCAST'
  return null
}

function FieldRow({ label, value }: { label: string; value: string }) {
  return (
    <li className="flex items-start justify-between gap-2 border-b border-ag-n-100 py-1.5 last:border-b-0">
      <span className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">{label}</span>
      <span className="text-right text-[13px] font-medium text-ag-n-900">{value}</span>
    </li>
  )
}

export function PrescriptionFieldsCard({
  prescriptionId,
  prescription: prescriptionProp,
  showApprove = true,
}: Props) {
  const { t } = useI18n()
  const { number, label } = useFormat()
  const canManage = useCanManageFarmOps()
  const query = usePrescriptionQuery(prescriptionProp ? null : prescriptionId)
  const prescription = prescriptionProp ?? query.data
  const approve = useApprovePrescription()
  const err = approve.error ? queryError(approve.error) : null

  const modeLabelKey = modeKey(prescription?.mode ?? undefined)
  const canApprove =
    showApprove && prescription?.status === 'DRAFT' && canManage

  return (
    <Card className="flex flex-col gap-2">
      <div className="flex items-center justify-between gap-2">
        <h3 className="font-display text-base font-bold">{t('decisions.prescription.mode')}</h3>
        {prescription ? <StatusBadge value={prescription.status} /> : null}
      </div>
      <InspectorQueryState
        isLoading={!prescriptionProp && query.isLoading}
        error={!prescriptionProp ? query.error : null}
        onRetry={() => void query.refetch()}
      >
        {!prescription ? (
          <p className="text-sm text-ag-n-600" role="status">
            {t('decisions.data.empty')}
          </p>
        ) : (
          <>
            <ul className="flex flex-col text-xs">
              <FieldRow
                label={t('decisions.prescription.mode')}
                value={modeLabelKey ? t(modeLabelKey) : label(prescription.mode)}
              />
              <FieldRow
                label={t('decisions.prescription.treatedFraction')}
                value={
                  prescription.treatedFraction != null
                    ? number(Number(prescription.treatedFraction), 2)
                    : '—'
                }
              />
              <FieldRow
                label={t('decisions.prescription.dose')}
                value={`${number(Number(prescription.plannedDose), 2)} ${prescription.unit}`}
              />
              <FieldRow
                label={t('decisions.prescription.activeIngredient')}
                value={label(prescription.activeIngredient)}
              />
              <FieldRow
                label={t('decisions.prescription.moaGroup')}
                value={label(prescription.moaGroup)}
              />
              <FieldRow
                label={t('decisions.prescription.receituario')}
                value={label(prescription.receituarioNumber)}
              />
              <FieldRow
                label={t('decisions.prescription.rtCpf')}
                value={label(prescription.responsibleTechCpf)}
              />
              <FieldRow
                label={t('decisions.prescription.phi')}
                value={
                  prescription.phiDays != null ? String(prescription.phiDays) : '—'
                }
              />
              <FieldRow
                label={t('decisions.prescription.reentry')}
                value={
                  prescription.reentryHours != null
                    ? String(prescription.reentryHours)
                    : '—'
                }
              />
            </ul>
            {canApprove ? (
              <Button
                disabled={approve.isPending}
                onClick={() => approve.mutate(prescription.id)}
              >
                {t('agronomy.prescriptions.approve')}
              </Button>
            ) : null}
            {err ? (
              <p className="text-xs text-ag-crit" role="alert">
                {err.message}
              </p>
            ) : null}
          </>
        )}
      </InspectorQueryState>
    </Card>
  )
}
