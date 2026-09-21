import { Button } from '@/shared/ui/Button'
import { useI18n } from '@/shared/i18n/useI18n'

export function InspectorEditButton({ onEdit }: { onEdit: () => void }) {
  const { t } = useI18n()
  return (
    <div className="mt-4">
      <Button type="button" variant="secondary" onClick={onEdit}>
        {t('form.edit')}
      </Button>
    </div>
  )
}
