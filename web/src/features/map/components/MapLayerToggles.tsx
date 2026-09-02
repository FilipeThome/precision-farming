import type { MapLayer } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'

type Props = {
  layers: MapLayer[]
  enabledKinds: Set<string>
  onToggle: (kind: string) => void
}

export function MapLayerToggles({ layers, enabledKinds, onToggle }: Props) {
  const { t } = useI18n()
  const { label } = useFormat()
  const kinds = [...new Set(layers.map((layer) => layer.kind))].sort()

  if (kinds.length === 0) {
    return (
      <Card className="mb-4">
        <p className="text-sm text-pf-muted">{t('map.layers.empty')}</p>
      </Card>
    )
  }

  const active = layers.filter((layer) => enabledKinds.has(layer.kind))

  return (
    <Card className="mb-4">
      <p className="mb-2 text-sm font-medium text-pf-green">{t('map.layers.title')}</p>
      <div className="mb-3 flex flex-wrap gap-2">
        {kinds.map((kind) => (
          <Button
            key={kind}
            variant={enabledKinds.has(kind) ? 'primary' : 'secondary'}
            aria-pressed={enabledKinds.has(kind)}
            onClick={() => onToggle(kind)}
          >
            {label(kind)}
          </Button>
        ))}
      </div>
      {active.length > 0 ? (
        <div>
          <p className="mb-1 text-xs text-pf-muted">{t('map.layers.legend')}</p>
          <ul className="space-y-1 text-sm text-pf-muted">
            {active.map((layer) => (
              <li key={layer.id}>
                {label(layer.kind)} · {label(layer.name)} · {label(layer.status)}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </Card>
  )
}
