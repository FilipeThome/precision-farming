import { type FormEvent, useState } from 'react'
import { useNavigate } from 'react-router'

import { useLoginMutation } from '@/features/auth/queries'
import { ApiError } from '@/shared/api/client'
import { useAuthStore } from '@/shared/auth/store'
import { LocaleToggle } from '@/shared/i18n/LocaleToggle'
import { useI18n } from '@/shared/i18n/useI18n'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'

export function LoginPage() {
  const navigate = useNavigate()
  const setSession = useAuthStore((s) => s.setSession)
  const login = useLoginMutation()
  const { t } = useI18n()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    try {
      const session = await login.mutateAsync({ email, password })
      setSession(session)
      navigate('/dashboard', { replace: true })
    } catch {
      // error rendered below
    }
  }

  const errorMessage =
    login.error instanceof ApiError
      ? login.error.message
      : login.isError
        ? t('login.error')
        : null

  return (
    <div className="flex min-h-screen items-center justify-center bg-ag-n-50 p-6">
      <Card className="w-full max-w-md p-8">
        <div className="mb-4">
          <div className="flex items-center gap-3">
            <img
              src="/brand/precision-farming-logo.svg"
              alt=""
              className="block h-auto min-w-0 w-[min(100%,16rem)] max-w-[calc(100%-6.75rem)]"
              draggable={false}
            />
            <div className="ml-auto shrink-0">
              <LocaleToggle />
            </div>
          </div>
          <h1 className="sr-only">{t('chrome.brand')}</h1>
          <p className="text-sm text-pf-muted">{t('login.subtitle')}</p>
        </div>
        <form onSubmit={onSubmit} className="flex flex-col gap-4">
          <label htmlFor="email" className="flex flex-col gap-1 text-sm">
            {t('login.email')}
            <input
              id="email"
              type="email"
              name="email"
              autoComplete="username"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="rounded-[8px] border border-ag-n-300 px-3 py-2"
            />
          </label>
          <label htmlFor="password" className="flex flex-col gap-1 text-sm">
            {t('login.password')}
            <input
              id="password"
              type="password"
              name="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="rounded-[8px] border border-ag-n-300 px-3 py-2"
            />
          </label>
          {errorMessage ? (
            <p className="text-sm text-red-800" role="alert">
              {errorMessage}
            </p>
          ) : null}
          <Button type="submit" disabled={login.isPending}>
            {login.isPending ? t('login.submitting') : t('login.submit')}
          </Button>
        </form>
      </Card>
    </div>
  )
}
