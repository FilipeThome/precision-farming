import { BadgeCheck, Bell, Wifi, WifiOff } from 'lucide-react'
import { Link } from 'react-router'

import { useAlertsQuery } from '@/features/alerts/queries'
import { useDecisionSources } from '@/features/decisions/queries'
import { pendingDecisions } from '@/features/decisions/selectors'
import { useI18n } from '@/shared/i18n/useI18n'
import { farmHref } from '@/shared/lib/useFarmFromSearch'
import { useOnline } from '@/shared/lib/useOnline'

type PillProps = {
  to?: string
  label: string
  tone: 'ok' | 'warn' | 'crit' | 'info' | 'neutral'
  icon?: 'dot' | 'check' | 'wifi' | 'wifioff'
}

const DOT: Record<PillProps['tone'], string> = {
  ok: 'bg-ag-ok',
  warn: 'bg-ag-warn',
  crit: 'bg-ag-crit',
  info: 'bg-ag-info',
  neutral: 'bg-ag-n-400',
}

function Pill({ to, label, tone, icon = 'dot' }: PillProps) {
  const body = (
    <>
      {icon === 'dot' ? <span className={`h-2 w-2 rounded-full ${DOT[tone]}`} aria-hidden /> : null}
      {icon === 'check' ? <BadgeCheck className="h-3.5 w-3.5" aria-hidden /> : null}
      {icon === 'wifi' ? <Wifi className="h-3.5 w-3.5 text-ag-ok" aria-hidden /> : null}
      {icon === 'wifioff' ? <WifiOff className="h-3.5 w-3.5 text-ag-warn" aria-hidden /> : null}
      <span>{label}</span>
    </>
  )
  const className =
    'inline-flex items-center gap-1.5 rounded-full border border-ag-n-200 bg-ag-n-0 px-2.5 py-1 text-xs font-medium text-ag-n-700'
  if (to) {
    return (
      <Link to={to} className={`${className} hover:border-ag-t-500`}>
        {body}
      </Link>
    )
  }
  return (
    <span className={className} role="status">
      {body}
    </span>
  )
}

/** Trust pills: online/offline, open alerts, pending approvals, bell. Nothing mocked. */
export function TrustStrip({ farmId }: { farmId: string | null }) {
  const { t } = useI18n()
  const online = useOnline()
  const alerts = useAlertsQuery(farmId)
  const decisions = useDecisionSources(farmId)
  const openAlerts = (alerts.data ?? []).filter((a) => a.status === 'OPEN').length
  const pending = pendingDecisions(decisions.items).length

  return (
    <div className="flex items-center gap-1.5" aria-label={t('trust.label')}>
      <Pill
        label={online ? t('trust.online') : t('trust.offline')}
        tone={online ? 'ok' : 'warn'}
        icon={online ? 'wifi' : 'wifioff'}
      />
      {alerts.data ? (
        <Pill to={farmHref('/alerts', farmId)} label={t('trust.openAlerts', { n: openAlerts })} tone={openAlerts > 0 ? 'crit' : 'ok'} />
      ) : null}
      {!decisions.isLoading ? (
        <Pill to={farmHref('/decisions', farmId)} label={t('trust.pendingApprovals', { n: pending })} tone="info" icon="check" />
      ) : null}
      <Link
        to={farmHref('/alerts', farmId)}
        className="relative grid h-11 w-11 place-items-center rounded-[8px] border border-ag-n-200 bg-ag-n-0 text-ag-n-700 hover:border-ag-t-500"
        aria-label={t('chrome.openAlerts', { count: openAlerts })}
        title={t('chrome.openAlerts', { count: openAlerts })}
      >
        <Bell className="h-4 w-4" aria-hidden />
        {openAlerts > 0 ? (
          <span className="absolute -right-1.5 -top-1.5 grid h-4 min-w-4 place-items-center rounded-full bg-ag-crit px-1 text-[10px] font-bold text-white">
            {openAlerts}
          </span>
        ) : null}
      </Link>
    </div>
  )
}
