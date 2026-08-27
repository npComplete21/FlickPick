import { authHeaders, requestJson } from './client'
import { IMPORT_SERVICE_URL } from './config'
import type { ImportResult, ImportedFilm } from './types'

export function importFromLetterboxd(
  token: string,
  letterboxdUsername: string,
): Promise<ImportResult> {
  return requestJson<ImportResult>(`${IMPORT_SERVICE_URL}/api/imports`, {
    method: 'POST',
    headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
    body: JSON.stringify({ letterboxdUsername }),
  })
}

export function getImportedFilms(token: string): Promise<ImportedFilm[]> {
  return requestJson<ImportedFilm[]>(`${IMPORT_SERVICE_URL}/api/imports`, {
    headers: authHeaders(token),
  })
}
