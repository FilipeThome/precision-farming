import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'

import { FleetList, visibleFleet } from '@/features/dashboard/components/FleetList'
import type { Machine } from '@/shared/api/types'

const machine: Machine = {
  id: 'm1',
  farmId: 'farm-2',
  name: 'Tractor',
  type: 'TRACTOR',
  manufacturer: 'X',
  model: 'Y',
  status: 'OPERATING',
}

function renderWith(ui: ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('FleetList', () => {
  it('includes farm on machine links (inspectHref contract)', () => {
    renderWith(<FleetList machines={[machine]} farmId={null} isLoading={false} isError={false} onRetry={() => {}} />)
    const hrefs = screen.getAllByRole('link').map((el) => el.getAttribute('href'))
    expect(hrefs).toContain('/machines?selected=m1&farm=farm-2')
  })

  it('shows an empty state without machines', () => {
    renderWith(<FleetList machines={[]} farmId={null} isLoading={false} isError={false} onRetry={() => {}} />)
    expect(screen.getByRole('status')).toHaveTextContent('Nenhuma máquina')
  })

  it('caps the visible fleet at six, operating first', () => {
    const many = Array.from({ length: 8 }, (_, i) => ({
      ...machine,
      id: `m${i}`,
      status: i % 2 === 0 ? 'IDLE' : 'OPERATING',
    }))
    const visible = visibleFleet(many)
    expect(visible).toHaveLength(6)
    expect(visible[0].status).toBe('OPERATING')
  })
})
