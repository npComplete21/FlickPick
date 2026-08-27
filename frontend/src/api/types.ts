export interface Movie {
  id: number
  title: string
  posterUrl: string | null
}

export interface Comparison {
  candidate: Movie
  opponent: Movie
}

export interface RankingEntry {
  movieId: number
  title: string
  posterUrl: string | null
  rankPosition: number
  score: number
}

export interface ImportResult {
  filmsFound: number
  newFilms: number
  alreadyKnown: number
  addedToCatalog: number
  updatedInCatalog: number
}

export interface ImportedFilm {
  tmdbId: number
  title: string
  releaseYear: number | null
  posterUrl: string | null
  watchedDate: string | null
  memberRating: number | null
}
