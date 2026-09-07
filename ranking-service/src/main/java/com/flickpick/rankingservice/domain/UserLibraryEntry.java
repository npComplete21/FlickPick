package com.flickpick.rankingservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

// "This user has watched this film" — one row per user per film they
// imported, with the rating they gave it elsewhere (Letterboxd) when they
// gave one.
//
// This is what scopes a user's ranking queue. The `movies` catalog is shared
// across everyone so that dedup by TMDb id works, which means it also
// contains films imported by *other* people; without this table a user would
// be asked to rank films they have never seen.
//
// The rating is only ever a *prior* on where a film probably belongs, used
// to narrow the binary-insertion search. The final ordering always comes
// from the user's own comparisons.
//
// Why not fold this into existing tables:
//   - not on Movie, because the catalog is shared and this is per person
//   - not on RankedMovie, because that row only exists once a film has been
//     placed, and this exists precisely to decide what to place next
@Entity
@Table(
        name = "user_library_entries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "movie_id"}))
public class UserLibraryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    // Nullable: plenty of Letterboxd entries are logged watched but unrated.
    // Such a film is still in the library and still gets ranked — it just
    // starts with the full search window.
    @Column
    private BigDecimal rating;

    protected UserLibraryEntry() {
        // required by JPA
    }

    public UserLibraryEntry(Long userId, Movie movie, BigDecimal rating) {
        this.userId = userId;
        this.movie = movie;
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Movie getMovie() {
        return movie;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }
}
