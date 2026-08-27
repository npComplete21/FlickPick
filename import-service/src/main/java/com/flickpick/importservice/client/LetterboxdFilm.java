package com.flickpick.importservice.client;

import java.math.BigDecimal;
import java.time.LocalDate;

// One watched film parsed out of a Letterboxd RSS feed. rating and
// watchedDate are nullable: not every entry carries a member rating (~8% of
// items in the feeds sampled while building this), and diary metadata can be
// absent on older entries.
public record LetterboxdFilm(
        Long tmdbId,
        String title,
        Integer year,
        String posterUrl,
        LocalDate watchedDate,
        BigDecimal rating) {
}
