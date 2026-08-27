package com.flickpick.rankingservice.service;

import com.flickpick.rankingservice.domain.Movie;
import com.flickpick.rankingservice.dto.CatalogUpdateResponse;
import com.flickpick.rankingservice.dto.MovieUpsertRequest;
import com.flickpick.rankingservice.repository.MovieRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Receives movies pushed in by Import Service. Ranking Service keeps its own
// copy of the catalog rather than calling Import Service while building a
// comparison queue: the queue is the hot path, and a network hop there would
// make ranking fail whenever Import Service is down.
@Service
public class MovieCatalogService {

    private final MovieRepository movieRepository;

    public MovieCatalogService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Transactional
    public CatalogUpdateResponse upsertAll(List<MovieUpsertRequest> requests) {
        int added = 0;
        int updated = 0;

        for (MovieUpsertRequest request : requests) {
            Movie existing = movieRepository.findByTmdbId(request.tmdbId()).orElse(null);
            if (existing == null) {
                movieRepository.save(
                        new Movie(request.tmdbId(), request.title(), request.posterUrl()));
                added++;
            } else if (existing.getPosterUrl() == null && request.posterUrl() != null) {
                // Only ever fill in a missing poster; never overwrite one we
                // already have. Re-importing the same film shouldn't churn
                // rows, so this counts as "updated" solely when it changed
                // something.
                existing.setPosterUrl(request.posterUrl());
                movieRepository.save(existing);
                updated++;
            }
        }
        return new CatalogUpdateResponse(added, updated);
    }
}
