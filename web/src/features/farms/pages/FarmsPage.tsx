import { useState } from 'react'

import { FarmFormDialog } from '@/features/farms/components/FarmFormDialog'
import { FarmInspector } from '@/features/farms/components/FarmInspector'
import { useFarmsQuery } from '@/features/farms/queries'
import { useCanCreateFarm, useCanWriteMasterData } from '@/shared/auth/roles'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { useSelectedId } from '@/shared/lib/useSelectedId'
import { Button } from '@/shared/ui/Button'
import { DetailDrawer } from '@/shared/ui/DetailDrawer'
import { EntityCard } from '@/shared/ui/EntityCard'
import { InspectorEditButton } from '@/shared/ui/InspectorEditButton'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'

export function FarmsPage() {
  const farms = useFarmsQuery()
  const err = queryError(farms.error)
  const { t } = useI18n()
  const { number, label } = useFormat()
  const { selectedId, setSelectedId } = useSelectedId()
  const selected = (farms.data ?? []).find((farm) => farm.id === selectedId)
  const canCreate = useCanCreateFarm()
  const canEdit = useCanWriteMasterData()
  const [form, setForm] = useState<'create' | 'edit' | null>(null)

  return (
    <section>
      <PageHeader
        title={t('farms.title')}
        description={t('farms.description')}
        actions={
          canCreate ? (
            <Button type="button" onClick={() => setForm('create')}>
              {t('form.new')}
            </Button>
          ) : null
        }
      />
      <QueryPageState
        isLoading={farms.isLoading}
        isError={farms.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!farms.isLoading && (farms.data?.length ?? 0) === 0}
        emptyTitle={t('farms.emptyTitle')}
        emptyDescription={t('farms.emptyDescription')}
        onRetry={() => void farms.refetch()}
      >
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {(farms.data ?? []).map((farm) => (
            <EntityCard
              key={farm.id}
              title={label(farm.id, farm.name)}
              subtitle={farm.location}
              meta={`${number(Number(farm.areaHa), 1)} ha · ${farm.timezone}`}
              kind="farm"
              selected={farm.id === selectedId}
              onSelect={() => setSelectedId(farm.id)}
            />
          ))}
        </div>
      </QueryPageState>
      <DetailDrawer
        open={Boolean(selectedId)}
        title={selected ? label(selected.id, selected.name) : t('inspector.notFound')}
        subtitle={selected ? undefined : t('inspector.notFoundHint')}
        onClose={() => setSelectedId(null)}
      >
        {selected ? (
          <>
            <FarmInspector farm={selected} />
            {canEdit ? <InspectorEditButton onEdit={() => setForm('edit')} /> : null}
          </>
        ) : null}
      </DetailDrawer>
      <FarmFormDialog
        open={form !== null}
        farm={form === 'edit' ? selected ?? null : null}
        onClose={() => setForm(null)}
      />
    </section>
  )
}
