import type { AiInsight } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { CHART_COLORS, ChartCard } from '@/shared/ui/ChartCard'
import { BarChartBlock } from '@/shared/ui/charts'
import { InspectorKpis } from '@/shared/ui/InspectorKpis'

type Props = { insight: AiInsight }

export function InsightInspector({ insight }: Props) {
  const { t } = useI18n()
  const { percent, dateTime, label } = useFormat()

  return (
    <div className="flex flex-col gap-4">
      <p className="text-sm text-pf-muted">
        {insight.model} · v{insight.modelVersion} · {dateTime(insight.generatedAt)}
      </p>
      <InspectorKpis
        items={[
          { label: t('ai.kpi.confidence'), value: percent(insight.confidence) },
          { label: t('charts.risk'), value: percent(insight.score) },
        ]}
      />
      <ChartCard title={t('ai.kpi.confidence')} description={t('charts.fromLive')} className="min-h-[220px]">
        <BarChartBlock
          data={[
            { name: t('ai.kpi.confidence'), value: Math.round(insight.confidence * 100) },
            { name: t('charts.risk'), value: Math.round(insight.score * 100) },
          ]}
          xKey="name"
          bars={[{ dataKey: 'value', color: CHART_COLORS.teal }]}
        />
      </ChartCard>
      <div>
        <h3 className="mb-2 text-sm font-semibold text-pf-green">{t('ai.factors')}</h3>
        <ul className="list-disc pl-5 text-sm text-pf-muted">
          {insight.explanation.map((line, index) => (
            <li key={`${insight.id}-${index}`}>{label(line)}</li>
          ))}
        </ul>
      </div>
    </div>
  )
}
