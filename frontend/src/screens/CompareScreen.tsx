import { useCallback, useEffect, useState } from 'react'
import { getNextComparison, submitComparison } from '../api/ranking'
import { ApiError } from '../api/client'
import type { Comparison } from '../api/types'

interface CompareScreenProps {
  token: string
  onUnauthorized: () => void
}

export default function CompareScreen({ token, onUnauthorized }: CompareScreenProps) {
  const [comparison, setComparison] = useState<Comparison | null | 'loading'>('loading')
  const [error, setError] = useState<string | null>(null)
  const [submittingId, setSubmittingId] = useState<number | null>(null)

  const loadNext = useCallback(() => {
    setComparison('loading')
    getNextComparison(token)
      .then(setComparison)
      .catch((err: unknown) => {
        if (err instanceof ApiError && err.status === 401) {
          onUnauthorized()
          return
        }
        setError('Failed to load the next comparison.')
      })
  }, [token, onUnauthorized])

  useEffect(() => {
    loadNext()
  }, [loadNext])

  const pick = (winnerMovieId: number) => {
    setSubmittingId(winnerMovieId)
    setError(null)
    submitComparison(token, winnerMovieId)
      .then(loadNext)
      .catch((err: unknown) => {
        if (err instanceof ApiError && err.status === 401) {
          onUnauthorized()
          return
        }
        setError('Failed to submit your pick.')
      })
      .finally(() => setSubmittingId(null))
  }

  if (comparison === 'loading') {
    return (
      <main className="compare-page">
        <p>Loading…</p>
      </main>
    )
  }

  if (comparison === null) {
    return (
      <main className="compare-page">
        <h2>You're all caught up</h2>
        <p>No more comparisons to make right now.</p>
      </main>
    )
  }

  return (
    <main className="compare-page">
      <h2>Which do you prefer?</h2>
      {error && <p className="auth-error">{error}</p>}
      <div className="movie-pair">
        {[comparison.candidate, comparison.opponent].map((movie) => (
          <button
            key={movie.id}
            className="movie-card"
            disabled={submittingId !== null}
            onClick={() => pick(movie.id)}
          >
            {movie.posterUrl && <img src={movie.posterUrl} alt={movie.title} />}
            <span>{movie.title}</span>
          </button>
        ))}
      </div>
    </main>
  )
}
