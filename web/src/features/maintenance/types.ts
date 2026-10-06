export type WorkOrder = {
  id: string
  farmId: string
  machineId: string
  title: string
  priority: string
  status: string
  createdAt: string
  completedAt: string | null
}
