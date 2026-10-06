export type Operation = {
  id: string
  fieldId: string
  farmId: string
  type: string
  status: string
  plannedStart: string | null
  plannedEnd: string | null
  actualStart: string | null
  actualEnd: string | null
  machineId: string | null
  pauseReason: string | null
  itemId: string | null
  itemQuantity: number | null
  areaHa: number | null
  /** Linked prescription when the operation is a spray job. */
  prescriptionId?: string | null
  /** Actual product volume applied (liters), when reported. */
  actualLiters?: number | null
}

export type MachineWorkSummary = {
  areaHa: number
  days: Array<{ day: string; areaHa: number }>
  inputs: Array<{ itemId: string; quantity: number }>
}
