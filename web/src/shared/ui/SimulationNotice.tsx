import { useI18n } from '@/shared/i18n/useI18n'

type Props = {
  /** When true, show the simulation banner. */
  simulation?: boolean
  /** Optional farm id/name for the farm-scoped caption. */
  farm?: string | null
  className?: string
}

/** Banner shown whenever a gateway payload sets `simulation: true`. */
export function SimulationNotice({ simulation, farm, className = '' }: Props) {
  const { t } = useI18n()
  if (!simulation) return null
  return (
    <p
      role="status"
      className={`rounded-[8px] border border-ag-info/30 bg-ag-info-bg px-2.5 py-1.5 text-[11px] font-medium text-ag-info ${className}`}
    >
      {farm ? t('demo.simulationFarm', { farm }) : t('demo.simulation')}
    </p>
  )
}
