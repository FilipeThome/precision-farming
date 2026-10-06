import { useEffect, useState } from 'react'

import { useSeasonCommands } from '@/features/seasons/queries'
import type { Season } from '@/features/seasons/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { EntityFormDialog } from '@/shared/ui/EntityFormDialog'
import { useCodeOptions, useFarmScope } from '@/shared/ui/useFarmScope'

type Props = { open: boolean; season: Season | null; onClose: () => void }

export function SeasonFormDialog({ open, season, onClose }: Props) {
  const { t } = useI18n()
  const { farmField, defaultFarmId } = useFarmScope()
  const crops = useCodeOptions(['SOY', 'CORN', 'COTTON'])
  const statuses = useCodeOptions(['PLANNED', 'ACTIVE', 'COMPLETED'])
  const commands = useSeasonCommands()
  const [values, setValues] = useState(blank(defaultFarmId))

  useEffect(() => {
    if (!open) return
    setValues(
      season
        ? {
            farmId: season.farmId ?? defaultFarmId,
            name: season.name ?? '',
            crop: season.crop ?? 'SOY',
            startDate: season.startDate?.slice(0, 10) ?? '',
            endDate: season.endDate?.slice(0, 10) ?? '',
            status: season.status ?? 'PLANNED',
          }
        : blank(defaultFarmId),
    )
  }, [open, season, defaultFarmId])

  const failure = commands.create.error || commands.patch.error
  return (
    <EntityFormDialog
      open={open}
      title={season ? t('form.edit') : t('form.new')}
      fields={[
        farmField,
        { name: 'name', label: t('form.field.name'), type: 'text', required: true },
        { name: 'crop', label: t('form.field.crop'), type: 'select', required: true, options: crops },
        { name: 'startDate', label: t('form.field.startDate'), type: 'date', required: true },
        { name: 'endDate', label: t('form.field.endDate'), type: 'date' },
        { name: 'status', label: t('form.field.status'), type: 'select', required: true, options: statuses },
      ]}
      values={values}
      onChange={(name, value) => setValues((prev) => ({ ...prev, [name]: value }))}
      pending={commands.create.isPending || commands.patch.isPending}
      error={failure ? queryError(failure).message : null}
      onClose={onClose}
      onSubmit={() => {
        const body = {
          farmId: values.farmId,
          name: values.name.trim(),
          crop: values.crop,
          startDate: values.startDate,
          endDate: values.endDate || null,
          status: values.status,
        }
        const req = season
          ? commands.patch.mutateAsync({ id: season.id, body })
          : commands.create.mutateAsync(body)
        void req.then(onClose)
      }}
    />
  )
}

function blank(farmId: string) {
  return { farmId, name: '', crop: 'SOY', startDate: '', endDate: '', status: 'PLANNED' }
}
