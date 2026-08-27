import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { getImportedFilms, importFromLetterboxd } from '../api/import'
import type { ImportResult, ImportedFilm } from '../api/types'

interface ImportScreenProps {
  token: string
  onUnauthorized: () => void
}

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 404) return "No public Letterboxd feed found for that username."
    if (error.status === 400) return 'That doesn’t look like a valid Letterboxd username.'
    if (error.status === 502) return 'Could not reach Letterboxd right now. Try again in a moment.'
  }
  return 'Something went wrong during the import.'
}

export default function ImportScreen({ token, onUnauthorized }: ImportScreenProps) {
  const [username, setUsername] = useState('')
  const [result, setResult] = useState<ImportResult | null>(null)
  const [films, setFilms] = useState<ImportedFilm[]>([])
  const [error, setError] = useState<string | null>(null)
  const [importing, setImporting] = useState(false)

  const handleUnauthorized = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return true
      }
      return false
    },
    [onUnauthorized],
  )

  const loadFilms = useCallback(() => {
    getImportedFilms(token)
      .then(setFilms)
      .catch((err: unknown) => {
        if (!handleUnauthorized(err)) setError('Could not load your imported films.')
      })
  }, [token, handleUnauthorized])

  useEffect(() => {
    loadFilms()
  }, [loadFilms])

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    setResult(null)
    setImporting(true)
    importFromLetterboxd(token, username.trim())
      .then((res) => {
        setResult(res)
        loadFilms()
      })
      .catch((err: unknown) => {
        if (!handleUnauthorized(err)) setError(errorMessage(err))
      })
      .finally(() => setImporting(false))
  }

  return (
    <main className="import-page">
      <h2>Import from Letterboxd</h2>
      <p className="import-help">
        Enter a Letterboxd username to pull in their recent watches. The public RSS feed
        covers roughly the last 50 films.
      </p>
      <form onSubmit={handleSubmit} className="import-form">
        <input
          type="text"
          value={username}
          placeholder="letterboxd username"
          onChange={(e) => setUsername(e.target.value)}
          required
        />
        <button type="submit" disabled={importing || username.trim() === ''}>
          {importing ? 'Importing…' : 'Import'}
        </button>
      </form>

      {error && <p className="auth-error">{error}</p>}

      {result && (
        <p className="import-result">
          Found {result.filmsFound} films — {result.newFilms} new, {result.alreadyKnown}{' '}
          already imported. {result.addedToCatalog} added to your ranking queue.
        </p>
      )}

      {films.length > 0 && (
        <>
          <h3>Imported films ({films.length})</h3>
          <ul className="imported-list">
            {films.map((film) => (
              <li key={film.tmdbId}>
                {film.posterUrl && <img src={film.posterUrl} alt={film.title} />}
                <span className="imported-title">
                  {film.title}
                  {film.releaseYear ? ` (${film.releaseYear})` : ''}
                </span>
                {film.memberRating !== null && (
                  <span className="imported-rating">{film.memberRating.toFixed(1)}</span>
                )}
              </li>
            ))}
          </ul>
        </>
      )}
    </main>
  )
}
