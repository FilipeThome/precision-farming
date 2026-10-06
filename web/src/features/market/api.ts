import { apiGet } from '@/shared/api/client'
import type { MarketContract, MarketExposure, MarketQuote } from '@/features/market/types'

export async function fetchMarketQuotes(farmId?: string | null): Promise<MarketQuote[]> {
  return apiGet<MarketQuote[]>('/api/v1/market/quotes', { farmId: farmId ?? undefined })
}

export async function fetchMarketContracts(farmId?: string | null): Promise<MarketContract[]> {
  return apiGet<MarketContract[]>('/api/v1/market/contracts', { farmId: farmId ?? undefined })
}

export async function fetchMarketExposure(farmId?: string | null): Promise<MarketExposure[]> {
  return apiGet<MarketExposure[]>('/api/v1/market/exposure', { farmId: farmId ?? undefined })
}
