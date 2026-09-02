import { type FormEvent, useState } from 'react'
import { useNavigate } from 'react-router'
import { Sprout } from 'lucide-react'

import { useLoginMutation } from '@/features/auth/queries'
import { ApiError } from '@/shared/api/client'
import { useAuthStore } from '@/shared/auth/store'
import { LocaleToggle } from '@/shared/i18n/LocaleToggle'
import { useI18n } from '@/shared/i18n/useI18n'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'

const DEMO_HINT = import.meta.env.VITE_DEMO_LOGIN_HINT === 'true'
const DEMO_EMAIL = 'manager@precisionfarming.demo'

export function LoginPage() {
  const navigate = useNavigate()
  const setSession = useAuthStore((s) => s.setSession)
  const login = useLoginMutation()
  const { t } = useI18n()
  const [email, setEmail] = useState(DEMO_HINT ? DEMO_EMAIL : '')
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
    <div className="flex min-h-screen items-center justify-center bg-pf-bg p-6">
      <Card className="w-full max-w-md p-8">
        <div className="mb-4 flex items-start justify-between gap-3">
          <div className="flex items-center gap-3 text-pf-green">
            <span className="rounded-[12px] bg-pf-green p-2 text-white">
              <Sprout className="h-6 w-6" aria-hidden />
            </span>
            <div>
              <h1 className="text-xl font-semibold">{t('chrome.brand')}</h1>
              <p className="text-sm text-pf-muted">{t('login.subtitle')}</p>
            </div>
          </div>
          <LocaleToggle />
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
              className="rounded-[12px] border border-pf-border px-3 py-2"
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
              className="rounded-[12px] border border-pf-border px-3 py-2"
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
        {DEMO_HINT ? (
          <p className="mt-4 text-xs text-pf-muted">
            {t('login.demoHint')} <code>{DEMO_EMAIL}</code> / <code>Precision@123</code>
          </p>
        ) : null}
      </Card>
    </div>
  )
}
