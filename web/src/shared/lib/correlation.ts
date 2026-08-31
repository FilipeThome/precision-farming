export const CORRELATION_HEADER = 'X-Correlation-Id'

export function newCorrelationId(): string {
  return crypto.randomUUID()
}

export function newIdempotencyKey(): string {
  return crypto.randomUUID()
}
