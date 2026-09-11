import { describe, expect, it } from 'vitest'

import { domainLabel, humanizeCode } from '@/shared/i18n/domainLabels'

describe('domain labels', () => {
  it('translates snake_case codes by locale', () => {
    expect(domainLabel('pt-BR', 'IN_PROGRESS')).toBe('Em andamento')
    expect(domainLabel('en-US', 'IN_PROGRESS')).toBe('In progress')
    expect(domainLabel('pt-BR', 'PESTICIDE')).toBe('Defensivo')
    expect(domainLabel('en-US', 'PESTICIDE')).toBe('Pesticide')
  })

  it('humanizes unknown SCREAMING_SNAKE codes', () => {
    expect(humanizeCode('TEXT_EXAMPLE', 'en-US')).toBe('Text example')
    expect(domainLabel('en-US', 'TEXT_EXAMPLE')).toBe('Text example')
    expect(domainLabel('pt-BR', 'TEXT_EXAMPLE')).toBe('Text example')
  })

  it('translates logistics and inventory codes', () => {
    expect(domainLabel('pt-BR', 'CENTRAL_WAREHOUSE')).toBe('Armazém Central')
    expect(domainLabel('en-US', 'CENTRAL_WAREHOUSE')).toBe('Central Warehouse')
    expect(domainLabel('en-US', 'Armazém Central')).toBe('Central Warehouse')
    expect(domainLabel('en-US', 'Glifosato')).toBe('Glyphosate')
    expect(domainLabel('pt-BR', 'GLYPHOSATE')).toBe('Glifosato')
  })

  it('maps Portuguese aliases when switching to English', () => {
    expect(domainLabel('en-US', 'Soja')).toBe('Soy')
    expect(domainLabel('en-US', 'Plantio')).toBe('Planting')
    expect(domainLabel('pt-BR', 'SOY')).toBe('Soja')
  })

  it('never invents names for uuids: falls back to a short, distinct id', () => {
    expect(domainLabel('pt-BR', 'bbc017bc-be38-34d4-95df-0b1f15162e1d')).toBe('bbc017bc')
    expect(domainLabel('en-US', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')).toBe('aaaaaaaa')
    expect(domainLabel('pt-BR', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb')).toBe('bbbbbbbb')
    expect(domainLabel('en-US', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')).not.toBe(
      domainLabel('en-US', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    )
  })

  it('covers maintenance and irrigation codes', () => {
    expect(domainLabel('pt-BR', 'FILTER_CHANGE')).toBe('Troca de filtros')
    expect(domainLabel('en-US', 'PIVOT_NORTH')).toBe('North Pivot')
    expect(domainLabel('pt-BR', 'WATER_DEFICIT')).toBe('Déficit hídrico estimado')
    expect(domainLabel('en-US', 'HYDRAULIC_OIL')).toBe('Hydraulic oil')
    expect(domainLabel('pt-BR', 'DRONE')).toBe('Drone')
    expect(domainLabel('pt-BR', 'IDLE')).toBe('Ocioso')
    expect(domainLabel('en-US', 'DemoJohnDeere')).toBe('DemoJohnDeere')
    expect(domainLabel('en-US', 'listMachines')).toBe('listMachines')
    expect(domainLabel('en-US', 'getTelemetry')).toBe('getTelemetry')
    expect(domainLabel('en-US', 'ndviOverlay')).toBe('ndviOverlay')
  })
})
