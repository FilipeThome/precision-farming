export type FieldState = 'planned' | 'progress' | 'done' | 'blocked' | 'stale' | 'none'

export const FIELD_STATE_COLORS: Record<FieldState, { fill: string; stroke: string; fillOpacity: number; dash?: string }> = {
  planned: { fill: '#d9c9a3', stroke: '#c9b98d', fillOpacity: 0.55 },
  progress: { fill: '#12a08e', stroke: '#0b6e62', fillOpacity: 0.6 },
  done: { fill: '#2a7350', stroke: '#1f5a3c', fillOpacity: 0.55 },
  blocked: { fill: '#d9d6ca', stroke: '#b42318', fillOpacity: 0.5, dash: '8 5' },
  stale: { fill: '#aaa697', stroke: '#8a877b', fillOpacity: 0.4, dash: '4 4' },
  none: { fill: '#e2dfd5', stroke: '#8a877b', fillOpacity: 0.18 },
}

/** Legend order shown on the Tower map. */
export const FIELD_STATE_LEGEND: FieldState[] = ['done', 'progress', 'planned', 'blocked', 'stale', 'none']
