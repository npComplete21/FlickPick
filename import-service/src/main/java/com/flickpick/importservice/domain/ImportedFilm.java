package com.flickpick.importservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

// Import Service's own record of what a user has imported. This is not the
// ranking catalog — Ranking Service keeps its own copy of the films it needs
// (see MovieCatalogService there). Keeping a record here is what makes
// re-importing idempotent, and is the foundation the later "refresh diffing"
// step builds on: to know what's new in a feed, you have to know what you
// already saw.
//
// Like Ranking Service, this stores a plain userId rather than a foreign key
// to User — Import Service doesn't own user data and won't join across a
// network boundary for it.
@Entity
@Table(
        name = "imported_films",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tmdb_id"}))
public class ImportedFilm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @Column(nullable = false)
    private String title;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "watched_date")
    private LocalDate watchedDate;

    @Column(name = "member_rating")
    private BigDecimal memberRating;

    @Column(name = "source_username", nullable = false)
    private String sourceUsername;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    protected ImportedFilm() {
        // required by JPA
    }

    public ImportedFilm(
            Long userId,
            Long tmdbId,
            String title,
            Integer releaseYear,
            String posterUrl,
            LocalDate watchedDate,
            BigDecimal memberRating,
            String sourceUsername) {
        this.userId = userId;
        this.tmdbId = tmdbId;
        this.title = title;
        this.releaseYear = releaseYear;
        this.posterUrl = posterUrl;
        this.watchedDate = watchedDate;
        this.memberRating = memberRating;
        this.sourceUsername = sourceUsername;
        this.importedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public String getTitle() {
        return title;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public LocalDate getWatchedDate() {
        return watchedDate;
    }

    public BigDecimal getMemberRating() {
        return memberRating;
    }

    public String getSourceUsername() {
        return sourceUsername;
    }

    public Instant getImportedAt() {
        return importedAt;
    }
}
