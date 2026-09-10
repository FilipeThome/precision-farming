import { useCallback } from 'react'
import { useSearchParams } from 'react-router'

export function usePatchSearchParams() {
  const [, setParams] = useSearchParams()
  return useCallback((updates: Record<string, string | null>, replace = false) => {
    setParams(
      (prev) => {
        const copy = new URLSearchParams(prev)
        for (const [key, value] of Object.entries(updates)) {
          if (value == null || value === '') copy.delete(key)
          else copy.set(key, value)
        }
        return copy
      },
      { replace },
    )
  }, [setParams])
}

export function useSearchParam(key: string, replace = false) {
  const [params, setParams] = useSearchParams()
  const value = params.get(key)

  const setValue = useCallback(
    (next: string | null) => {
      setParams(
        (prev) => {
          const copy = new URLSearchParams(prev)
          if (next == null || next === '') copy.delete(key)
          else copy.set(key, next)
          return copy
        },
        { replace },
      )
    },
    [key, replace, setParams],
  )

  return [value, setValue] as const
}
