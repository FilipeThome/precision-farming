export type AiInsight = {
  id: string
  type: string
  entityId: string
  score: number
  confidence: number
  horizonHours: number | null
  model: string
  modelVersion: string
  generatedAt: string
  explanation: string[]
  demo: boolean
}
