import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { PhotoField } from '@/shared/ui/PhotoField'

describe('PhotoField', () => {
  it('accepts jpeg files', () => {
    const onChange = vi.fn()
    render(<PhotoField file={null} onChange={onChange} />)
    const input = screen.getByLabelText('Foto da máquina') as HTMLInputElement
    const file = new File(['x'], 'tractor.jpg', { type: 'image/jpeg' })
    fireEvent.change(input, { target: { files: [file] } })
    expect(onChange).toHaveBeenCalledWith(file)
  })

  it('rejects unsupported types and shows an error', () => {
    const onChange = vi.fn()
    const onInvalid = vi.fn()
    render(<PhotoField file={null} onChange={onChange} onInvalid={onInvalid} />)
    const input = screen.getByLabelText('Foto da máquina') as HTMLInputElement
    const file = new File(['x'], 'notes.txt', { type: 'text/plain' })
    fireEvent.change(input, { target: { files: [file] } })
    expect(onChange).toHaveBeenCalledWith(null)
    expect(onInvalid).toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('JPEG')
  })

  it('rejects files larger than 5MB', () => {
    const onChange = vi.fn()
    render(<PhotoField file={null} onChange={onChange} />)
    const input = screen.getByLabelText('Foto da máquina') as HTMLInputElement
    const file = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'big.jpg', { type: 'image/jpeg' })
    fireEvent.change(input, { target: { files: [file] } })
    expect(onChange).toHaveBeenCalledWith(null)
    expect(screen.getByRole('alert')).toBeInTheDocument()
  })
})
