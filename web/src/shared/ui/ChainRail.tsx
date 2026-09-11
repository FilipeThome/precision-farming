import { Check, X } from 'lucide-react'

export type ChainStepState = 'done' | 'now' | 'pending' | 'blocked'

export type ChainStep = {
  id: string
  label: string
  state: ChainStepState
  /** Optional tooltip/hint (e.g. "inferred"). */
  hint?: string
}

type ChainRailProps = {
  steps: ChainStep[]
  className?: string
  'aria-label'?: string
}

const TEXT: Record<ChainStepState, string> = {
  done: 'text-ag-g-800',
  now: 'font-bold text-ag-t-700',
  pending: 'text-ag-n-600',
  blocked: 'font-semibold text-ag-crit',
}

const DOT: Record<ChainStepState, string> = {
  done: 'border-ag-g-600 bg-ag-g-600 text-white',
  now: 'border-ag-t-500 bg-ag-t-500 shadow-[0_0_0_4px_var(--color-ag-t-100)]',
  pending: 'border-ag-n-300 bg-ag-n-0',
  blocked: 'border-ag-crit bg-ag-crit-bg text-ag-crit',
}

/** Horizontal stepper: Sinal → Contexto → Recomendação → Aprovação → Ordem → Execução … */
export function ChainRail({ steps, className = '', 'aria-label': ariaLabel }: ChainRailProps) {
  return (
    <ol className={`flex items-start py-1.5 ${className}`} aria-label={ariaLabel}>
      {steps.map((step, index) => {
        const last = index === steps.length - 1
        return (
          <li
            key={step.id}
            className={`relative flex flex-1 flex-col items-center gap-1.5 text-center text-[11px] ${TEXT[step.state]}`}
            data-state={step.state}
            aria-current={step.state === 'now' ? 'step' : undefined}
            title={step.hint}
          >
            {!last ? (
              <span
                className={`absolute left-1/2 right-[-50%] top-[9px] h-0.5 ${step.state === 'done' ? 'bg-ag-g-600' : 'bg-ag-n-200'}`}
                aria-hidden
              />
            ) : null}
            <span className={`z-[1] grid h-[18px] w-[18px] place-items-center rounded-full border-2 ${DOT[step.state]}`} aria-hidden>
              {step.state === 'done' ? <Check className="h-3 w-3" /> : null}
              {step.state === 'blocked' ? <X className="h-3 w-3" /> : null}
            </span>
            <span>{step.label}</span>
          </li>
        )
      })}
    </ol>
  )
}
