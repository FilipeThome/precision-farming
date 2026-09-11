import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { ChainRail } from '@/shared/ui/ChainRail'

describe('ChainRail', () => {
  it('renders every step with its state and marks the current one', () => {
    render(
      <ChainRail
        aria-label="cadeia"
        steps={[
          { id: 'signal', label: 'Sinal', state: 'done' },
          { id: 'approval', label: 'Aprovação', state: 'now' },
          { id: 'order', label: 'Ordem', state: 'pending' },
          { id: 'exec', label: 'Execução', state: 'blocked' },
        ]}
      />,
    )
    const items = screen.getAllByRole('listitem')
    expect(items).toHaveLength(4)
    expect(items[0]).toHaveAttribute('data-state', 'done')
    expect(items[1]).toHaveAttribute('aria-current', 'step')
    expect(items[3]).toHaveAttribute('data-state', 'blocked')
    expect(screen.getByRole('list', { name: 'cadeia' })).toBeInTheDocument()
  })
})
