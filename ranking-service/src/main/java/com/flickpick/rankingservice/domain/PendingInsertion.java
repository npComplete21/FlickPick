package com.flickpick.rankingservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// The binary-search state for one in-progress "place this movie into the
// user's ranking" operation. Exists between requests specifically because
// the search takes multiple round trips — one HTTP request per comparison
// — and needs to remember its bounds in between, including across a user
// pausing and coming back later.
@Entity
@Table(name = "pending_insertions")
public class PendingInsertion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "low_bound", nullable = false)
    private int lowBound;

    @Column(name = "high_bound", nullable = false)
    private int highBound;

    protected PendingInsertion() {
        // required by JPA
    }

    public PendingInsertion(Long userId, Movie movie, int lowBound, int highBound) {
        this.userId = userId;
        this.movie = movie;
        this.lowBound = lowBound;
        this.highBound = highBound;
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

    public int getLowBound() {
        return lowBound;
    }

    public int getHighBound() {
        return highBound;
    }

    public void setLowBound(int lowBound) {
        this.lowBound = lowBound;
    }

    public void setHighBound(int highBound) {
        this.highBound = highBound;
    }

    public int mid() {
        return (lowBound + highBound) / 2;
    }

    public boolean isResolved() {
        return lowBound >= highBound;
    }
}
