package com.flickpick.rankingservice.repository;

import com.flickpick.rankingservice.domain.Movie;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    Optional<Movie> findByTmdbId(Long tmdbId);

    // The "unranked backlog" for a user: any catalog movie they haven't
    // placed into their RankedMovie list yet.
    //
    // Films with a TMDb id came from a real import and are queued first;
    // the fake seed catalog (tmdbId IS NULL) falls to the back. Without
    // this, the 8 seeds hold ids 1-8 and a user who imported their whole
    // Letterboxd history would have to rank all of them before reaching a
    // single film of their own. Ordering by id within each group keeps the
    // queue deterministic and repeatable.
    //
    // Returns the whole backlog (not just one) since the service layer only
    // needs the first entry but a singular return type here would throw if
    // more than one row matched.
    @Query("""
            SELECT m FROM Movie m
            WHERE m.id NOT IN (SELECT rm.movie.id FROM RankedMovie rm WHERE rm.userId = :userId)
            ORDER BY CASE WHEN m.tmdbId IS NULL THEN 1 ELSE 0 END ASC, m.id ASC
            """)
    List<Movie> findUnrankedForUser(@Param("userId") Long userId);
}
