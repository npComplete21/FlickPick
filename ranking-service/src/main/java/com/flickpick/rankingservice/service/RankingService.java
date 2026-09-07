package com.flickpick.rankingservice.service;

import com.flickpick.rankingservice.domain.Movie;
import com.flickpick.rankingservice.domain.PendingInsertion;
import com.flickpick.rankingservice.domain.RankedMovie;
import com.flickpick.rankingservice.domain.UserLibraryEntry;
import com.flickpick.rankingservice.dto.ComparisonResponse;
import com.flickpick.rankingservice.dto.MovieResponse;
import com.flickpick.rankingservice.repository.MovieRepository;
import com.flickpick.rankingservice.repository.PendingInsertionRepository;
import com.flickpick.rankingservice.repository.RankedMovieRepository;
import com.flickpick.rankingservice.repository.UserLibraryEntryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RankingService {

    private final MovieRepository movieRepository;
    private final RankedMovieRepository rankedMovieRepository;
    private final PendingInsertionRepository pendingInsertionRepository;
    private final UserLibraryEntryRepository userLibraryEntryRepository;
    private final ScoreCalculator scoreCalculator;

    public RankingService(
            MovieRepository movieRepository,
            RankedMovieRepository rankedMovieRepository,
            PendingInsertionRepository pendingInsertionRepository,
            UserLibraryEntryRepository userLibraryEntryRepository,
            ScoreCalculator scoreCalculator) {
        this.movieRepository = movieRepository;
        this.rankedMovieRepository = rankedMovieRepository;
        this.pendingInsertionRepository = pendingInsertionRepository;
        this.userLibraryEntryRepository = userLibraryEntryRepository;
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
        evictSeedsOnceLibraryExists(userId);
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

    // The seed catalog is a fallback for an account with nothing imported,
    // but Compare is the frontend's default landing view — so a brand new
    // user fires next-comparison before they have even found the Import tab.
    // That starts a seed insertion, and the very first seed auto-places with
    // zero comparisons. Left alone it latches: both survive the import, and
    // fake films the user never said they watched sit permanently in a real
    // ranking, and keep showing up as opponents.
    //
    // So the moment the user actually has a library, seeds are evicted —
    // any in-flight seed insertion and any already-placed seed rows. The
    // handful of comparisons already answered about them are discarded,
    // which is the right trade: they were about films the user never claimed
    // to have seen.
    private void evictSeedsOnceLibraryExists(Long userId) {
        if (userLibraryEntryRepository.countByUserId(userId) == 0) {
            return; // still nothing imported, so the seeds are legitimate
        }

        pendingInsertionRepository.findByUserId(userId)
                .filter(p -> p.getMovie().getTmdbId() == null)
                .ifPresent(p -> {
                    pendingInsertionRepository.delete(p);
                    // Flush before anything re-uses the unique user_id.
                    pendingInsertionRepository.flush();
                });

        List<RankedMovie> all = rankedMovieRepository.findByUserIdOrderByRankPositionAsc(userId);
        List<RankedMovie> seeds =
                all.stream().filter(rm -> rm.getMovie().getTmdbId() == null).toList();
        if (seeds.isEmpty()) {
            return;
        }
        rankedMovieRepository.deleteAll(seeds);
        rankedMovieRepository.flush();

        // Removing rows leaves holes, so close them up. Staged through
        // negatives for the same reason the insertion shift is: no
        // intermediate flush order may collide on (user_id, rank_position).
        List<RankedMovie> remaining =
                all.stream().filter(rm -> rm.getMovie().getTmdbId() != null).toList();
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setRankPosition(-(i + 1));
        }
        rankedMovieRepository.saveAllAndFlush(remaining);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setRankPosition(i);
        }
        rankedMovieRepository.saveAllAndFlush(remaining);

        recomputeScores(userId);
    }

    private Optional<PendingInsertion> startNextInsertion(Long userId) {
        List<Movie> unranked = movieRepository.findUnrankedInLibrary(userId);
        if (unranked.isEmpty() && userLibraryEntryRepository.countByUserId(userId) == 0) {
            // Nothing imported at all, so fall back to the seeded stand-in
            // catalog — a brand new account still gets something to rank.
            // Deliberately gated on an *empty library* rather than an empty
            // backlog: someone who has imported and ranked everything is
            // finished, and shouldn't be handed fake films afterwards.
            unranked = movieRepository.findUnrankedSeeds(userId);
        }
        if (unranked.isEmpty()) {
            return Optional.empty();
        }
        Movie next = unranked.get(0);
        List<RankedMovie> ranked = rankedMovieRepository.findByUserIdOrderByRankPositionAsc(userId);
        Bounds bounds = seedBounds(userId, next, ranked);
        return Optional.of(pendingInsertionRepository.save(
                new PendingInsertion(userId, next, bounds.low(), bounds.high())));
    }

    private record Bounds(int low, int high) {
    }

    // Narrows the initial binary-search window using a rating the user
    // already gave this film elsewhere (imported from Letterboxd). Without
    // one, the window is the whole list — exactly the previous behaviour.
    //
    // Why this matters: binary insertion costs ceil(log2 i) comparisons for
    // the i-th film, so a 50-film import runs to ~230 comparisons and a
    // full library runs to thousands. Seeding the bounds cut that by ~59%
    // on real imported data.
    //
    // Only the *leading* run of films rated strictly better, and the
    // *trailing* run rated strictly worse, are excluded; each run stops at
    // the first film that breaks it, and unrated films break a run too
    // since they assert nothing about where the candidate belongs.
    //
    // Be clear about what this costs: within a rating band the user's
    // comparisons decide the order, but *across* bands the imported rating
    // is effectively a hard constraint, not a hint. Someone who now rates a
    // film differently than they did on Letterboxd cannot express that here
    // — a film they starred 1.0 can never be compared against, or placed
    // above, one they starred 5.0. Measured on a real 49-film import this
    // buys a 61-70% cut in comparisons (231 -> 91 when choices agree with
    // the stars, -> 69 when they contradict them), which is the trade being
    // made. Loosening it (biasing only the starting midpoint, or widening
    // the band) would restore full expressiveness for fewer savings.
    private Bounds seedBounds(Long userId, Movie candidate, List<RankedMovie> ranked) {
        int size = ranked.size();
        BigDecimal rating = userLibraryEntryRepository
                .findByUserIdAndMovieId(userId, candidate.getId())
                .map(UserLibraryEntry::getRating)
                .orElse(null);
        if (rating == null || size == 0) {
            return new Bounds(0, size);
        }

        // Unrated entries are filtered out rather than mapped to null:
        // Collectors.toMap throws on null values, and an absent key is
        // already exactly how "no opinion" is handled below.
        Map<Long, BigDecimal> ratings = userLibraryEntryRepository.findByUserId(userId).stream()
                .filter(e -> e.getRating() != null)
                .collect(Collectors.toMap(
                        e -> e.getMovie().getId(), UserLibraryEntry::getRating, (a, b) -> a));

        int low = 0;
        while (low < size && ratedBetterThan(ranked, low, ratings, rating)) {
            low++;
        }
        int high = size;
        while (high > low && ratedWorseThan(ranked, high - 1, ratings, rating)) {
            high--;
        }
        return new Bounds(low, high);
    }

    private boolean ratedBetterThan(
            List<RankedMovie> ranked, int index, Map<Long, BigDecimal> ratings, BigDecimal rating) {
        BigDecimal other = ratings.get(ranked.get(index).getMovie().getId());
        return other != null && other.compareTo(rating) > 0;
    }

    private boolean ratedWorseThan(
            List<RankedMovie> ranked, int index, Map<Long, BigDecimal> ratings, BigDecimal rating) {
        BigDecimal other = ratings.get(ranked.get(index).getMovie().getId());
        return other != null && other.compareTo(rating) < 0;
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

        // Shifting everyone down by one has to happen without ever having two
        // rows on the same position, because (user_id, rank_position) is
        // unique and Postgres checks it per row, not at commit.
        //
        // Doing it in one pass is not safe even iterating high-to-low: we
        // only control the order we *call* setters in, not the order
        // Hibernate emits the UPDATEs at flush. If it writes 3->4 before
        // 4->5, it collides with the row still sitting at 4.
        //
        // So move the affected rows through a negative staging range first.
        // Real positions are always >= 0, so nothing there can collide, and
        // p -> -(p + 2) is injective so the staged rows don't collide with
        // each other either. The second pass maps -(p + 2) -> p + 1.
        toShift.forEach(rm -> rm.setRankPosition(-(rm.getRankPosition() + 2)));
        rankedMovieRepository.saveAllAndFlush(toShift);
        toShift.forEach(rm -> rm.setRankPosition(-rm.getRankPosition() - 1));
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
