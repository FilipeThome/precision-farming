export type WeatherForecast = {
  id: string
  farmId: string
  forecastAt: string
  temperatureMin: number
  temperatureMax: number
  rainMm: number
  rainProbability: number
  windKmh: number
  humidityPct: number
  sprayingWindow: string
  vintage: string
}

export type WeatherWindow = {
  id: string
  farmId: string
  windowType: string
  startAt: string
  endAt: string
  rating: string
  notes: string | null
}

export type ParametricIndex = {
  farmId: string
  simulation?: boolean
  days: ParametricIndexDay[]
}

export type ParametricIndexDay = {
  date: string
  rainMm: number
  waterDeficitMm: number
  frostRisk: boolean | string | number
}

export type PlantingGate = {
  farmId: string
  municipality: string | null
  crop: string
  date: string
  decision: 'ALLOWED' | 'BLOCKED' | string
  reason: 'ZARC_OUT_OF_WINDOW' | 'SANITARY_VOID' | string | null
  simulation?: boolean
}
