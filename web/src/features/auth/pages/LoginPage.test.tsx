import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { LoginPage } from './LoginPage'

const mutateAsync = vi.fn()
const setSession = vi.fn()

vi.mock('@/features/auth/queries', () => ({
  useLoginMutation: () => ({
    mutateAsync,
    isPending: false,
    isError: false,
    error: null,
  }),
}))

vi.mock('@/shared/auth/store', () => ({
  useAuthStore: (selector: (s: { setSession: typeof setSession }) => unknown) =>
    selector({ setSession }),
}))

function renderLogin() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    mutateAsync.mockReset()
    setSession.mockReset()
  })

  it('renders email, password and demo hint', () => {
    renderLogin()
    expect(screen.getByLabelText('E-mail')).toBeInTheDocument()
    expect(screen.getByLabelText('Senha')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument()
    expect(screen.getByText(/manager@precisionfarming.demo/)).toBeInTheDocument()
    expect(screen.getByText(/Precision@123/)).toBeInTheDocument()
  })

  it('submits credentials and stores the session', async () => {
    const user = userEvent.setup()
    mutateAsync.mockResolvedValue({
      accessToken: 'a',
      refreshToken: 'r',
      role: 'FARM_MANAGER',
      userId: '1',
      name: 'Manager',
      email: 'manager@precisionfarming.demo',
    })
    renderLogin()
    await user.type(screen.getByLabelText('Senha'), 'Precision@123')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(mutateAsync).toHaveBeenCalledWith({
      email: 'manager@precisionfarming.demo',
      password: 'Precision@123',
    })
    expect(setSession).toHaveBeenCalled()
  })
})
