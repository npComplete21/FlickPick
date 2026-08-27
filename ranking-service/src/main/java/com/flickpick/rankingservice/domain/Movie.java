package com.flickpick.rankingservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Canonical TMDb id, used as the dedup key when Import Service pushes
    // films in. Nullable because the seeded stand-in catalog predates any
    // import and has no TMDb identity — Postgres allows repeated NULLs
    // under a unique constraint, so those rows don't collide with each
    // other. Deliberately not the primary key: Ranking Service's own ids
    // are already referenced by RankedMovie/PendingInsertion rows.
    @Column(name = "tmdb_id", unique = true)
    private Long tmdbId;

    @Column(nullable = false)
    private String title;

    @Column(name = "poster_url")
    private String posterUrl;

    protected Movie() {
        // required by JPA
    }

    public Movie(String title, String posterUrl) {
        this.title = title;
        this.posterUrl = posterUrl;
    }

    public Movie(Long tmdbId, String title, String posterUrl) {
        this.tmdbId = tmdbId;
        this.title = title;
        this.posterUrl = posterUrl;
    }

    public Long getId() {
        return id;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public String getTitle() {
        return title;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }
}
