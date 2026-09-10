import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { DetailDrawer } from '@/shared/ui/DetailDrawer'

describe('DetailDrawer', () => {
  it('is absent when closed and a dialog when open', () => {
    const { rerender } = render(
      <DetailDrawer open={false} title="Máquina" onClose={() => undefined}>
        corpo
      </DetailDrawer>,
    )
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    rerender(
      <DetailDrawer open title="Máquina" onClose={() => undefined}>
        corpo
      </DetailDrawer>,
    )
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('Máquina')).toBeInTheDocument()
  })

  it('closes on Escape, overlay and the close button', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(
      <DetailDrawer open title="Máquina" onClose={onClose}>
        corpo
      </DetailDrawer>,
    )

    await user.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalled()

    const closeButtons = screen.getAllByRole('button', { name: 'Fechar' })
    await user.click(closeButtons[0])
    await user.click(closeButtons[1])
    expect(onClose).toHaveBeenCalledTimes(3)
  })
})
