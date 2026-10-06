export type InventoryItem = {
  id: string
  farmId: string
  name: string
  category: string
  unit: string
  quantity: number
  reserved: number
}

export type InventoryMovement = {
  id: string
  itemId: string
  type: string
  quantity: number
  occurredAt: string
  reference: string | null
}
