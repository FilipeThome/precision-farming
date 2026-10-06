import { useEffect, useState } from 'react'

import { useFieldCommands } from '@/features/fields/queries'
import type { Field } from '@/features/fields/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { centroidOfGeometry, geometryForFieldSave } from '@/shared/maps/geometry'
import { EntityFormDialog } from '@/shared/ui/EntityFormDialog'
import { useCodeOptions, useFarmScope } from '@/shared/ui/useFarmScope'

type Props = { open: boolean; field: Field | null; onClose: () => void }

export function FieldFormDialog({ open, field, onClose }: Props) {
  const { t } = useI18n()
  const { farmField, defaultFarmId } = useFarmScope()
  const crops = useCodeOptions(['SOY', 'CORN', 'COTTON'])
  const commands = useFieldCommands()
  const [values, setValues] = useState(blank(defaultFarmId))
  const [origin, setOrigin] = useState({ lat: '', lon: '', areaHa: '' })

  useEffect(() => {
    if (!open) return
    if (!field) {
      const next = blank(defaultFarmId)
      setValues(next)
      setOrigin({ lat: next.lat, lon: next.lon, areaHa: next.areaHa })
      return
    }
    const c = centroidOfGeometry(field.geometry)
    const next = {
      farmId: field.farmId,
      name: field.name,
      crop: field.crop,
      variety: field.variety ?? '',
      lat: c ? String(c.lat) : '',
      lon: c ? String(c.lon) : '',
      areaHa: String(field.areaHa),
    }
    setValues(next)
    setOrigin({ lat: next.lat, lon: next.lon, areaHa: next.areaHa })
  }, [open, field, defaultFarmId])

  const failure = commands.create.error || commands.patch.error
  return (
    <EntityFormDialog
      open={open}
      title={field ? t('form.edit') : t('form.new')}
      fields={[
        ...(field ? [] : [farmField]),
        { name: 'name', label: t('form.field.name'), type: 'text', required: true },
        { name: 'crop', label: t('form.field.crop'), type: 'select', required: true, options: crops },
        { name: 'variety', label: t('form.field.variety'), type: 'text' },
        { name: 'lat', label: t('form.field.lat'), type: 'number', required: true, step: '0.0001' },
        { name: 'lon', label: t('form.field.lon'), type: 'number', required: true, step: '0.0001' },
        { name: 'areaHa', label: t('form.field.areaHa'), type: 'number', required: true, step: '0.1', min: '0' },
      ]}
      values={values}
      onChange={(name, value) => {
        if (field && name === 'farmId') return
        setValues((prev) => ({ ...prev, [name]: value }))
      }}
      pending={commands.create.isPending || commands.patch.isPending}
      error={failure ? queryError(failure).message : null}
      onClose={onClose}
      onSubmit={() => {
        const body = {
          farmId: field ? field.farmId : values.farmId,
          name: values.name.trim(),
          crop: values.crop,
          variety: values.variety.trim() || null,
          areaHa: Number(values.areaHa),
          geometry: geometryForFieldSave({
            existingGeometry: field?.geometry,
            originalLat: origin.lat,
            originalLon: origin.lon,
            originalAreaHa: origin.areaHa,
            lat: values.lat,
            lon: values.lon,
            areaHa: values.areaHa,
          }),
        }
        const req = field ? commands.patch.mutateAsync({ id: field.id, body }) : commands.create.mutateAsync(body)
        void req.then(onClose)
      }}
    />
  )
}

function blank(farmId: string) {
  return { farmId, name: '', crop: 'SOY', variety: '', lat: '-22.9', lon: '-49.9', areaHa: '' }
}
