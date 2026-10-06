export type Alert = {
  id: string
  farmId: string
  severity: string
  type: string
  title: string
  message: string
  entityType: string | null
  entityId: string | null
  status: string
  createdAt: string
}
