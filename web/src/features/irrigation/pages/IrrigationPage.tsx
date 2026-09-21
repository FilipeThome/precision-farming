import { useState } from 'react'

import { IrrigationAssetFormDialog } from '@/features/irrigation/components/IrrigationAssetFormDialog'
import {
  useIrrigationAssetsQuery,
  useIrrigationRecommendationsQuery,
} from '@/features/irrigation/queries'
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
import { SectionTabs } from '@/shared/ui/SectionTabs'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

type Tab = 'assets' | 'recommendations'

export function IrrigationPage() {
  const farmId = useUiStore((s) => s.farmId)
  const [tab, setTab] = useState<Tab>('assets')
  const { t } = useI18n()
  const { number, dateTime, label } = useFormat()
  const { selectedId, setSelectedId } = useSelectedId()
  const assets = useIrrigationAssetsQuery(farmId, { enabled: tab === 'assets' })
  const recommendations = useIrrigationRecommendationsQuery(farmId, {
    enabled: tab === 'recommendations',
  })
  const active = tab === 'assets' ? assets : recommendations
  const err = queryError(active.error)
  const canWrite = useCanWriteMasterData()
  const [form, setForm] = useState<'create' | 'edit' | null>(null)
  const selected = (assets.data ?? []).find((row) => row.id === selectedId)

  return (
    <section>
      <PageHeader
        title={t('irrigation.title')}
        description={t('irrigation.description')}
        actions={
          canWrite && tab === 'assets' ? (
            <Button type="button" onClick={() => setForm('create')}>
              {t('form.new')}
            </Button>
          ) : null
        }
      />
      <SectionTabs
        active={tab}
        onChange={setTab}
        tabs={[
          { id: 'assets', labelKey: 'irrigation.tab.assets' },
          { id: 'recommendations', labelKey: 'irrigation.tab.recommendations' },
        ]}
      />
      <QueryPageState
        isLoading={active.isLoading}
        isError={active.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!active.isLoading && (active.data?.length ?? 0) === 0}
        emptyTitle={
          tab === 'assets'
            ? t('irrigation.assets.emptyTitle')
            : t('irrigation.recommendations.emptyTitle')
        }
        emptyDescription={
          tab === 'assets'
            ? t('irrigation.assets.emptyDescription')
            : t('irrigation.recommendations.emptyDescription')
        }
        onRetry={() => void active.refetch()}
      >
        <div className="grid gap-3 md:grid-cols-2">
          {tab === 'assets'
            ? (assets.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={label(row.name, row.id)}
                  subtitle={label(row.type)}
                  kind="irrigation"
                  selected={row.id === selectedId}
                  onSelect={() => setSelectedId(row.id)}
                >
                  {row.status ? <StatusBadge value={row.status} /> : null}
                </EntityCard>
              ))
            : (recommendations.data ?? []).map((row) => (
                <EntityCard
                  key={row.id}
                  title={label(row.reason, row.fieldId ?? row.id)}
                  subtitle={
                    row.volumeMm != null ? `${number(Number(row.volumeMm), 1)} mm` : undefined
                  }
                  meta={`${label(row.priority)} · ${dateTime(row.recommendedAt)}`}
                  kind="irrigation"
                />
              ))}
        </div>
      </QueryPageState>
      <DetailDrawer
        open={Boolean(selectedId) && tab === 'assets'}
        title={selected ? label(selected.name, selected.id) : t('inspector.notFound')}
        onClose={() => setSelectedId(null)}
      >
        {selected && canWrite ? <InspectorEditButton onEdit={() => setForm('edit')} /> : null}
      </DetailDrawer>
      <IrrigationAssetFormDialog
        open={form !== null}
        asset={form === 'edit' ? selected ?? null : null}
        onClose={() => setForm(null)}
      />
    </section>
  )
}
