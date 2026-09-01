import type { IntegrationConnector } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { EntityCard } from '@/shared/ui/EntityCard'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = {
  connector: IntegrationConnector
}

export function ConnectorCard({ connector }: Props) {
  const { t } = useI18n()

  return (
    <EntityCard
      title={connector.name}
      subtitle={connector.type}
      meta={t('integrations.mode', { mode: connector.mode })}
    >
      <div className="mt-2 flex flex-wrap gap-2">
        <StatusBadge value={connector.mode} />
        {connector.capabilities.map((capability) => (
          <span
            key={capability}
            className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-700"
          >
            {capability}
          </span>
        ))}
      </div>
      <p className="mt-2 text-xs text-pf-muted">{t('integrations.capabilities')}</p>
    </EntityCard>
  )
}
