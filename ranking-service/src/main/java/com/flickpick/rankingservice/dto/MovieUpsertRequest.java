package com.flickpick.rankingservice.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// One movie being pushed into the catalog by Import Service, along with the
// calling user's own rating of it where they had one.
//
// tmdbId is required here (unlike on the Movie entity, where the seeded
// catalog predates TMDb identity) because it's the dedup key — a push with
// no stable identity couldn't be matched against what's already stored.
//
// rating is nullable: plenty of Letterboxd entries are logged unrated. It's
// the *caller's* rating, not a property of the film, so it's stored per user
// rather than on the shared catalog row.
public record MovieUpsertRequest(
        @NotNull Long tmdbId, @NotNull String title, String posterUrl, BigDecimal rating) {
}
