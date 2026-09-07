# Ranking Service – 02: Rating-Seeded Ranking & Per-User Libraries

**Date:** 2026-09-07

## Why this step happened at all

It wasn't in the build order. Real imports made the comparison burden
visible: binary insertion costs `ceil(log2 i)` comparisons for the i-th
film, which compounds badly.

| films | comparisons | at 3s each |
|-------|-------------|------------|
| 8 (the old seed catalog) | 17 | 1 min |
| 49 (one RSS import) | 231 | 12 min |
| 1000 (a real library) | 8,977 | 7.5 hrs |

The 8 fake seed movies hid this completely. Building the export-ZIP importer
next — the "obvious" way to finish Step 4 — would have made it ~20x worse,
since it pulls a user's whole history rather than ~50 recent films. So the
ranking flow had to scale before more films were poured into it.

## What was built

**Ratings now narrow the binary-search window.** Import Service already
stored each film's Letterboxd rating but never sent it on. It now travels
with the movie push, and Ranking Service uses it to seed `PendingInsertion`'s
low/high bounds instead of starting at `(0, listSize)`. `PendingInsertion`
already carried those bounds across requests, so this was a change to where
the search *starts*, not a new algorithm.

**Measured on a real 49-film import:**

| scenario | comparisons | vs unseeded (231) |
|----------|-------------|-------------------|
| user's choices agree with their stars | 91 | −61% |
| user's choices contradict their stars | 69 | −70% |

## The design decision worth remembering

Ratings are a **hard constraint across rating bands, not a soft prior.**

The contrarian simulation proves it: a user who picks the *lower*-rated film
every single time still ends up with their 5.0s at the top and their 1.0 at
the bottom. Comparisons only reorder films *within* a band.

That is the price of the 61-70%. Someone whose taste has shifted since they
rated a film on Letterboxd cannot express it here — a film they starred 1.0
will never even be offered as a comparison against one they starred 5.0.
Accepted deliberately. Loosening it (biasing only the starting midpoint, or
widening the band so only films ≥1.5 stars apart are excluded) would restore
full expressiveness for a smaller saving.

## A bug the measurement uncovered, which mattered more than the feature

The first measurement returned only **13%**, not the ~59% predicted. The
cause was not tuning — it was a real product bug.

**The movie catalog is shared across all users** (that's what makes TMDb
dedup work), and the backlog query drew from the whole catalog. So a user was
being asked to rank films *other people* had imported and they had never
seen. The test user imported 49 films but was queued 69: their 49, the 8
seeds, and 12 films other users had imported. Those unrated films also broke
the seeding, since an unrated film ends a rating run and stops the bounds
from narrowing.

Fixed by introducing `UserLibraryEntry` — "this user has watched this film",
with an optional rating. The backlog is now scoped to the user's own library
(`findUnrankedInLibrary`), falling back to the seed catalog only for an
account that has imported nothing. This replaces the `tmdbId IS NULL`
ordering hack added in the previous step.

## Two more bugs found while verifying

**1. Position-shift could violate `(user_id, rank_position)`.** Latent on
`main` since the ranking service was written. `finalizeInsertion` shifts
every row down one to make room; `saveAllAndFlush` correctly forces the
UPDATEs ahead of the INSERT, but does **not** control ordering *among* the
UPDATEs. Shifting `[4,3,2] -> [5,4,3]` blows up if Hibernate emits `3->4`
before `4->5`. Iterating high-to-low only controls the order setters are
called, not the order the SQL is emitted.

Rating-seeding exposed it because many films now auto-resolve inside a
single `getNextComparison` loop, so shifts repeat within one persistence
context instead of being spread across separate HTTP requests. Fixed by
staging the shift through a disjoint negative range (`p -> -(p+2) -> p+1`),
which cannot collide regardless of flush order.

**2. The seed fallback latched.** `App.tsx` lands every new user on Compare,
so `next-comparison` fires *before* they have found the Import tab. With an
empty library the seed fallback kicked in, started a seed insertion, and
auto-placed the first seed with zero comparisons. Both survived the
subsequent import, so fake films sat permanently in a real ranking and kept
appearing as opponents.

Fixed with `evictSeedsOnceLibraryExists`: the moment a user has a library,
any in-flight seed insertion and any already-placed seed rows are removed and
the remaining positions closed up (staged through negatives, same reason as
above). The few comparisons already answered about seeds are discarded —
correct, since they were about films the user never claimed to have seen.

## Verified

- API simulation, 49-film import: 231 -> 91 comparisons (agree) and -> 69
  (contradict); contiguous positions 0-48, no duplicates, in both.
- Regression for the latch: sign up -> Compare (seeds appear) -> answer one
  -> import -> Compare. Result: no seed films in the final ranking.
- In-browser, same bad path end to end: final Rankings shows only the two
  imported films with posters. No console errors.

## Not done

- Export-ZIP importer (still the open half of Step 4) — now viable, since
  the comparison burden scales.
- Already-ranked seeds are evicted, but a user who ranked seeds and *never*
  imports keeps them, which is intended.
- The old `user_movie_ratings` table from an intermediate iteration of this
  work is left orphaned in dev databases by `ddl-auto: update`; harmless, and
  a real migration story is still owed.
