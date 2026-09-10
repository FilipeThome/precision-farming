import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { Tractor } from 'lucide-react'
import { describe, expect, it } from 'vitest'

import { KpiCard } from '@/shared/ui/KpiCard'

describe('KpiCard', () => {
  it('renders a link when to is set', () => {
    render(
      <MemoryRouter>
        <KpiCard label="Alertas" value={4} icon={Tractor} to="/alerts" />
      </MemoryRouter>,
    )
    expect(screen.getByRole('link', { name: 'Alertas: 4' })).toHaveAttribute('href', '/alerts')
  })

  it('does not render a link without to', () => {
    render(<KpiCard label="Alertas" value={4} icon={Tractor} />)
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
    expect(screen.getByText('Alertas')).toBeInTheDocument()
  })
})
