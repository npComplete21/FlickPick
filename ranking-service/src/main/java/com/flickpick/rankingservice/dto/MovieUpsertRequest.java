package com.flickpick.rankingservice.dto;

import jakarta.validation.constraints.NotNull;

// One movie being pushed into the catalog by Import Service. tmdbId is
// required here (unlike on the Movie entity, where the seeded catalog
// predates TMDb identity) because it's the dedup key — a push with no
// stable identity couldn't be matched against what's already stored.
public record MovieUpsertRequest(
        @NotNull Long tmdbId, @NotNull String title, String posterUrl) {
}
