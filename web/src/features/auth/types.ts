export type TokenResponse = {
  accessToken: string
  refreshToken: string
  tokenType?: string
  role: string
  userId: string
  name: string
  email: string
}

export type MeResponse = {
  id: string
  name: string
  email: string
  role: string
}
