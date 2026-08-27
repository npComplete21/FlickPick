package com.flickpick.importservice.dto;

import com.flickpick.importservice.domain.ImportedFilm;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportedFilmResponse(
        Long tmdbId,
        String title,
        Integer releaseYear,
        String posterUrl,
        LocalDate watchedDate,
        BigDecimal memberRating) {

    public static ImportedFilmResponse from(ImportedFilm film) {
        return new ImportedFilmResponse(
                film.getTmdbId(),
                film.getTitle(),
                film.getReleaseYear(),
                film.getPosterUrl(),
                film.getWatchedDate(),
                film.getMemberRating());
    }
}
