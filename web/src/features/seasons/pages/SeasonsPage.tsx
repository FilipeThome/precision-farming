import { useState } from 'react'

import { SeasonFormDialog } from '@/features/seasons/components/SeasonFormDialog'
import { SeasonInspector } from '@/features/seasons/components/SeasonInspector'
import { useSeasonsQuery } from '@/features/seasons/queries'
import { useCanWriteMasterData } from '@/shared/auth/roles'
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
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

export function SeasonsPage() {
  const farmId = useUiStore((s) => s.farmId)
  const seasons = useSeasonsQuery(farmId)
  const err = queryError(seasons.error)
  const { t } = useI18n()
  const { date, label } = useFormat()
  const { selectedId, setSelectedId } = useSelectedId()
  const selected = (seasons.data ?? []).find((season) => season.id === selectedId)
  const canWrite = useCanWriteMasterData()
  const [form, setForm] = useState<'create' | 'edit' | null>(null)

  return (
    <section>
      <PageHeader
        title={t('seasons.title')}
        description={t('seasons.description')}
        actions={
          canWrite ? (
            <Button type="button" onClick={() => setForm('create')}>
              {t('form.new')}
            </Button>
          ) : null
        }
      />
      <QueryPageState
        isLoading={seasons.isLoading}
        isError={seasons.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!seasons.isLoading && (seasons.data?.length ?? 0) === 0}
        emptyTitle={t('seasons.emptyTitle')}
        emptyDescription={t('seasons.emptyDescription')}
        onRetry={() => void seasons.refetch()}
      >
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {(seasons.data ?? []).map((season) => (
            <EntityCard
              key={season.id}
              title={label(season.name, season.id)}
              subtitle={season.crop ? label(season.crop) : undefined}
              meta={`${date(season.startDate)} – ${date(season.endDate)}`}
              kind="season"
              selected={season.id === selectedId}
              onSelect={() => setSelectedId(season.id)}
            >
              {season.status ? <StatusBadge value={season.status} /> : null}
            </EntityCard>
          ))}
        </div>
      </QueryPageState>
      <DetailDrawer
        open={Boolean(selectedId)}
        title={selected ? label(selected.name, selected.id) : t('inspector.notFound')}
        subtitle={selected ? undefined : t('inspector.notFoundHint')}
        onClose={() => setSelectedId(null)}
      >
        {selected ? (
          <>
            <SeasonInspector season={selected} />
            {canWrite ? <InspectorEditButton onEdit={() => setForm('edit')} /> : null}
          </>
        ) : null}
      </DetailDrawer>
      <SeasonFormDialog
        open={form !== null}
        season={form === 'edit' ? selected ?? null : null}
        onClose={() => setForm(null)}
      />
    </section>
  )
}
