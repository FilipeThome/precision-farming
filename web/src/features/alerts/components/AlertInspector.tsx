import { Link } from 'react-router'

import { useAckAlertMutation } from '@/features/alerts/queries'
import type { Alert } from '@/shared/api/types'
import { farmPhoto, machinePhoto } from '@/shared/demo/media'
import { inspectHref } from '@/shared/lib/useFarmFromSearch'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { Button } from '@/shared/ui/Button'
import { EntityPhoto } from '@/shared/ui/EntityPhoto'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = { alert: Alert }

export function AlertInspector({ alert }: Props) {
  const ack = useAckAlertMutation()
  const { t } = useI18n()
  const { label, dateTime } = useFormat()
  const thumb =
    alert.entityType === 'MACHINE' && alert.entityId
      ? machinePhoto(alert.entityId)
      : farmPhoto(alert.farmId)
  const machineLink =
    alert.entityType === 'MACHINE' && alert.entityId
      ? inspectHref('/machines', alert.entityId, alert.farmId)
      : null
  const fieldLink =
    alert.entityType === 'FIELD' && alert.entityId
      ? inspectHref('/fields', alert.entityId, alert.farmId)
      : null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-3">
        <EntityPhoto src={thumb} alt={label(alert.title)} />
        <div className="flex flex-wrap gap-2">
          <StatusBadge value={alert.severity} />
          <StatusBadge value={alert.status} />
        </div>
      </div>
      <p className="text-sm text-pf-muted">{label(alert.message)}</p>
      <p className="text-xs text-pf-muted">{dateTime(alert.createdAt)}</p>
      {alert.entityType ? (
        <p className="text-sm">
          <span className="text-pf-muted">{t('alerts.relatedEntity')}: </span>
          {label(alert.entityType)}
        </p>
      ) : null}
      <div className="flex flex-wrap gap-2">
        {machineLink ? (
          <Link
            to={machineLink}
            className="text-sm font-medium text-pf-teal underline-offset-2 hover:underline"
          >
            {t('alerts.jumpMachine')}
          </Link>
        ) : null}
        {fieldLink ? (
          <Link
            to={fieldLink}
            className="text-sm font-medium text-pf-teal underline-offset-2 hover:underline"
          >
            {t('alerts.jumpField')}
          </Link>
        ) : null}
      </div>
      {alert.status === 'OPEN' ? (
        <Button disabled={ack.isPending} onClick={() => ack.mutate(alert.id)}>
          {t('alerts.ack')}
        </Button>
      ) : null}
    </div>
  )
}
