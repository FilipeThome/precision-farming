import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'

import { inspectHref, useFarmFromSearch } from '@/shared/lib/useFarmFromSearch'
import { useUiStore } from '@/shared/ui/uiStore'

function Probe() {
  useFarmFromSearch()
  const farmId = useUiStore((s) => s.farmId)
  return <span data-testid="farm">{farmId ?? 'none'}</span>
}

describe('useFarmFromSearch', () => {
  afterEach(() => {
    useUiStore.getState().setFarmId(null)
  })

  it('copies farm query into the UI store', () => {
    render(
      <MemoryRouter initialEntries={['/operations?selected=op-1&farm=farm-9']}>
        <Probe />
      </MemoryRouter>,
    )
    expect(screen.getByTestId('farm')).toHaveTextContent('farm-9')
  })

  it('builds inspector hrefs with farm', () => {
    expect(inspectHref('/alerts', 'a1', 'f1')).toBe('/alerts?selected=a1&farm=f1')
    expect(inspectHref('/machines', 'm1')).toBe('/machines?selected=m1')
  })
})
