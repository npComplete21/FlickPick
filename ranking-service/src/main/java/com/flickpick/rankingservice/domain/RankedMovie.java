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

@Entity
@Table(
        name = "ranked_movies",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "rank_position"}),
                @UniqueConstraint(columnNames = {"user_id", "movie_id"})
        })
public class RankedMovie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Not a foreign key — User lives in a different service's database.
    // We trust the JWT subject claim rather than joining across a network
    // boundary to confirm the user exists.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "rank_position", nullable = false)
    private int rankPosition;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal score;

    protected RankedMovie() {
        // required by JPA
    }

    public RankedMovie(Long userId, Movie movie, int rankPosition, BigDecimal score) {
        this.userId = userId;
        this.movie = movie;
        this.rankPosition = rankPosition;
        this.score = score;
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

    public int getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(int rankPosition) {
        this.rankPosition = rankPosition;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }
}
