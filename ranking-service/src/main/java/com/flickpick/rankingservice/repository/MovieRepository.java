package com.flickpick.rankingservice.repository;

import com.flickpick.rankingservice.domain.Movie;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    // The "unranked backlog" for a user: any seeded movie they haven't
    // placed into their RankedMovie list yet. Ordered by id purely for a
    // deterministic, repeatable queue order during this fake-data stage.
    // Returns the whole backlog (not just one) since the service layer only
    // needs the first entry but a singular return type here would throw if
    // more than one row matched.
    @Query("""
            SELECT m FROM Movie m
            WHERE m.id NOT IN (SELECT rm.movie.id FROM RankedMovie rm WHERE rm.userId = :userId)
            ORDER BY m.id ASC
            """)
    List<Movie> findUnrankedForUser(@Param("userId") Long userId);
}
