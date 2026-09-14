import type {
  AgronomyRecommendation,
  AiInsight,
  Field,
  IrrigationRecommendation,
  Prescription,
} from '@/shared/api/types'

export type DecisionSource = 'PRESCRIPTION' | 'IRRIGATION' | 'AI_INSIGHT' | 'AGRONOMY'
export type DecisionStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXECUTED' | 'UNKNOWN'

/** Pure, serializable projection of any "decision-like" record returned by the gateway. */
export type DecisionItem = {
  /** Stable composite id: `${source}:${rawId}` (safe as a route param). */
  id: string
  rawId: string
  source: DecisionSource
  /** Raw title code/text; components localize through `label()`. */
  title: string
  /** Optional quantity attached to the decision (rate, volume…). */
  quantity?: { value: number; unit: string }
  fieldId?: string
  farmId?: string
  /** Raw entity id for AI insights that could not be resolved to a field. */
  entityId?: string
  status: DecisionStatus
  rawStatus?: string
  createdAt?: string
  approvedAt?: string
  confidence?: number
  score?: number
  explanation?: string[]
  summary?: string
  priority?: string
  model?: string
  modelVersion?: string
  capabilities: { approve: boolean; simulate: boolean }
}

export type DecisionSources = {
  prescriptions?: Prescription[]
  irrigation?: IrrigationRecommendation[]
  agronomy?: AgronomyRecommendation[]
  insights?: AiInsight[]
  fields?: Field[]
}

const PENDING = new Set(['DRAFT', 'PENDING', 'RECOMMENDED', 'OPEN', 'PROPOSED', 'NEW'])
const APPROVED = new Set(['APPROVED', 'ACCEPTED'])
const REJECTED = new Set(['REJECTED', 'DISMISSED', 'CANCELLED', 'CANCELED'])
const EXECUTED = new Set(['EXECUTED', 'APPLIED', 'DONE', 'COMPLETED'])

export function normalizeStatus(raw?: string | null): DecisionStatus {
  if (!raw) return 'UNKNOWN'
  const value = raw.trim().toUpperCase()
  if (PENDING.has(value)) return 'PENDING'
  if (APPROVED.has(value)) return 'APPROVED'
  if (REJECTED.has(value)) return 'REJECTED'
  if (EXECUTED.has(value)) return 'EXECUTED'
  return 'UNKNOWN'
}

export function decisionId(source: DecisionSource, rawId: string): string {
  return `${source}:${rawId}`
}

export function parseDecisionId(id: string): { source: DecisionSource; rawId: string } | null {
  const idx = id.indexOf(':')
  if (idx <= 0) return null
  const source = id.slice(0, idx) as DecisionSource
  if (!['PRESCRIPTION', 'IRRIGATION', 'AI_INSIGHT', 'AGRONOMY'].includes(source)) return null
  return { source, rawId: id.slice(idx + 1) }
}

function fieldIndex(fields: Field[] | undefined): Map<string, Field> {
  return new Map((fields ?? []).map((f) => [f.id, f]))
}

function optional(value: string | null | undefined): string | undefined {
  return value == null || value === '' ? undefined : value
}

function fromPrescription(p: Prescription, fields: Map<string, Field>): DecisionItem {
  const field = fields.get(p.fieldId)
  return {
    id: decisionId('PRESCRIPTION', p.id),
    rawId: p.id,
    source: 'PRESCRIPTION',
    title: p.product,
    quantity: Number.isFinite(p.rate) ? { value: p.rate, unit: p.unit } : undefined,
    fieldId: optional(p.fieldId),
    farmId: optional(p.farmId) ?? field?.farmId,
    status: normalizeStatus(p.status),
    rawStatus: optional(p.status),
    createdAt: optional(p.createdAt),
    approvedAt: optional(p.approvedAt),
    capabilities: { approve: normalizeStatus(p.status) === 'PENDING', simulate: false },
  }
}

function fromIrrigation(r: IrrigationRecommendation, fields: Map<string, Field>): DecisionItem {
  const field = r.fieldId ? fields.get(r.fieldId) : undefined
  return {
    id: decisionId('IRRIGATION', r.id),
    rawId: r.id,
    source: 'IRRIGATION',
    title: r.reason ?? 'IRRIGATION',
    quantity: r.volumeMm != null && Number.isFinite(r.volumeMm) ? { value: r.volumeMm, unit: 'mm' } : undefined,
    fieldId: optional(r.fieldId),
    farmId: optional(r.farmId) ?? field?.farmId,
    status: normalizeStatus(r.status),
    rawStatus: optional(r.status),
    createdAt: optional(r.recommendedAt),
    priority: optional(r.priority),
    summary: optional(r.reason),
    capabilities: { approve: false, simulate: normalizeStatus(r.status) === 'PENDING' },
  }
}

function fromAgronomy(r: AgronomyRecommendation, fields: Map<string, Field>): DecisionItem {
  const field = r.fieldId ? fields.get(r.fieldId) : undefined
  return {
    id: decisionId('AGRONOMY', r.id),
    rawId: r.id,
    source: 'AGRONOMY',
    title: r.title ?? r.kind ?? 'RECOMMENDATION',
    fieldId: optional(r.fieldId),
    farmId: optional(r.farmId) ?? field?.farmId,
    status: normalizeStatus(r.status),
    rawStatus: optional(r.status),
    createdAt: optional(r.createdAt),
    priority: optional(r.priority),
    summary: optional(r.summary),
    capabilities: { approve: false, simulate: false },
  }
}

function fromInsight(i: AiInsight, fields: Map<string, Field>): DecisionItem {
  // AiInsight only has `entityId`; resolve to a field when it matches a loaded field.
  const field = fields.get(i.entityId)
  return {
    id: decisionId('AI_INSIGHT', i.id),
    rawId: i.id,
    source: 'AI_INSIGHT',
    title: i.type,
    fieldId: field?.id,
    farmId: field?.farmId,
    entityId: optional(i.entityId),
    status: 'UNKNOWN',
    createdAt: optional(i.generatedAt),
    confidence: typeof i.confidence === 'number' && Number.isFinite(i.confidence) ? i.confidence : undefined,
    score: typeof i.score === 'number' && Number.isFinite(i.score) ? i.score : undefined,
    explanation: Array.isArray(i.explanation) ? i.explanation.filter((line) => typeof line === 'string' && line !== '') : undefined,
    model: optional(i.model),
    modelVersion: optional(i.modelVersion),
    capabilities: { approve: false, simulate: false },
  }
}

/** Adapts every decision-like record into a unified list. Pure: no hooks, no mutations. */
export function toDecisionItems(sources: DecisionSources): DecisionItem[] {
  const fields = fieldIndex(sources.fields)
  return [
    ...(sources.prescriptions ?? []).map((p) => fromPrescription(p, fields)),
    ...(sources.irrigation ?? []).map((r) => fromIrrigation(r, fields)),
    ...(sources.agronomy ?? []).map((r) => fromAgronomy(r, fields)),
    ...(sources.insights ?? []).map((i) => fromInsight(i, fields)),
  ]
}

export const LOW_CONFIDENCE = 0.6

export function needsHumanReview(item: DecisionItem): boolean {
  return item.confidence != null && item.confidence < LOW_CONFIDENCE
}
