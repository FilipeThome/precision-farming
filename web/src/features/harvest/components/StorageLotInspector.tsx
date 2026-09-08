import type { StorageLot } from '@/shared/api/types'
import { cropPhoto, storagePhoto } from '@/shared/demo/media'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { EntityPhoto } from '@/shared/ui/EntityPhoto'
import { InspectorKpis } from '@/shared/ui/InspectorKpis'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = { lot: StorageLot }

export function StorageLotInspector({ lot }: Props) {
  const { t } = useI18n()
  const { number, label, dateTime } = useFormat()

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-3">
        <EntityPhoto src={cropPhoto(lot.crop) || storagePhoto()} alt={label(lot.crop)} />
        <StatusBadge value={lot.quality} />
      </div>
      <InspectorKpis
        items={[
          { label: t('harvest.kpi.actual'), value: `${number(Number(lot.tons), 1)} t` },
          { label: t('fields.kpi.crop'), value: label(lot.crop) },
        ]}
      />
      <p className="text-xs text-pf-muted">{dateTime(lot.receivedAt)}</p>
    </div>
  )
}
