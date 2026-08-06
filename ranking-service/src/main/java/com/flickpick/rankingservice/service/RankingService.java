package com.flickpick.rankingservice.service;

import com.flickpick.rankingservice.domain.Movie;
import com.flickpick.rankingservice.domain.PendingInsertion;
import com.flickpick.rankingservice.domain.RankedMovie;
import com.flickpick.rankingservice.dto.ComparisonResponse;
import com.flickpick.rankingservice.dto.MovieResponse;
import com.flickpick.rankingservice.repository.MovieRepository;
import com.flickpick.rankingservice.repository.PendingInsertionRepository;
import com.flickpick.rankingservice.repository.RankedMovieRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RankingService {

    private final MovieRepository movieRepository;
    private final RankedMovieRepository rankedMovieRepository;
    private final PendingInsertionRepository pendingInsertionRepository;
    private final ScoreCalculator scoreCalculator;

    public RankingService(
            MovieRepository movieRepository,
            RankedMovieRepository rankedMovieRepository,
            PendingInsertionRepository pendingInsertionRepository,
            ScoreCalculator scoreCalculator) {
        this.movieRepository = movieRepository;
        this.rankedMovieRepository = rankedMovieRepository;
        this.pendingInsertionRepository = pendingInsertionRepository;
        this.scoreCalculator = scoreCalculator;
    }

    // Returns the next comparison the user needs to answer, or empty if
    // there's nothing left in their backlog to rank. A movie that's the
    // very first (or only) one being inserted resolves with zero
    // comparisons needed (nothing to compare it against yet), so this loops
    // past any such auto-placements rather than surfacing them as a
    // "comparison" with no real opponent.
    @Transactional
    public Optional<ComparisonResponse> getNextComparison(Long userId) {
        while (true) {
            PendingInsertion pending = pendingInsertionRepository.findByUserId(userId)
                    .orElseGet(() -> startNextInsertion(userId).orElse(null));

            if (pending == null) {
                return Optional.empty();
            }
            if (pending.isResolved()) {
                finalizeInsertion(pending);
                continue;
            }

            Movie opponent = movieAtPosition(userId, pending.mid());
            return Optional.of(new ComparisonResponse(
                    MovieResponse.from(pending.getMovie()), MovieResponse.from(opponent)));
        }
    }

    @Transactional
    public void submitComparison(Long userId, Long winnerMovieId) {
        PendingInsertion pending = pendingInsertionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "No comparison in progress for this user"));

        Movie candidate = pending.getMovie();
        Movie opponent = movieAtPosition(userId, pending.mid());

        if (winnerMovieId.equals(candidate.getId())) {
            pending.setHighBound(pending.mid());
        } else if (winnerMovieId.equals(opponent.getId())) {
            pending.setLowBound(pending.mid() + 1);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "winnerMovieId must be one of the two movies being compared");
        }
        pendingInsertionRepository.save(pending);
        // Resolution (low >= high) is handled lazily, the next time
        // getNextComparison() is called — not here.
    }

    @Transactional(readOnly = true)
    public List<RankedMovie> getRankings(Long userId) {
        return rankedMovieRepository.findByUserIdOrderByRankPositionAsc(userId);
    }

    private Optional<PendingInsertion> startNextInsertion(Long userId) {
        List<Movie> unranked = movieRepository.findUnrankedForUser(userId);
        if (unranked.isEmpty()) {
            return Optional.empty();
        }
        Movie next = unranked.get(0);
        int currentSize = (int) rankedMovieRepository.countByUserId(userId);
        return Optional.of(pendingInsertionRepository.save(
                new PendingInsertion(userId, next, 0, currentSize)));
    }

    private Movie movieAtPosition(Long userId, int position) {
        return rankedMovieRepository.findByUserIdAndRankPosition(userId, position)
                .orElseThrow(() -> new IllegalStateException(
                        "Expected a ranked movie at position " + position + " for user " + userId))
                .getMovie();
    }

    private void finalizeInsertion(PendingInsertion pending) {
        Long userId = pending.getUserId();
        int position = pending.getLowBound();

        List<RankedMovie> toShift = rankedMovieRepository
                .findByUserIdAndRankPositionGreaterThanEqualOrderByRankPositionDesc(userId, position);
        toShift.forEach(rm -> rm.setRankPosition(rm.getRankPosition() + 1));
        // Must flush now, not just save(): Hibernate runs all INSERTs before
        // any UPDATEs within a single flush regardless of call order, so
        // without forcing this update to hit the DB first, the insert below
        // would collide with the not-yet-vacated position.
        rankedMovieRepository.saveAllAndFlush(toShift);

        rankedMovieRepository.save(new RankedMovie(userId, pending.getMovie(), position, BigDecimal.ZERO));
        pendingInsertionRepository.delete(pending);

        recomputeScores(userId);
    }

    private void recomputeScores(Long userId) {
        List<RankedMovie> all = rankedMovieRepository.findByUserIdOrderByRankPositionAsc(userId);
        int total = all.size();
        all.forEach(rm -> rm.setScore(scoreCalculator.scoreForPosition(rm.getRankPosition(), total)));
        rankedMovieRepository.saveAll(all);
    }
}
