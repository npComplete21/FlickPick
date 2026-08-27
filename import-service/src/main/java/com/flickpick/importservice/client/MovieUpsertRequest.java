package com.flickpick.importservice.client;

// Mirrors Ranking Service's MovieUpsertRequest. Deliberately duplicated
// rather than extracted into a shared module: a shared DTO jar would couple
// the two services' release cycles, which is the coupling the service split
// was meant to avoid. The cost is keeping two small records in step.
public record MovieUpsertRequest(Long tmdbId, String title, String posterUrl) {
}
