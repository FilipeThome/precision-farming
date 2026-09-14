import { Navigate, useLocation } from 'react-router'

type Props = { to: string }

/** Redirects to `to` while preserving the current query string (e.g. `?tab=&selected=&farm=`). */
export function RedirectWithSearch({ to }: Props) {
  const { search } = useLocation()
  return <Navigate to={`${to}${search}`} replace />
}
