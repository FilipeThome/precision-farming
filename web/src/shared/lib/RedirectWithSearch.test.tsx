import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation, useNavigationType } from 'react-router'
import { describe, expect, it } from 'vitest'

import { RedirectWithSearch } from '@/shared/lib/RedirectWithSearch'

function Probe() {
  const { pathname, search } = useLocation()
  return <span data-testid="loc">{pathname + search}</span>
}

describe('RedirectWithSearch', () => {
  it('keeps the query string when redirecting', () => {
    render(
      <MemoryRouter initialEntries={['/harvest?tab=storage&selected=lot-1&farm=f1']}>
        <Routes>
          <Route path="/harvest" element={<RedirectWithSearch to="/harvest/detail" />} />
          <Route path="/harvest/detail" element={<Probe />} />
        </Routes>
      </MemoryRouter>,
    )
    expect(screen.getByTestId('loc')).toHaveTextContent('/harvest/detail?tab=storage&selected=lot-1&farm=f1')
  })

  it('lands on the bare target when there is no query string', () => {
    render(
      <MemoryRouter initialEntries={['/harvest']}>
        <Routes>
          <Route path="/harvest" element={<RedirectWithSearch to="/harvest/detail" />} />
          <Route path="/harvest/detail" element={<Probe />} />
        </Routes>
      </MemoryRouter>,
    )
    expect(screen.getByTestId('loc')).toHaveTextContent(/^\/harvest\/detail$/)
  })

  it('replaces the history entry instead of pushing a new one', () => {
    function Type() {
      const type = useNavigationType()
      return <span data-testid="type">{type}</span>
    }
    render(
      <MemoryRouter initialEntries={['/old?farm=f1']}>
        <Routes>
          <Route path="/old" element={<RedirectWithSearch to="/new" />} />
          <Route path="/new" element={<Type />} />
        </Routes>
      </MemoryRouter>,
    )
    expect(screen.getByTestId('type')).toHaveTextContent('REPLACE')
  })
})
