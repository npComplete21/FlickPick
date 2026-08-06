package com.flickpick.rankingservice.dto;

import com.flickpick.rankingservice.domain.RankedMovie;
import java.math.BigDecimal;

public record RankingEntryResponse(
        Long movieId, String title, String posterUrl, int rankPosition, BigDecimal score) {

    public static RankingEntryResponse from(RankedMovie rankedMovie) {
        return new RankingEntryResponse(
                rankedMovie.getMovie().getId(),
                rankedMovie.getMovie().getTitle(),
                rankedMovie.getMovie().getPosterUrl(),
                rankedMovie.getRankPosition(),
                rankedMovie.getScore());
    }
}
