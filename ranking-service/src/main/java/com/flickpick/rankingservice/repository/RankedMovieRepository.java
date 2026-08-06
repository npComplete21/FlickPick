package com.flickpick.rankingservice.repository;

import com.flickpick.rankingservice.domain.RankedMovie;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankedMovieRepository extends JpaRepository<RankedMovie, Long> {

    List<RankedMovie> findByUserIdOrderByRankPositionAsc(Long userId);

    Optional<RankedMovie> findByUserIdAndRankPosition(Long userId, int rankPosition);

    // Descending order matters: when shifting existing entries up by one to
    // make room for an insertion, updating highest-position-first means each
    // UPDATE moves an entry into a position that's already vacant, avoiding
    // a transient collision with the (user_id, rank_position) unique
    // constraint that ascending order would cause.
    List<RankedMovie> findByUserIdAndRankPositionGreaterThanEqualOrderByRankPositionDesc(
            Long userId, int rankPosition);

    long countByUserId(Long userId);
}
