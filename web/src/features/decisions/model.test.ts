import { describe, expect, it } from 'vitest'

import {
  decisionId,
  LOW_CONFIDENCE,
  needsHumanReview,
  normalizeStatus,
  parseDecisionId,
  toDecisionItems,
} from '@/features/decisions/model'
import type { AiInsight, Field, IrrigationRecommendation, Prescription } from '@/shared/api/types'

const field: Field = {
  id: 'field-1',
  farmId: 'farm-1',
  name: 'Talhão Norte',
  areaHa: 25.1,
  crop: 'SOY',
  variety: null,
  geometry: '',
}

const prescription: Prescription = {
  id: 'p1',
  farmId: 'farm-1',
  fieldId: 'field-1',
  product: 'GLYPHOSATE',
  rate: 2.5,
  unit: 'L/ha',
  status: 'DRAFT',
  createdAt: '2026-09-10T08:00:00Z',
  approvedAt: null,
}

const irrigation: IrrigationRecommendation = {
  id: 'i1',
  fieldId: 'field-1',
  volumeMm: 18,
  priority: 'HIGH',
  status: 'PENDING',
  recommendedAt: '2026-09-10T08:05:00Z',
  reason: 'WATER_DEFICIT',
}

const insight: AiInsight = {
  id: 'a1',
  type: 'YIELD_FORECAST',
  entityId: 'field-1',
  score: 0.7,
  confidence: 0.41,
  horizonHours: 72,
  model: 'yield-v1',
  modelVersion: '1.4',
  generatedAt: '2026-09-10T08:02:00Z',
  explanation: ['STABLE_NDVI', ''],
  demo: false,
}

describe('normalizeStatus', () => {
  it.each([
    ['DRAFT', 'PENDING'],
    ['recommended', 'PENDING'],
    ['APPROVED', 'APPROVED'],
    ['REJECTED', 'REJECTED'],
    ['APPLIED', 'EXECUTED'],
    [undefined, 'UNKNOWN'],
    ['WHATEVER', 'UNKNOWN'],
    // normalization: trims + upper-cases and accepts every alias in each family
    ['  draft ', 'PENDING'],
    ['open', 'PENDING'],
    ['Proposed', 'PENDING'],
    ['NEW', 'PENDING'],
    ['accepted', 'APPROVED'],
    ['dismissed', 'REJECTED'],
    ['CANCELLED', 'REJECTED'],
    ['canceled', 'REJECTED'],
    ['done', 'EXECUTED'],
    ['completed', 'EXECUTED'],
    ['EXECUTED', 'EXECUTED'],
    [null, 'UNKNOWN'],
    ['', 'UNKNOWN'],
    ['   ', 'UNKNOWN'],
  ])('%s → %s', (raw, expected) => {
    expect(normalizeStatus(raw)).toBe(expected)
  })
})

describe('toDecisionItems – all-optional DTOs and unresolved ids', () => {
  it('agronomy recommendation with only an id yields a safe UNKNOWN item', () => {
    const [item] = toDecisionItems({ agronomy: [{ id: 'r1' }] })
    expect(item).toMatchObject({
      id: 'AGRONOMY:r1',
      rawId: 'r1',
      source: 'AGRONOMY',
      title: 'RECOMMENDATION',
      status: 'UNKNOWN',
      capabilities: { approve: false, simulate: false },
    })
    expect(item.fieldId).toBeUndefined()
    expect(item.farmId).toBeUndefined()
    expect(item.rawStatus).toBeUndefined()
    expect(item.createdAt).toBeUndefined()
    expect(item.priority).toBeUndefined()
    expect(item.summary).toBeUndefined()
  })

  it('agronomy title falls back to kind before the generic code', () => {
    const [byKind] = toDecisionItems({ agronomy: [{ id: 'r1', kind: 'FERTILIZATION' }] })
    expect(byKind.title).toBe('FERTILIZATION')
    const [byTitle] = toDecisionItems({ agronomy: [{ id: 'r1', kind: 'FERTILIZATION', title: 'Apply N' }] })
    expect(byTitle.title).toBe('Apply N')
  })

  it('keeps fieldId but not farmId when the field is not loaded', () => {
    const [agro] = toDecisionItems({ agronomy: [{ id: 'r1', fieldId: 'f-unknown' }], fields: [field] })
    expect(agro.fieldId).toBe('f-unknown')
    expect(agro.farmId).toBeUndefined()

    const [irr] = toDecisionItems({ irrigation: [{ id: 'i1', fieldId: 'f-unknown' }], fields: [] })
    expect(irr.fieldId).toBe('f-unknown')
    expect(irr.farmId).toBeUndefined()
  })

  it('prefers the DTO farmId over the one resolved through the field', () => {
    const [item] = toDecisionItems({ agronomy: [{ id: 'r1', fieldId: 'field-1', farmId: 'farm-explicit' }], fields: [field] })
    expect(item.farmId).toBe('farm-explicit')
  })

  it('treats empty strings as missing values', () => {
    const [p] = toDecisionItems({
      prescriptions: [{ ...prescription, fieldId: '', farmId: '', status: '', createdAt: '', approvedAt: '' }],
    })
    expect(p.fieldId).toBeUndefined()
    expect(p.farmId).toBeUndefined()
    expect(p.rawStatus).toBeUndefined()
    expect(p.status).toBe('UNKNOWN')
    expect(p.createdAt).toBeUndefined()
    expect(p.approvedAt).toBeUndefined()
    expect(p.capabilities.approve).toBe(false)

    const [a] = toDecisionItems({ insights: [{ ...insight, entityId: '', model: '', modelVersion: '' }] })
    expect(a.entityId).toBeUndefined()
    expect(a.model).toBeUndefined()
    expect(a.modelVersion).toBeUndefined()
  })

  it('prescription farmId falls back to the field when the DTO omits it', () => {
    const [item] = toDecisionItems({ prescriptions: [{ ...prescription, farmId: '' }], fields: [field] })
    expect(item.farmId).toBe('farm-1')
  })

  it('drops non-finite quantities and confidences', () => {
    const [p] = toDecisionItems({ prescriptions: [{ ...prescription, rate: Number.NaN }] })
    expect(p.quantity).toBeUndefined()
    const [i] = toDecisionItems({ irrigation: [{ id: 'i1', volumeMm: Number.POSITIVE_INFINITY }] })
    expect(i.quantity).toBeUndefined()
    const [a] = toDecisionItems({
      insights: [{ ...insight, confidence: Number.NaN, score: undefined as unknown as number }],
    })
    expect(a.confidence).toBeUndefined()
    expect(a.score).toBeUndefined()
    expect(needsHumanReview(a)).toBe(false)
  })

  it('tolerates a malformed explanation payload', () => {
    const [noArray] = toDecisionItems({ insights: [{ ...insight, explanation: 'text' as unknown as string[] }] })
    expect(noArray.explanation).toBeUndefined()
    const [mixed] = toDecisionItems({ insights: [{ ...insight, explanation: ['A', 3, null, ''] as unknown as string[] }] })
    expect(mixed.explanation).toEqual(['A'])
  })

  it('AI insights are never pending and carry no approve/simulate capability', () => {
    const [item] = toDecisionItems({ insights: [insight] })
    expect(item.status).toBe('UNKNOWN')
    expect(item.rawStatus).toBeUndefined()
    expect(item.approvedAt).toBeUndefined()
    expect(item.capabilities).toEqual({ approve: false, simulate: false })
  })

  it('irrigation only offers simulate while pending', () => {
    const [done] = toDecisionItems({ irrigation: [{ ...irrigation, status: 'EXECUTED' }] })
    expect(done.capabilities.simulate).toBe(false)
    const [rejected] = toDecisionItems({ irrigation: [{ ...irrigation, status: 'REJECTED' }] })
    expect(rejected.capabilities.simulate).toBe(false)
  })

  it('emits sources in a stable order and keeps ids unique across sources sharing a raw id', () => {
    const items = toDecisionItems({
      prescriptions: [{ ...prescription, id: 'same' }],
      irrigation: [{ id: 'same' }],
      agronomy: [{ id: 'same' }],
      insights: [{ ...insight, id: 'same' }],
    })
    expect(items.map((i) => i.source)).toEqual(['PRESCRIPTION', 'IRRIGATION', 'AGRONOMY', 'AI_INSIGHT'])
    expect(new Set(items.map((i) => i.id)).size).toBe(4)
  })
})

describe('needsHumanReview', () => {
  it('is strictly below the threshold and false without a confidence', () => {
    const base = toDecisionItems({ insights: [insight] })[0]
    expect(needsHumanReview({ ...base, confidence: LOW_CONFIDENCE })).toBe(false)
    expect(needsHumanReview({ ...base, confidence: LOW_CONFIDENCE - 0.01 })).toBe(true)
    expect(needsHumanReview({ ...base, confidence: 0 })).toBe(true)
    expect(needsHumanReview({ ...base, confidence: undefined })).toBe(false)
  })
})

describe('toDecisionItems', () => {
  it('adapts prescriptions with approve capability while pending', () => {
    const [item] = toDecisionItems({ prescriptions: [prescription], fields: [field] })
    expect(item.id).toBe('PRESCRIPTION:p1')
    expect(item.status).toBe('PENDING')
    expect(item.rawStatus).toBe('DRAFT')
    expect(item.quantity).toEqual({ value: 2.5, unit: 'L/ha' })
    expect(item.capabilities).toEqual({ approve: true, simulate: false })
    expect(item.approvedAt).toBeUndefined()
  })

  it('drops approve capability once approved', () => {
    const [item] = toDecisionItems({
      prescriptions: [{ ...prescription, status: 'APPROVED', approvedAt: '2026-09-10T09:00:00Z' }],
    })
    expect(item.status).toBe('APPROVED')
    expect(item.capabilities.approve).toBe(false)
    expect(item.approvedAt).toBe('2026-09-10T09:00:00Z')
  })

  it('resolves farm through the field for all-optional DTOs', () => {
    const [item] = toDecisionItems({ irrigation: [irrigation], fields: [field] })
    expect(item.source).toBe('IRRIGATION')
    expect(item.farmId).toBe('farm-1')
    expect(item.quantity).toEqual({ value: 18, unit: 'mm' })
    expect(item.capabilities.simulate).toBe(true)
    expect(item.priority).toBe('HIGH')
  })

  it('keeps createdAt undefined when the DTO has no timestamp', () => {
    const [item] = toDecisionItems({ irrigation: [{ id: 'i2' }] })
    expect(item.createdAt).toBeUndefined()
    expect(item.status).toBe('UNKNOWN')
    expect(item.fieldId).toBeUndefined()
  })

  it('resolves AI insight entityId only when it matches a loaded field', () => {
    const [resolved] = toDecisionItems({ insights: [insight], fields: [field] })
    expect(resolved.fieldId).toBe('field-1')
    expect(resolved.farmId).toBe('farm-1')
    expect(resolved.confidence).toBe(0.41)
    expect(resolved.explanation).toEqual(['STABLE_NDVI'])
    expect(needsHumanReview(resolved)).toBe(true)

    const [unresolved] = toDecisionItems({ insights: [{ ...insight, entityId: 'machine-9' }], fields: [field] })
    expect(unresolved.fieldId).toBeUndefined()
    expect(unresolved.entityId).toBe('machine-9')
  })

  it('returns an empty list for empty sources', () => {
    expect(toDecisionItems({})).toEqual([])
  })
})

describe('parseDecisionId', () => {
  it('round-trips composite ids', () => {
    expect(parseDecisionId('PRESCRIPTION:p1')).toEqual({ source: 'PRESCRIPTION', rawId: 'p1' })
    expect(parseDecisionId('bogus')).toBeNull()
    expect(parseDecisionId('NOPE:x')).toBeNull()
  })

  it('splits on the first colon only so raw ids may contain colons', () => {
    const id = decisionId('AI_INSIGHT', 'urn:a:b')
    expect(id).toBe('AI_INSIGHT:urn:a:b')
    expect(parseDecisionId(id)).toEqual({ source: 'AI_INSIGHT', rawId: 'urn:a:b' })
  })

  it('rejects ids with an empty source and tolerates an empty raw id', () => {
    expect(parseDecisionId(':x')).toBeNull()
    expect(parseDecisionId('')).toBeNull()
    expect(parseDecisionId('IRRIGATION:')).toEqual({ source: 'IRRIGATION', rawId: '' })
  })

  it('round-trips every item produced by the adapter', () => {
    const items = toDecisionItems({ prescriptions: [prescription], irrigation: [irrigation], agronomy: [{ id: 'r1' }], insights: [insight] })
    for (const item of items) {
      expect(parseDecisionId(item.id)).toEqual({ source: item.source, rawId: item.rawId })
    }
  })
})
