import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { getRankings } from '../api/ranking'
import type { RankingEntry } from '../api/types'

interface RankingsScreenProps {
  token: string
  onUnauthorized: () => void
}

export default function RankingsScreen({ token, onUnauthorized }: RankingsScreenProps) {
  const [rankings, setRankings] = useState<RankingEntry[] | 'loading'>('loading')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getRankings(token)
      .then(setRankings)
      .catch((err: unknown) => {
        if (err instanceof ApiError && err.status === 401) {
          onUnauthorized()
          return
        }
        setError('Failed to load your rankings.')
      })
  }, [token, onUnauthorized])

  if (error) {
    return (
      <main className="rankings-page">
        <p className="auth-error">{error}</p>
      </main>
    )
  }

  if (rankings === 'loading') {
    return (
      <main className="rankings-page">
        <p>Loading…</p>
      </main>
    )
  }

  if (rankings.length === 0) {
    return (
      <main className="rankings-page">
        <h2>Your Rankings</h2>
        <p>No ranked movies yet — go make some comparisons.</p>
      </main>
    )
  }

  return (
    <main className="rankings-page">
      <h2>Your Rankings</h2>
      <ol className="ranking-list">
        {rankings.map((entry) => (
          <li key={entry.movieId}>
            {entry.posterUrl && <img src={entry.posterUrl} alt={entry.title} />}
            <span className="ranking-title">{entry.title}</span>
            <span className="ranking-score">{entry.score.toFixed(1)}</span>
          </li>
        ))}
      </ol>
    </main>
  )
}
