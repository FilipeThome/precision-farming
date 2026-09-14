import { useState } from 'react'
import { CircleCheck, Lock } from 'lucide-react'
import { Link } from 'react-router'

import { useApprovePrescription } from '@/features/agronomy/queries'
import type { DecisionItem } from '@/features/decisions/model'
import { useAuthStore } from '@/shared/auth/store'
import { useCanManageFarmOps } from '@/shared/auth/roles'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { useFormat } from '@/shared/lib/useFormat'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'

type Props = { item: DecisionItem }

export function ApprovalPanel({ item }: Props) {
  const { t } = useI18n()
  const { label, dateTime } = useFormat()
  const canManage = useCanManageFarmOps()
  const name = useAuthStore((s) => s.name)
  const role = useAuthStore((s) => s.role)
  const approve = useApprovePrescription()
  const [justification, setJustification] = useState('')
  const err = approve.error ? queryError(approve.error) : null

  const canApprove = item.source === 'PRESCRIPTION' && item.capabilities.approve && canManage
  const pending = item.status === 'PENDING'

  return (
    <Card className={`flex flex-col gap-2.5 ${pending ? 'border-ag-t-500 shadow-[0_0_0_3px_var(--color-ag-t-50)]' : ''}`}>
      <div className="flex items-center justify-between">
        <h2 className="font-display text-base font-bold">{t('decisions.approval.title')}</h2>
        {canApprove ? <span className="text-xs text-ag-n-600">{t('decisions.approval.youApprove')}</span> : null}
      </div>

      {!pending ? (
        <p className="text-sm text-ag-n-700">
          {item.status === 'APPROVED' || item.status === 'EXECUTED'
            ? t('decisions.approval.alreadyApproved', { when: item.approvedAt ? dateTime(item.approvedAt) : '—' })
            : item.status === 'REJECTED'
              ? t('decisions.approval.rejected')
              : t('decisions.approval.noWorkflow')}
        </p>
      ) : null}

      {pending && canApprove ? (
        <>
          <div className="flex items-center gap-2">
            <span className="grid h-[30px] w-[30px] place-items-center rounded-full bg-ag-g-600 text-[11px] font-bold text-white" aria-hidden>
              {(name ?? '·').slice(0, 2).toUpperCase()}
            </span>
            <div className="min-w-0">
              <b className="block truncate text-[13px]">{name}</b>
              <span className="text-xs text-ag-n-600">{label(role)}</span>
            </div>
          </div>
          <label className="flex flex-col gap-1 text-xs font-semibold" htmlFor="decision-justification">
            {t('decisions.approval.justification')}{' '}
            <span className="font-normal text-ag-n-600">{t('decisions.approval.required')}</span>
            <textarea
              id="decision-justification"
              value={justification}
              onChange={(e) => setJustification(e.target.value)}
              rows={3}
              className="w-full resize-none rounded-[8px] border border-ag-n-300 bg-ag-n-0 p-2 text-xs font-normal text-ag-n-900"
            />
          </label>
          <Button
            className="w-full"
            disabled={justification.trim() === '' || approve.isPending}
            onClick={() => approve.mutate(item.rawId)}
          >
            <CircleCheck className="h-4 w-4" aria-hidden />
            {approve.isPending ? t('decisions.approval.approving') : t('decisions.approval.approve')}
          </Button>
          <p className="flex items-start gap-1 text-[11px] text-ag-n-600">
            <Lock className="mt-0.5 h-3 w-3 shrink-0" aria-hidden />
            {t('decisions.approval.justificationNotSent')}
          </p>
        </>
      ) : null}

      {pending && item.source === 'PRESCRIPTION' && !canManage ? (
        <p className="text-sm text-ag-n-600">{t('decisions.approval.noPermission')}</p>
      ) : null}

      {pending && item.source === 'IRRIGATION' ? (
        <>
          <p className="text-sm text-ag-n-700">{t('decisions.approval.irrigationHint')}</p>
          <Link
            to={item.farmId ? `/irrigation?farm=${encodeURIComponent(item.farmId)}` : '/irrigation'}
            className="inline-flex items-center justify-center rounded-[8px] bg-ag-t-600 px-3 py-2 text-[13px] font-semibold text-white hover:bg-ag-t-700"
          >
            {t('decisions.approval.openIrrigation')}
          </Link>
        </>
      ) : null}

      {pending && item.source === 'AGRONOMY' ? (
        <p className="text-sm text-ag-n-600">{t('decisions.approval.noWorkflow')}</p>
      ) : null}

      {err ? (
        <p className="text-xs text-ag-crit" role="alert">
          {err.message}
          {err.correlationId ? ` · ${t('common.correlationId')} ${err.correlationId}` : ''}
        </p>
      ) : null}
      {approve.isSuccess ? (
        <p className="text-xs text-ag-ok" role="status">
          {t('decisions.approval.approved')}
        </p>
      ) : null}
    </Card>
  )
}
