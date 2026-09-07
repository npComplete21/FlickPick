package com.flickpick.rankingservice.repository;

import com.flickpick.rankingservice.domain.Movie;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    Optional<Movie> findByTmdbId(Long tmdbId);

    // The user's unranked backlog: films in *their own* library that they
    // haven't placed yet.
    //
    // Scoped to the library rather than the whole catalog because `movies`
    // is shared across all users (that's what makes TMDb dedup work) — an
    // unscoped query would ask people to rank films someone else imported
    // and they have never seen.
    //
    // Ordered by id for a deterministic, repeatable queue.
    @Query("""
            SELECT m FROM Movie m
            WHERE m.id IN (SELECT e.movie.id FROM UserLibraryEntry e WHERE e.userId = :userId)
              AND m.id NOT IN (SELECT rm.movie.id FROM RankedMovie rm WHERE rm.userId = :userId)
            ORDER BY m.id ASC
            """)
    List<Movie> findUnrankedInLibrary(@Param("userId") Long userId);

    // Fallback for an account that has imported nothing at all, so a brand
    // new user still has something to rank. Seeded stand-in films are the
    // ones with no TMDb identity (see MovieSeeder).
    @Query("""
            SELECT m FROM Movie m
            WHERE m.tmdbId IS NULL
              AND m.id NOT IN (SELECT rm.movie.id FROM RankedMovie rm WHERE rm.userId = :userId)
            ORDER BY m.id ASC
            """)
    List<Movie> findUnrankedSeeds(@Param("userId") Long userId);
}
