export type MarketQuote = {
  id: string
  commodity?: string
  price?: number
  currency?: string
  unit?: string
  quotedAt?: string
  market?: string
  exchange?: string
}

export type MarketContract = {
  id: string
  farmId?: string
  commodity?: string
  volumeTons?: number
  volumeT?: number
  price?: number
  currency?: string
  status?: string
  counterparty?: string
  deliveryAt?: string
}

export type MarketExposure = {
  id: string
  farmId?: string
  commodity?: string
  openT?: number
  hedgedT?: number
  riskScore?: number
  netTons?: number
  markToMarket?: number
  currency?: string
  asOf?: string
}
