import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, useSearchParams } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'

import { farmHref, inspectHref, useFarmFromSearch } from '@/shared/lib/useFarmFromSearch'
import { useUiStore } from '@/shared/ui/uiStore'

function Probe() {
  useFarmFromSearch()
  const farmId = useUiStore((s) => s.farmId)
  const [params] = useSearchParams()
  return (
    <>
      <span data-testid="farm">{farmId ?? 'none'}</span>
      <span data-testid="url-farm">{params.get('farm') ?? 'none'}</span>
    </>
  )
}

describe('useFarmFromSearch', () => {
  afterEach(() => {
    useUiStore.getState().setFarmId(null)
  })

  it('applies farm on the first render over a persisted filter', () => {
    useUiStore.getState().setFarmId('farm-old')
    render(
      <MemoryRouter initialEntries={['/operations?selected=op-1&farm=farm-9']}>
        <Probe />
      </MemoryRouter>,
    )
    expect(screen.getByTestId('farm')).toHaveTextContent('farm-9')
  })

  it('writes the store farm into the URL when the query has none', async () => {
    useUiStore.getState().setFarmId('farm-store')
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Probe />
      </MemoryRouter>,
    )
    expect(screen.getByTestId('farm')).toHaveTextContent('farm-store')
    await waitFor(() => {
      expect(screen.getByTestId('url-farm')).toHaveTextContent('farm-store')
    })
  })

  it('builds inspector hrefs with farm', () => {
    expect(inspectHref('/alerts', 'a1', 'f1')).toBe('/alerts?selected=a1&farm=f1')
    expect(inspectHref('/machines', 'm1')).toBe('/machines?selected=m1')
    expect(farmHref('/machines', 'f1')).toBe('/machines?farm=f1')
    expect(farmHref('/machines', null)).toBe('/machines')
  })
})
