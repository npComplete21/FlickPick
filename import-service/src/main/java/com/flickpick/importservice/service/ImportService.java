package com.flickpick.importservice.service;

import com.flickpick.importservice.client.LetterboxdFilm;
import com.flickpick.importservice.client.LetterboxdRssClient;
import com.flickpick.importservice.client.MovieUpsertRequest;
import com.flickpick.importservice.client.RankingClient;
import com.flickpick.importservice.domain.ImportedFilm;
import com.flickpick.importservice.dto.ImportResultResponse;
import com.flickpick.importservice.repository.ImportedFilmRepository;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportService {

    private final LetterboxdRssClient letterboxdRssClient;
    private final RankingClient rankingClient;
    private final ImportedFilmRepository importedFilmRepository;

    public ImportService(
            LetterboxdRssClient letterboxdRssClient,
            RankingClient rankingClient,
            ImportedFilmRepository importedFilmRepository) {
        this.letterboxdRssClient = letterboxdRssClient;
        this.rankingClient = rankingClient;
        this.importedFilmRepository = importedFilmRepository;
    }

    @Transactional
    public ImportResultResponse importFromLetterboxd(
            Long userId, String username, String bearerToken) {

        List<LetterboxdFilm> films = letterboxdRssClient.fetchWatchedFilms(username);

        // A feed can legitimately list the same film twice (a rewatch is its
        // own entry), so collapse within the batch before touching the DB —
        // otherwise the (user_id, tmdb_id) unique constraint would reject
        // the whole import. Keeping the first occurrence keeps the most
        // recent watch, since the feed is newest-first.
        LinkedHashMap<Long, LetterboxdFilm> distinct = new LinkedHashMap<>();
        films.forEach(film -> distinct.putIfAbsent(film.tmdbId(), film));

        Set<Long> alreadyImported = new HashSet<>(
                importedFilmRepository
                        .findByUserIdAndTmdbIdIn(userId, List.copyOf(distinct.keySet()))
                        .stream()
                        .map(ImportedFilm::getTmdbId)
                        .toList());

        List<LetterboxdFilm> newFilms = distinct.values().stream()
                .filter(film -> !alreadyImported.contains(film.tmdbId()))
                .toList();

        importedFilmRepository.saveAll(newFilms.stream()
                .map(film -> new ImportedFilm(
                        userId,
                        film.tmdbId(),
                        film.title(),
                        film.year(),
                        film.posterUrl(),
                        film.watchedDate(),
                        film.rating(),
                        username))
                .toList());

        // Push every film we know about for this user, not just the new
        // ones: the catalog is shared across users, so a film this user
        // re-imported may still be missing from Ranking Service if it was
        // first seen during a failed push. Ranking Service upserts by TMDb
        // id, so re-sending known films is a no-op there.
        RankingClient.CatalogUpdateResponse catalogUpdate = rankingClient.pushMovies(
                distinct.values().stream()
                        .map(film -> new MovieUpsertRequest(
                                film.tmdbId(), film.title(), film.posterUrl()))
                        .toList(),
                bearerToken);

        return new ImportResultResponse(
                distinct.size(),
                newFilms.size(),
                distinct.size() - newFilms.size(),
                catalogUpdate.added(),
                catalogUpdate.updated());
    }

    @Transactional(readOnly = true)
    public List<ImportedFilm> getImportedFilms(Long userId) {
        return importedFilmRepository.findByUserIdOrderByImportedAtDesc(userId);
    }
}
