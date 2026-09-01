import type { MessageKey } from '@/shared/i18n/useI18n'
import { useI18n } from '@/shared/i18n/useI18n'
import { Button } from '@/shared/ui/Button'

type TabDef<T extends string> = { id: T; labelKey: MessageKey }

type SectionTabsProps<T extends string> = {
  tabs: TabDef<T>[]
  active: T
  onChange: (id: T) => void
}

export function SectionTabs<T extends string>({ tabs, active, onChange }: SectionTabsProps<T>) {
  const { t } = useI18n()
  return (
    <div className="mb-4 flex flex-wrap gap-2" role="tablist">
      {tabs.map((tab) => (
        <Button
          key={tab.id}
          role="tab"
          aria-selected={active === tab.id}
          variant={active === tab.id ? 'primary' : 'secondary'}
          onClick={() => onChange(tab.id)}
        >
          {t(tab.labelKey)}
        </Button>
      ))}
    </div>
  )
}
