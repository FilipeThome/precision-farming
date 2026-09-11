import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'

import { preserveFarmSearch, RouteTabs } from '@/shared/ui/RouteTabs'

describe('RouteTabs', () => {
  it('keeps only the farm param', () => {
    expect(preserveFarmSearch('?farm=f1&selected=x')).toBe('?farm=f1')
    expect(preserveFarmSearch('?selected=x')).toBe('')
  })

  it('handles empty, blank-farm and encoded values', () => {
    expect(preserveFarmSearch('')).toBe('')
    expect(preserveFarmSearch('?farm=')).toBe('')
    expect(preserveFarmSearch('?tab=storage&farm=a%20b')).toBe('?farm=a%20b')
    expect(preserveFarmSearch('farm=f1')).toBe('?farm=f1')
  })

  it('links carry no query string when the farm is not set', () => {
    render(
      <MemoryRouter initialEntries={['/fields?selected=a&tab=x']}>
        <RouteTabs tabs={[{ to: '/farms', labelKey: 'nav.farms' }, { to: '/fields', labelKey: 'nav.fields' }]} />
      </MemoryRouter>,
    )
    expect(screen.getByRole('link', { name: 'Fazendas' })).toHaveAttribute('href', '/farms')
    expect(screen.getByRole('link', { name: 'Talhões' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Fazendas' })).not.toHaveAttribute('aria-current')
    expect(screen.getByRole('navigation')).toHaveAttribute('aria-label')
  })

  it('tabs match exactly by default; end=false also lights nested routes', () => {
    render(
      <MemoryRouter initialEntries={['/decisions/PRESCRIPTION:abc?farm=f1']}>
        <RouteTabs
          tabs={[
            { to: '/decisions', labelKey: 'tabs.decisionQueue', end: false },
            { to: '/agronomy', labelKey: 'nav.agronomy' },
          ]}
        />
      </MemoryRouter>,
    )
    const queue = screen.getByRole('link', { name: 'Fila de decisões' })
    expect(queue).toHaveAttribute('aria-current', 'page')
    expect(queue).toHaveAttribute('href', '/decisions?farm=f1')
    expect(screen.getByRole('link', { name: 'Agronomia' })).not.toHaveAttribute('aria-current')
  })

  it('the tower tab is not active on the nested detail route', () => {
    render(
      <MemoryRouter initialEntries={['/harvest/detail?farm=f1&tab=storage']}>
        <RouteTabs
          tabs={[
            { to: '/harvest', labelKey: 'tabs.harvestTower' },
            { to: '/harvest/detail', labelKey: 'tabs.harvestDetail' },
          ]}
        />
      </MemoryRouter>,
    )
    const links = screen.getAllByRole('link')
    expect(links[0]).not.toHaveAttribute('aria-current')
    expect(links[0]).toHaveAttribute('href', '/harvest?farm=f1')
    expect(links[1]).toHaveAttribute('aria-current', 'page')
    expect(links[1]).toHaveAttribute('href', '/harvest/detail?farm=f1')
  })

  it('links preserve farm and mark the active route', () => {
    render(
      <MemoryRouter initialEntries={['/farms?farm=f1&selected=a']}>
        <RouteTabs
          tabs={[
            { to: '/farms', labelKey: 'nav.farms' },
            { to: '/fields', labelKey: 'nav.fields' },
          ]}
        />
      </MemoryRouter>,
    )
    const farms = screen.getByRole('link', { name: 'Fazendas' })
    expect(farms).toHaveAttribute('href', '/farms?farm=f1')
    expect(farms).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Talhões' })).toHaveAttribute('href', '/fields?farm=f1')
  })
})
