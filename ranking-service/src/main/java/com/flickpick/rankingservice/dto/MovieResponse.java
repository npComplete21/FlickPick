package com.flickpick.rankingservice.dto;

import com.flickpick.rankingservice.domain.Movie;

public record MovieResponse(Long id, String title, String posterUrl) {

    public static MovieResponse from(Movie movie) {
        return new MovieResponse(movie.getId(), movie.getTitle(), movie.getPosterUrl());
    }
}
