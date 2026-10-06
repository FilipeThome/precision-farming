import type { IntegrationConnector } from '@/features/integrations/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { EntityCard } from '@/shared/ui/EntityCard'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = {
  connector: IntegrationConnector
}

export function ConnectorCard({ connector }: Props) {
  const { t } = useI18n()
  const { label } = useFormat()

  return (
    <EntityCard
      title={label(connector.name)}
      subtitle={label(connector.type)}
      meta={t('integrations.mode', { mode: label(connector.mode) })}
    >
      <div className="mt-2 flex flex-wrap gap-2">
        <StatusBadge value={connector.mode} />
        {connector.capabilities.map((capability) => (
          <span
            key={capability}
            className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-700"
          >
            {label(capability)}
          </span>
        ))}
      </div>
      <p className="mt-2 text-xs text-pf-muted">{t('integrations.capabilities')}</p>
    </EntityCard>
  )
}
