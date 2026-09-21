import { beforeEach, describe, expect, it, vi } from 'vitest'

const apiUpload = vi.fn()
const apiPost = vi.fn()
const apiPatch = vi.fn()

vi.mock('@/shared/api/client', () => ({
  apiUpload: (...args: unknown[]) => apiUpload(...args),
  apiPost: (...args: unknown[]) => apiPost(...args),
  apiPatch: (...args: unknown[]) => apiPatch(...args),
}))

import { saveMachine } from './api'

const body = {
  farmId: 'farm-1',
  name: 'JD 8R',
  type: 'TRACTOR',
  manufacturer: 'John Deere',
  model: '8R 410',
  status: 'IDLE',
}

describe('saveMachine', () => {
  beforeEach(() => {
    apiUpload.mockReset()
    apiPost.mockReset()
    apiPatch.mockReset()
  })

  it('uploads the photo before creating the machine', async () => {
    const order: string[] = []
    apiUpload.mockImplementation(async () => {
      order.push('upload')
      return { id: 'file-1' }
    })
    apiPost.mockImplementation(async () => {
      order.push('create')
      return { id: 'm1', ...body, photoFileId: 'file-1' }
    })
    await saveMachine({
      body,
      photo: new File(['x'], 'tractor.jpg', { type: 'image/jpeg' }),
    })
    expect(order).toEqual(['upload', 'create'])
    expect(apiPost).toHaveBeenCalledWith(
      '/api/v1/machines',
      expect.objectContaining({ photoFileId: 'file-1' }),
    )
    expect(apiPatch).not.toHaveBeenCalled()
  })

  it('uploads the photo before patching an existing machine', async () => {
    const order: string[] = []
    apiUpload.mockImplementation(async () => {
      order.push('upload')
      return { id: 'file-2' }
    })
    apiPatch.mockImplementation(async () => {
      order.push('patch')
      return { id: 'm1', ...body, photoFileId: 'file-2' }
    })
    await saveMachine({
      id: 'm1',
      body,
      photo: new File(['x'], 'tractor.jpg', { type: 'image/jpeg' }),
    })
    expect(order).toEqual(['upload', 'patch'])
    expect(apiPatch).toHaveBeenCalledWith(
      '/api/v1/machines/m1',
      expect.objectContaining({ photoFileId: 'file-2' }),
    )
  })
})
