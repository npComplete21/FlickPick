package com.flickpick.importservice.client;

import java.math.BigDecimal;

// Mirrors Ranking Service's MovieUpsertRequest. Deliberately duplicated
// rather than extracted into a shared module: a shared DTO jar would couple
// the two services' release cycles, which is the coupling the service split
// was meant to avoid. The cost is keeping two small records in step.
//
// rating is the importing user's own rating of the film (nullable — plenty
// of Letterboxd entries are logged unrated). Ranking Service uses it only to
// narrow the binary-insertion search, never as the final ordering.
public record MovieUpsertRequest(
        Long tmdbId, String title, String posterUrl, BigDecimal rating) {
}
