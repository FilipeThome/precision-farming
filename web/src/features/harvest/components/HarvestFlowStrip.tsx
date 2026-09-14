import type { FlowStage, FlowStageId } from '@/features/harvest/model/postHarvestKpis'
import type { MessageKey } from '@/shared/i18n/useI18n'
import { useI18n } from '@/shared/i18n/useI18n'
import { Card } from '@/shared/ui/Card'
import { ChainRail } from '@/shared/ui/ChainRail'

const STAGE_KEY: Record<FlowStageId, MessageKey> = {
  harvest: 'postHarvest.flow.harvest',
  transport: 'postHarvest.flow.transport',
  storage: 'postHarvest.flow.storage',
  quality: 'postHarvest.flow.quality',
}

export function HarvestFlowStrip({ stages }: { stages: FlowStage[] }) {
  const { t } = useI18n()
  return (
    <Card className="px-3.5 pb-2 pt-1 shadow-none">
      <ChainRail
        aria-label={t('postHarvest.flow.label')}
        steps={stages.map((stage) => ({
          id: stage.id,
          label: t(STAGE_KEY[stage.id]),
          state: stage.state,
          hint: stage.count > 0 ? t('postHarvest.flow.count', { n: stage.count }) : undefined,
        }))}
      />
    </Card>
  )
}
