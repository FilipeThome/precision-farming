import { describe, expect, it } from 'vitest'

import { enUS } from '@/shared/i18n/dictionaries/en-US'
import { ptBR } from '@/shared/i18n/dictionaries/pt-BR'

const placeholders = (template: string) => [...template.matchAll(/\{(\w+)\}/g)].map((m) => m[1]).sort()

describe('i18n dictionaries', () => {
  const ptKeys = Object.keys(ptBR).sort()
  const enKeys = Object.keys(enUS).sort()

  it('pt-BR and en-US have identical key sets', () => {
    expect(enKeys).toEqual(ptKeys)
  })

  it('has no empty translations in either locale', () => {
    const emptyPt = ptKeys.filter((k) => (ptBR as Record<string, string>)[k].trim() === '')
    const emptyEn = enKeys.filter((k) => (enUS as Record<string, string>)[k].trim() === '')
    expect(emptyPt).toEqual([])
    expect(emptyEn).toEqual([])
  })

  it('uses the same interpolation placeholders per key', () => {
    const mismatched = ptKeys.filter(
      (k) => placeholders((ptBR as Record<string, string>)[k]).join(',') !== placeholders((enUS as Record<string, string>)[k]).join(','),
    )
    expect(mismatched).toEqual([])
  })

  it('carries the Terra redesign keys used by the shell, trust strip and decisions', () => {
    for (const key of [
      'domain.tower',
      'domain.decisions',
      'trust.online',
      'trust.offline',
      'trust.openAlerts',
      'trust.pendingApprovals',
      'tabs.decisionQueue',
      'tabs.harvestTower',
      'tabs.harvestDetail',
      'decisions.linkedOp.inferred',
      'reports.catalogFallback',
    ] as const) {
      expect(ptBR[key], key).toBeTruthy()
      expect(enUS[key], key).toBeTruthy()
    }
  })

  it('includes form validation keys in both locales', () => {
    for (const key of [
      'form.validation.fieldNotInFarm',
      'form.validation.machineNotInFarm',
      'form.validation.plannedWindow',
      'form.validation.plannedOrder',
    ] as const) {
      expect(ptBR[key], key).toBeTruthy()
      expect(enUS[key], key).toBeTruthy()
    }
  })

  it('no longer ships demo/seed copy', () => {
    const all = [...Object.values(ptBR), ...Object.values(enUS)].join('\n')
    expect(all).not.toMatch(/precisionfarming\.demo/i)
    expect(all).not.toMatch(/Precision@123/)
  })
})
