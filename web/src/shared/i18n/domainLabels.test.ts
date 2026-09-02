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

  it('resolves demo farm UUIDs to localized names', () => {
    expect(domainLabel('pt-BR', 'bbc017bc-be38-34d4-95df-0b1f15162e1d')).toBe('Fazenda Boa Vista')
    expect(domainLabel('en-US', 'bbc017bc-be38-34d4-95df-0b1f15162e1d')).toBe('Boa Vista Farm')
  })

  it('falls unknown UUIDs back to a short id instead of a generic word', () => {
    expect(domainLabel('en-US', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')).toBe('aaaaaaaa')
    expect(domainLabel('pt-BR', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb')).toBe('bbbbbbbb')
    expect(domainLabel('en-US', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')).not.toBe(
      domainLabel('en-US', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    )
  })

  it('keeps field-015 distinct from field-009', () => {
    expect(domainLabel('pt-BR', '4aacee0b-3a7c-3823-bd95-4f86bf857350')).toBe('Talhão Leste')
    expect(domainLabel('pt-BR', '9e876222-3f39-3190-ad8c-1c147b28f6e6')).toBe('Talhão Nordeste')
  })

  it('covers seeded maintenance and irrigation codes', () => {
    expect(domainLabel('pt-BR', 'FILTER_CHANGE')).toBe('Troca de filtros')
    expect(domainLabel('en-US', 'PIVOT_NORTH')).toBe('North Pivot')
    expect(domainLabel('pt-BR', 'WATER_DEFICIT')).toBe('Déficit hídrico estimado')
    expect(domainLabel('en-US', 'HYDRAULIC_OIL')).toBe('Hydraulic oil')
    expect(domainLabel('pt-BR', 'DRONE')).toBe('Drone')
    expect(domainLabel('pt-BR', '9861d50d-527b-385c-b89b-b0674467015f')).toBe('Drone 01')
    expect(domainLabel('pt-BR', '9b2296fa-d133-37db-be2f-be69dc802915')).toBe('Drone 02')
    expect(domainLabel('pt-BR', 'IDLE')).toBe('Ocioso')
    expect(domainLabel('en-US', 'DemoJohnDeere')).toBe('DemoJohnDeere')
    expect(domainLabel('en-US', 'listMachines')).toBe('listMachines')
    expect(domainLabel('en-US', 'getTelemetry')).toBe('getTelemetry')
    expect(domainLabel('en-US', 'ndviOverlay')).toBe('ndviOverlay')
  })
})
