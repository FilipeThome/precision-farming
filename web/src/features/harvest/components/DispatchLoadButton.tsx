import { useDispatchLoad } from '@/features/harvest/queries'
import { useCanManageFarmOps } from '@/shared/auth/roles'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { Button } from '@/shared/ui/Button'

type Props = {
  loadId: string
  disabled?: boolean
}

export function DispatchLoadButton({ loadId, disabled }: Props) {
  const { t } = useI18n()
  const canDispatch = useCanManageFarmOps()
  const dispatch = useDispatchLoad()
  const err = dispatch.error ? queryError(dispatch.error) : null

  if (!canDispatch) return null

  return (
    <div className="mt-2 flex flex-col gap-1">
      <Button
        disabled={disabled || dispatch.isPending}
        onClick={() => dispatch.mutate(loadId)}
      >
        {t('harvest.logistics.dispatch')}
      </Button>
      {err ? (
        <p className="text-xs text-red-800" role="alert">
          {err.message}
          {err.correlationId ? ` · ${t('common.correlationId')} ${err.correlationId}` : ''}
        </p>
      ) : null}
    </div>
  )
}
