import { ApiError } from '@/shared/api/client'

export function queryError(error: unknown): { message: string; correlationId?: string } {
  if (error instanceof ApiError) {
    return { message: error.message, correlationId: error.correlationId }
  }
  if (error instanceof Error) {
    return { message: error.message }
  }
  return { message: 'Erro inesperado' }
}
