import { render, screen } from '@testing-library/react'
import { Bot, Tractor, Wheat } from 'lucide-react'
import { describe, expect, it } from 'vitest'

import { EntityTile, entityIcon } from '@/shared/ui/EntityTile'

describe('EntityTile', () => {
  it('picks the icon by kind and machine type', () => {
    expect(entityIcon('machine')).toBe(Tractor)
    expect(entityIcon('machine', 'DRONE')).toBe(Bot)
    expect(entityIcon('machine', 'unknown-type')).toBe(Tractor)
    expect(entityIcon('crop')).toBe(Wheat)
  })

  it('is decorative unless a label is given', () => {
    const { rerender } = render(<EntityTile kind="farm" />)
    expect(screen.queryByRole('img')).toBeNull()
    rerender(<EntityTile kind="farm" label="Fazenda Alfa" />)
    expect(screen.getByRole('img', { name: 'Fazenda Alfa' })).toBeInTheDocument()
  })

  it('renders a photo when photoUrl is set', () => {
    render(<EntityTile kind="machine" photoUrl="blob:test" label="JD 8R" />)
    expect(screen.getByRole('img', { name: 'JD 8R' }).querySelector('img')).toHaveAttribute('src', 'blob:test')
  })
})
