package com.flickpick.importservice.dto;

// filmsFound  — watched films in the feed (feed items that were lists or
//               other activity are not counted)
// newFilms    — of those, ones this user hadn't imported before
// alreadyKnown— skipped as duplicates of a previous import
// addedToCatalog / updatedInCatalog — what Ranking Service reported back
public record ImportResultResponse(
        int filmsFound,
        int newFilms,
        int alreadyKnown,
        int addedToCatalog,
        int updatedInCatalog) {
}
