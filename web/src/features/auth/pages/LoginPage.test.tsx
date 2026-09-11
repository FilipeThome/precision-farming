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

  it('renders empty email and password fields without any prefilled hint', () => {
    renderLogin()
    expect(screen.getByLabelText('E-mail')).toHaveValue('')
    expect(screen.getByLabelText('Senha')).toHaveValue('')
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument()
    expect(screen.queryByText(/precisionfarming.demo/)).not.toBeInTheDocument()
    expect(screen.queryByText(/Precision@123/)).not.toBeInTheDocument()
  })

  it('has no prefilled values, placeholders or hint text carrying credentials', () => {
    const { container } = renderLogin()
    const inputs = container.querySelectorAll('input')
    expect(inputs.length).toBeGreaterThanOrEqual(2)
    for (const input of inputs) {
      expect(input.value).toBe('')
      expect(input.defaultValue).toBe('')
      expect(input.getAttribute('placeholder') ?? '').not.toMatch(/@|Precision/)
    }
    expect(screen.getByLabelText('Senha')).toHaveAttribute('type', 'password')
    expect(container.textContent).not.toMatch(/@precisionfarming|demo|Demo/)
  })

  it('does not store a session when the login fails', async () => {
    const user = userEvent.setup()
    mutateAsync.mockRejectedValue(new Error('boom'))
    renderLogin()
    await user.type(screen.getByLabelText('E-mail'), 'a@b.co')
    await user.type(screen.getByLabelText('Senha'), 'x')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(mutateAsync).toHaveBeenCalledTimes(1)
    expect(setSession).not.toHaveBeenCalled()
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
    await user.type(screen.getByLabelText('E-mail'), 'manager@precisionfarming.demo')
    await user.type(screen.getByLabelText('Senha'), 'Precision@123')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(mutateAsync).toHaveBeenCalledWith({
      email: 'manager@precisionfarming.demo',
      password: 'Precision@123',
    })
    expect(setSession).toHaveBeenCalled()
  })
})
