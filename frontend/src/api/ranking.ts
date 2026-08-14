import { ApiError, authHeaders, requestJson, requestVoid } from './client'
import { RANKING_SERVICE_URL } from './config'
import type { Comparison, RankingEntry } from './types'

export async function getNextComparison(token: string): Promise<Comparison | null> {
  const res = await fetch(`${RANKING_SERVICE_URL}/api/rankings/next-comparison`, {
    headers: authHeaders(token),
  })
  if (res.status === 204) return null
  if (!res.ok) throw new ApiError(res.status, 'Failed to load next comparison')
  return (await res.json()) as Comparison
}

export function submitComparison(token: string, winnerMovieId: number): Promise<void> {
  return requestVoid(`${RANKING_SERVICE_URL}/api/rankings/compare`, {
    method: 'POST',
    headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
    body: JSON.stringify({ winnerMovieId }),
  })
}

export function getRankings(token: string): Promise<RankingEntry[]> {
  return requestJson<RankingEntry[]>(`${RANKING_SERVICE_URL}/api/rankings`, {
    headers: authHeaders(token),
  })
}
