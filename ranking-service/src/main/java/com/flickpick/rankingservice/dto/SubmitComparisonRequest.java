package com.flickpick.rankingservice.dto;

import jakarta.validation.constraints.NotNull;

public record SubmitComparisonRequest(@NotNull Long winnerMovieId) {
}
