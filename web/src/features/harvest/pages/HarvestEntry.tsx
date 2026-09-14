import { useLocation } from 'react-router'

import { PostHarvestPage } from '@/features/harvest/pages/PostHarvestPage'
import { RedirectWithSearch } from '@/shared/lib/RedirectWithSearch'

/**
 * `/harvest` is the Post-Harvest Control Tower. Legacy deep links carrying `?tab=` or `?selected=`
 * (pre-redesign HarvestPage) are forwarded to `/harvest/detail` with the same query string.
 */
export function HarvestEntry() {
  const { search } = useLocation()
  const params = new URLSearchParams(search)
  if (params.has('tab') || params.has('selected') || params.has('view')) {
    return <RedirectWithSearch to="/harvest/detail" />
  }
  return <PostHarvestPage />
}
