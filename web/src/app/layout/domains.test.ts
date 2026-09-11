import { describe, expect, it } from 'vitest'

import { activeDomainId, ADMIN_DOMAIN, DOMAINS, domainTabs } from '@/app/layout/domains'
import { enUS } from '@/shared/i18n/dictionaries/en-US'
import { ptBR } from '@/shared/i18n/dictionaries/pt-BR'

describe('domains', () => {
  it('has nine domains', () => {
    expect(DOMAINS).toHaveLength(9)
  })

  it('lights the right domain for untouched pages', () => {
    expect(activeDomainId('/dashboard')).toBe('tower')
    expect(activeDomainId('/fields')).toBe('farms')
    expect(activeDomainId('/machines')).toBe('operations')
    expect(activeDomainId('/agronomy')).toBe('decisions')
    expect(activeDomainId('/decisions/PRESCRIPTION:abc')).toBe('decisions')
    expect(activeDomainId('/reports')).toBe('data')
    expect(activeDomainId('/harvest/detail')).toBe('harvest')
    expect(activeDomainId('/compliance/lots/LOT-1')).toBe('esg')
    expect(activeDomainId('/settings')).toBe('admin')
    expect(activeDomainId('/nowhere')).toBeNull()
  })

  it('exposes the sub-tabs of the active domain', () => {
    expect(domainTabs('/fields')?.map((t) => t.to)).toEqual(['/farms', '/fields', '/seasons'])
    expect(domainTabs('/map')).toBeUndefined()
  })

  it('matches deeply nested routes only where a splat is declared', () => {
    expect(activeDomainId('/decisions/AI_INSIGHT:x/anything/deeper')).toBe('decisions')
    expect(activeDomainId('/harvest/detail/extra')).toBe('harvest')
    expect(activeDomainId('/compliance/esg')).toBe('esg')
    // no splat on these: nested paths do not light the domain
    expect(activeDomainId('/settings/users')).toBeNull()
    expect(activeDomainId('/fields/abc')).toBeNull()
    expect(activeDomainId('/alerts/123')).toBeNull()
  })

  it('does not confuse prefixes, tolerates trailing slashes and ignores the root', () => {
    expect(activeDomainId('/fieldsx')).toBeNull()
    expect(activeDomainId('/farms/')).toBe('farms')
    expect(activeDomainId('/harvest/')).toBe('harvest')
    expect(activeDomainId('/')).toBeNull()
    expect(activeDomainId('')).toBeNull()
  })

  it('returns the tabs of nested routes and none for admin', () => {
    expect(domainTabs('/decisions/PRESCRIPTION:p1')?.map((t) => t.to)).toEqual(['/decisions', '/agronomy', '/irrigation', '/weather', '/ai'])
    expect(domainTabs('/harvest/detail')?.map((t) => t.to)).toEqual(['/harvest', '/harvest/detail'])
    expect(domainTabs('/settings')).toBeUndefined()
    expect(domainTabs('/nowhere')).toBeUndefined()
  })

  it('every domain entry point and tab lights its own domain (config consistency)', () => {
    for (const domain of [...DOMAINS, ADMIN_DOMAIN]) {
      expect(activeDomainId(domain.to), domain.id).toBe(domain.id)
      for (const tab of domain.tabs ?? []) {
        expect(activeDomainId(tab.to), `${domain.id} tab ${tab.to}`).toBe(domain.id)
      }
    }
  })

  it('has unique ids and route patterns across domains', () => {
    const all = [...DOMAINS, ADMIN_DOMAIN]
    expect(new Set(all.map((d) => d.id)).size).toBe(all.length)
    const patterns = all.flatMap((d) => d.routes)
    expect(new Set(patterns).size).toBe(patterns.length)
  })

  it('all labels exist in both dictionaries', () => {
    const all = [...DOMAINS, ADMIN_DOMAIN]
    const keys = all.flatMap((d) => [d.labelKey, ...(d.tabs ?? []).map((t) => t.labelKey)])
    for (const key of keys) {
      expect(ptBR[key], key).toBeTruthy()
      expect(enUS[key], key).toBeTruthy()
    }
  })
})
