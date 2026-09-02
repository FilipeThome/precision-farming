import { describe, expect, it } from 'vitest'

import { pnlChartRows } from '@/shared/charts/adapters'
import { domainLabel } from '@/shared/i18n/domainLabels'
import type { FinancePnl } from '@/shared/api/types'

describe('pnlChartRows', () => {
  it('does not merge unknown farm UUIDs into one series', () => {
    const rows: FinancePnl[] = [
      {
        id: 'pnl-a',
        farmId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        farmName: undefined,
        revenue: 1,
        cost: 0,
        margin: 1,
        currency: 'BRL',
        period: 'YTD',
      },
      {
        id: 'pnl-b',
        farmId: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        farmName: undefined,
        revenue: 2,
        cost: 0,
        margin: 2,
        currency: 'BRL',
        period: 'YTD',
      },
    ]
    const chart = pnlChartRows(rows, (v) => domainLabel('en-US', v))
    expect(chart[0].name).toBe('aaaaaaaa')
    expect(chart[1].name).toBe('bbbbbbbb')
    expect(chart[0].name).not.toBe(chart[1].name)
  })
})
