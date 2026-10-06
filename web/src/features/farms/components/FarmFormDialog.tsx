import { useEffect, useState } from 'react'

import { useFarmCommands } from '@/features/farms/queries'
import type { Farm } from '@/features/farms/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { EntityFormDialog } from '@/shared/ui/EntityFormDialog'

type Props = { open: boolean; farm: Farm | null; onClose: () => void }

export function FarmFormDialog({ open, farm, onClose }: Props) {
  const { t } = useI18n()
  const commands = useFarmCommands()
  const [values, setValues] = useState(blank())

  useEffect(() => {
    if (!open) return
    setValues(
      farm
        ? {
            name: farm.name,
            location: farm.location,
            areaHa: String(farm.areaHa),
            timezone: farm.timezone,
          }
        : blank(),
    )
  }, [open, farm])

  const pending = commands.create.isPending || commands.patch.isPending
  const failure = commands.create.error || commands.patch.error
  const error = failure ? queryError(failure).message : null

  return (
    <EntityFormDialog
      open={open}
      title={farm ? t('form.edit') : t('form.new')}
      fields={[
        { name: 'name', label: t('form.field.name'), type: 'text', required: true },
        { name: 'location', label: t('form.field.location'), type: 'text', required: true },
        { name: 'areaHa', label: t('form.field.areaHa'), type: 'number', required: true, step: '0.1', min: '0' },
        { name: 'timezone', label: t('form.field.timezone'), type: 'text', required: true },
      ]}
      values={values}
      onChange={(name, value) => setValues((prev) => ({ ...prev, [name]: value }))}
      pending={pending}
      error={error}
      onClose={onClose}
      onSubmit={() => {
        const body = {
          name: values.name.trim(),
          location: values.location.trim(),
          areaHa: Number(values.areaHa),
          timezone: values.timezone.trim() || 'America/Sao_Paulo',
        }
        const req = farm ? commands.patch.mutateAsync({ id: farm.id, body }) : commands.create.mutateAsync(body)
        void req.then(onClose)
      }}
    />
  )
}

function blank() {
  return { name: '', location: '', areaHa: '', timezone: 'America/Sao_Paulo' }
}
