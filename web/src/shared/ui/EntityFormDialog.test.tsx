import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { EntityFormDialog } from '@/shared/ui/EntityFormDialog'

describe('EntityFormDialog', () => {
  it('submits values and cancels', () => {
    const onSubmit = vi.fn()
    const onClose = vi.fn()
    const onChange = vi.fn()
    render(
      <EntityFormDialog
        open
        title="Nova fazenda"
        fields={[{ name: 'name', label: 'Nome', type: 'text', required: true }]}
        values={{ name: 'Alfa' }}
        onChange={onChange}
        onSubmit={onSubmit}
        onClose={onClose}
      />,
    )
    expect(screen.getByRole('dialog', { name: 'Nova fazenda' })).toBeInTheDocument()
    fireEvent.submit(screen.getByRole('dialog').querySelector('form')!)
    expect(onSubmit).toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(onClose).toHaveBeenCalled()
  })

  it('disables submit while pending', () => {
    render(
      <EntityFormDialog
        open
        title="Nova fazenda"
        fields={[{ name: 'name', label: 'Nome', type: 'text' }]}
        values={{ name: '' }}
        onChange={() => undefined}
        onSubmit={() => undefined}
        onClose={() => undefined}
        pending
      />,
    )
    expect(screen.getByRole('button', { name: 'Salvando…' })).toBeDisabled()
  })
})
