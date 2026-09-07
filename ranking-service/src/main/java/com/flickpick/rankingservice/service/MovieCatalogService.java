package com.flickpick.rankingservice.service;

import com.flickpick.rankingservice.domain.Movie;
import com.flickpick.rankingservice.domain.UserLibraryEntry;
import com.flickpick.rankingservice.dto.CatalogUpdateResponse;
import com.flickpick.rankingservice.dto.MovieUpsertRequest;
import com.flickpick.rankingservice.repository.MovieRepository;
import com.flickpick.rankingservice.repository.UserLibraryEntryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Receives movies pushed in by Import Service. Ranking Service keeps its own
// copy of the catalog rather than calling Import Service while building a
// comparison queue: the queue is the hot path, and a network hop there would
// make ranking fail whenever Import Service is down.
//
// The film itself goes into the shared catalog; the caller's rating of it is
// stored per user, and is used later only to narrow the binary-insertion
// search (see RankingService.startNextInsertion).
@Service
public class MovieCatalogService {

    private final MovieRepository movieRepository;
    private final UserLibraryEntryRepository userLibraryEntryRepository;

    public MovieCatalogService(
            MovieRepository movieRepository, UserLibraryEntryRepository userLibraryEntryRepository) {
        this.movieRepository = movieRepository;
        this.userLibraryEntryRepository = userLibraryEntryRepository;
    }

    @Transactional
    public CatalogUpdateResponse upsertAll(Long userId, List<MovieUpsertRequest> requests) {
        int added = 0;
        int updated = 0;

        for (MovieUpsertRequest request : requests) {
            Movie movie = movieRepository.findByTmdbId(request.tmdbId()).orElse(null);
            if (movie == null) {
                movie = movieRepository.save(
                        new Movie(request.tmdbId(), request.title(), request.posterUrl()));
                added++;
            } else if (movie.getPosterUrl() == null && request.posterUrl() != null) {
                // Only ever fill in a missing poster; never overwrite one we
                // already have. Re-importing the same film shouldn't churn
                // rows, so this counts as "updated" solely when it changed
                // something.
                movie.setPosterUrl(request.posterUrl());
                movieRepository.save(movie);
                updated++;
            }
            upsertLibraryEntry(userId, movie, request);
        }
        return new CatalogUpdateResponse(added, updated);
    }

    // Records that this user has the film, whether or not they rated it.
    // Membership is what scopes their ranking queue, so an unrated watch
    // still has to produce a row — it just carries a null rating and gets
    // the full search window when it's placed.
    private void upsertLibraryEntry(Long userId, Movie movie, MovieUpsertRequest request) {
        userLibraryEntryRepository
                .findByUserIdAndMovieId(userId, movie.getId())
                .ifPresentOrElse(
                        existing -> {
                            if (request.rating() != null) {
                                existing.setRating(request.rating());
                                userLibraryEntryRepository.save(existing);
                            }
                        },
                        () -> userLibraryEntryRepository.save(
                                new UserLibraryEntry(userId, movie, request.rating())));
    }
}
