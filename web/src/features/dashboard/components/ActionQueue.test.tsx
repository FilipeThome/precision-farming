import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'

import { ActionQueue } from '@/features/dashboard/components/ActionQueue'
import type { DecisionItem } from '@/features/decisions/model'
import type { Alert } from '@/shared/api/types'

const alert: Alert = {
  id: 'a1',
  farmId: 'farm-1',
  severity: 'CRITICAL',
  type: 'WEATHER',
  title: 'Rain',
  message: 'Storm',
  entityType: null,
  entityId: null,
  status: 'OPEN',
  createdAt: '2026-09-01T00:00:00Z',
}

const prescription: DecisionItem = {
  id: 'PRESCRIPTION:p1',
  rawId: 'p1',
  source: 'PRESCRIPTION',
  title: 'GLYPHOSATE',
  quantity: { value: 2.5, unit: 'L/ha' },
  fieldId: 'field-1',
  farmId: 'farm-3',
  status: 'PENDING',
  rawStatus: 'DRAFT',
  createdAt: '2026-09-01T00:00:00Z',
  capabilities: { approve: true, simulate: false },
}

const insight: DecisionItem = {
  id: 'AI_INSIGHT:i1',
  rawId: 'i1',
  source: 'AI_INSIGHT',
  title: 'YIELD_FORECAST',
  status: 'UNKNOWN',
  confidence: 0.41,
  createdAt: '2026-09-01T00:00:00Z',
  capabilities: { approve: false, simulate: false },
}

function renderQueue(decisions: DecisionItem[], alerts: Alert[]) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ActionQueue decisions={decisions} alerts={alerts} isLoading={false} isError={false} onRetry={() => {}} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ActionQueue', () => {
  it('links alerts with farm (inspectHref contract) and decisions to the ledger', () => {
    renderQueue([prescription], [alert])
    const hrefs = screen.getAllByRole('link').map((el) => el.getAttribute('href'))
    expect(hrefs).toContain('/alerts?selected=a1&farm=farm-1')
    expect(hrefs).toContain('/decisions/PRESCRIPTION%3Ap1?farm=farm-3')
    expect(screen.getByRole('button', { name: 'Reconhecer' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Revisar' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Aprovar' })).not.toBeInTheDocument()
  })

  it('flags low-confidence insights for human review with a confidence meter', () => {
    renderQueue([insight], [])
    expect(screen.getByText('Revisão humana')).toBeInTheDocument()
    expect(screen.getByRole('meter')).toHaveAttribute('aria-valuenow', '0.41')
  })

  it('renders an explicit empty state', () => {
    renderQueue([], [{ ...alert, status: 'ACKED' }])
    expect(screen.getByRole('status')).toHaveTextContent('Nada pendente')
  })
})
