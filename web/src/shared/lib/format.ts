const statusLabels: Record<string, string> = {
  OPERATING: 'Operando',
  IDLE: 'Parada',
  MAINTENANCE: 'Manutenção',
  PLANNED: 'Planejada',
  IN_PROGRESS: 'Em andamento',
  PAUSED: 'Pausada',
  COMPLETED: 'Concluída',
  OPEN: 'Aberto',
  ACKED: 'Reconhecido',
  CRITICAL: 'Crítico',
  WARNING: 'Atenção',
  INFO: 'Info',
  FAVORABLE: 'Favorável',
  UNFAVORABLE: 'Desfavorável',
}

export function formatStatus(value: string): string {
  return statusLabels[value] ?? value
}

export function formatNumber(value: number, digits = 0): string {
  return new Intl.NumberFormat('pt-BR', {
    maximumFractionDigits: digits,
    minimumFractionDigits: digits,
  }).format(value)
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date)
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium' }).format(date)
}

export function formatPercent(value: number): string {
  return `${formatNumber(value * 100, 0)}%`
}
