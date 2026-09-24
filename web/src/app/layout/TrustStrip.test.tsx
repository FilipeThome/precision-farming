import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { TrustStrip } from '@/app/layout/TrustStrip'
import type { AiInsight, Alert, IrrigationRecommendation, Prescription } from '@/shared/api/types'

const prescription = (id: string, status: string): Prescription => ({
  id,
  farmId: 'farm-1',
  fieldId: 'f1',
  product: 'X',
  plannedDose: 1,
  unit: 'L/ha',
  status,
  createdAt: '2026-09-10T08:00:00Z',
  approvedAt: null,
})

const alert = (id: string, status: string, severity = 'WARNING'): Alert => ({
  id,
  farmId: 'farm-1',
  severity,
  type: 'T',
  title: 'A',
  message: '',
  entityType: null,
  entityId: null,
  status,
  createdAt: '2026-09-10T08:00:00Z',
})

const insight: AiInsight = {
  id: 'a1',
  type: 'YIELD_FORECAST',
  entityId: 'f1',
  score: 0.5,
  confidence: 0.2,
  horizonHours: null,
  model: 'm',
  modelVersion: '1',
  generatedAt: '2026-09-10T08:00:00Z',
  explanation: [],
  demo: false,
}

type Seed = {
  alerts?: Alert[]
  prescriptions?: Prescription[]
  irrigation?: IrrigationRecommendation[]
  insights?: AiInsight[]
}

/** Seeds every query the strip reads (farm = null → "all" keys). Unseeded queries stay pending forever. */
function renderStrip(seed: Seed, options: { seedAll?: boolean } = { seedAll: true }) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, queryFn: () => new Promise(() => undefined) } },
  })
  if (seed.alerts) client.setQueryData(['alerts', 'all'], seed.alerts)
  if (options.seedAll) {
    client.setQueryData(['agronomy', 'prescriptions', 'all'], seed.prescriptions ?? [])
    client.setQueryData(['agronomy', 'recommendations', 'all'], [])
    client.setQueryData(['irrigation', 'recommendations', 'all'], seed.irrigation ?? [])
    client.setQueryData(['ai', 'insights', 'all'], seed.insights ?? [])
    client.setQueryData(['fields', 'all'], [])
  }
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <TrustStrip farmId={null} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('TrustStrip', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('counts pending approvals from prescriptions + irrigation only (not approved, not low-confidence insights)', () => {
    renderStrip({
      alerts: [],
      prescriptions: [prescription('p1', 'DRAFT'), prescription('p2', 'APPROVED'), prescription('p3', 'REJECTED')],
      irrigation: [{ id: 'i1', status: 'PENDING' }, { id: 'i2', status: 'RECOMMENDED' }, { id: 'i3', status: 'EXECUTED' }],
      insights: [insight],
    })
    const pill = screen.getByRole('link', { name: '3 aprovações pendentes' })
    expect(pill).toHaveAttribute('href', '/decisions')
  })

  it('shows zero pending approvals when nothing is waiting', () => {
    renderStrip({ alerts: [] })
    expect(screen.getByRole('link', { name: '0 aprovações pendentes' })).toBeInTheDocument()
  })

  it('hides the approvals pill while the decision sources are still loading', () => {
    renderStrip({ alerts: [] }, { seedAll: false })
    expect(screen.queryByText(/aprovações pendentes/)).toBeNull()
    expect(screen.getAllByRole('link', { name: '0 alertas abertos' })).toHaveLength(2)
  })

  it('counts only OPEN alerts, links to /alerts and badges the bell', () => {
    renderStrip({ alerts: [alert('a', 'OPEN', 'CRITICAL'), alert('b', 'OPEN'), alert('c', 'ACKED', 'CRITICAL')] })
    // the pill (text) and the bell (aria-label) both announce the same count
    const links = screen.getAllByRole('link', { name: '2 alertas abertos' })
    expect(links).toHaveLength(2)
    for (const link of links) expect(link).toHaveAttribute('href', '/alerts')
    expect(screen.getByText('2', { selector: 'span' })).toBeInTheDocument()
  })

  it('does not render the bell badge when there are no open alerts', () => {
    renderStrip({ alerts: [alert('c', 'ACKED', 'CRITICAL')] })
    expect(screen.queryByText('0', { selector: 'span' })).toBeNull()
    expect(screen.getAllByRole('link', { name: '0 alertas abertos' })).toHaveLength(2)
  })

  it('hides the alerts pill until alerts have loaded (bell stays)', () => {
    renderStrip({})
    expect(screen.queryByText(/alertas abertos/)).toBeNull()
    expect(screen.getAllByRole('link', { name: '0 alertas abertos' })).toHaveLength(1)
  })

  it('reflects navigator.onLine', () => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false)
    renderStrip({ alerts: [] })
    expect(screen.getByRole('status')).toHaveTextContent('Offline')
  })

  it('shows online when the browser is online', () => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true)
    renderStrip({ alerts: [] })
    expect(screen.getByRole('status')).toHaveTextContent('Online')
  })
})
