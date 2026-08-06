package com.flickpick.rankingservice.service;

import com.flickpick.rankingservice.domain.Movie;
import com.flickpick.rankingservice.repository.MovieRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Stand-in for real Letterboxd/TMDb data, which Import Service doesn't
// exist yet to provide. Seeds a fixed catalog once, on first startup, so
// the ranking flow (comparison queue + binary insertion) can be built and
// tested end-to-end before real ingestion is wired up.
@Component
public class MovieSeeder implements CommandLineRunner {

    private static final List<String> SEED_TITLES = List.of(
            "The Godfather",
            "Spirited Away",
            "Parasite",
            "Mad Max: Fury Road",
            "The Grand Budapest Hotel",
            "No Country for Old Men",
            "Whiplash",
            "Everything Everywhere All at Once");

    private final MovieRepository movieRepository;

    public MovieSeeder(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
    public void run(String... args) {
        if (movieRepository.count() > 0) {
            return;
        }
        SEED_TITLES.forEach(title -> movieRepository.save(new Movie(title, null)));
    }
}
