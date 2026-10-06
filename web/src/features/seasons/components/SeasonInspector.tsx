import { PlantingGateCard } from '@/features/seasons/components/PlantingGateCard'
import type { Season } from '@/features/seasons/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { EntityTile } from '@/shared/ui/EntityTile'
import { StatusBadge } from '@/shared/ui/StatusBadge'

export function SeasonInspector({ season }: { season: Season }) {
  const { t } = useI18n()
  const { date, label } = useFormat()
  return (
    <div className="flex flex-col gap-3">
      <div className="flex gap-3">
        <EntityTile kind="season" size="lg" label={label(season.name, season.id)} />
        {season.status ? <StatusBadge value={season.status} /> : null}
      </div>
      <p className="text-sm text-pf-muted">{season.crop ? label(season.crop) : t('form.field.crop')}</p>
      <p className="text-xs text-pf-muted">
        {date(season.startDate)} – {date(season.endDate)}
      </p>
      <PlantingGateCard season={season} />
    </div>
  )
}
