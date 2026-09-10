import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { describe, expect, it } from 'vitest'

import { useSelectedId } from '@/shared/lib/useSelectedId'

function Probe() {
  const { selectedId, setSelectedId } = useSelectedId()
  const location = useLocation()
  return (
    <div>
      <span data-testid="id">{selectedId ?? 'none'}</span>
      <span data-testid="search">{location.search}</span>
      <button type="button" onClick={() => setSelectedId('abc')}>
        set
      </button>
      <button type="button" onClick={() => setSelectedId(null)}>
        clear
      </button>
    </div>
  )
}

describe('useSelectedId', () => {
  it('sets and clears selected while preserving other params', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/alerts?severity=CRITICAL&tab=plans']}>
        <Probe />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('id')).toHaveTextContent('none')
    expect(screen.getByTestId('search').textContent).toContain('severity=CRITICAL')
    expect(screen.getByTestId('search').textContent).toContain('tab=plans')

    await user.click(screen.getByRole('button', { name: 'set' }))
    expect(screen.getByTestId('id')).toHaveTextContent('abc')
    expect(screen.getByTestId('search').textContent).toContain('selected=abc')
    expect(screen.getByTestId('search').textContent).toContain('severity=CRITICAL')
    expect(screen.getByTestId('search').textContent).toContain('tab=plans')

    await user.click(screen.getByRole('button', { name: 'clear' }))
    expect(screen.getByTestId('id')).toHaveTextContent('none')
    expect(screen.getByTestId('search').textContent).not.toContain('selected=')
    expect(screen.getByTestId('search').textContent).toContain('severity=CRITICAL')
  })
})
