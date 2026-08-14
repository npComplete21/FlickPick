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
