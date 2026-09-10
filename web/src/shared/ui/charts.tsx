import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

import { useI18n } from '@/shared/i18n/useI18n'
import { CHART_COLORS, CHART_SERIES } from '@/shared/ui/ChartCard'

function ChartEmpty() {
  const { t } = useI18n()
  return <p className="flex h-full items-center justify-center text-sm text-pf-muted">{t('charts.empty')}</p>
}

type NamedValue = { name: string; value: number }

type BarChartBlockProps = {
  data: Array<Record<string, string | number>>
  xKey: string
  bars: Array<{ dataKey: string; color?: string; name?: string }>
}

export function BarChartBlock({ data, xKey, bars }: BarChartBlockProps) {
  if (data.length === 0) return <ChartEmpty />
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#d7ddd8" />
        <XAxis dataKey={xKey} tick={{ fontSize: 11, fill: CHART_COLORS.muted }} />
        <YAxis tick={{ fontSize: 11, fill: CHART_COLORS.muted }} />
        <Tooltip />
        {bars.length > 1 ? <Legend /> : null}
        {bars.map((bar, i) => (
          <Bar
            key={bar.dataKey}
            dataKey={bar.dataKey}
            name={bar.name ?? bar.dataKey}
            fill={bar.color ?? CHART_SERIES[i % CHART_SERIES.length]}
            radius={[4, 4, 0, 0]}
          />
        ))}
      </BarChart>
    </ResponsiveContainer>
  )
}

type LineChartBlockProps = {
  data: Array<Record<string, string | number>>
  xKey: string
  lines: Array<{ dataKey: string; color?: string; name?: string }>
}

export function LineChartBlock({ data, xKey, lines }: LineChartBlockProps) {
  if (data.length === 0) return <ChartEmpty />
  return (
    <ResponsiveContainer width="100%" height="100%">
      <LineChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#d7ddd8" />
        <XAxis dataKey={xKey} tick={{ fontSize: 11, fill: CHART_COLORS.muted }} />
        <YAxis tick={{ fontSize: 11, fill: CHART_COLORS.muted }} />
        <Tooltip />
        {lines.length > 1 ? <Legend /> : null}
        {lines.map((line, i) => (
          <Line
            key={line.dataKey}
            type="monotone"
            dataKey={line.dataKey}
            name={line.name ?? line.dataKey}
            stroke={line.color ?? CHART_SERIES[i % CHART_SERIES.length]}
            strokeWidth={2}
            dot={false}
          />
        ))}
      </LineChart>
    </ResponsiveContainer>
  )
}

type PieChartBlockProps = {
  data: NamedValue[]
}

export function PieChartBlock({ data }: PieChartBlockProps) {
  if (data.length === 0) return <ChartEmpty />
  return (
    <ResponsiveContainer width="100%" height="100%">
      <PieChart>
        <Pie data={data} dataKey="value" nameKey="name" innerRadius={48} outerRadius={78} paddingAngle={2}>
          {data.map((_, i) => (
            <Cell key={i} fill={CHART_SERIES[i % CHART_SERIES.length]} />
          ))}
        </Pie>
        <Tooltip />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  )
}
