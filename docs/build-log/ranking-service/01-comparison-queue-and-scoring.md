# Ranking Service – 01: Comparison Queue, Binary Insertion, Scoring

**Date:** 2026-08-06

Built the core ranking flow: `PendingInsertion` tracks one in-progress
binary-search placement per user (survives across requests, so pause/resume
works for free). `GET /next-comparison` returns the current comparison, or
starts the next unranked movie, or reports nothing left. `POST /compare`
narrows the search bounds. Resolution (`low >= high`) is handled lazily on
the *next* `next-comparison` call, not inline in `compare()`.

**JWT verification:** copied User Service's public key (safe to commit,
unlike the private key) into `ranking-service/src/main/resources/certs/`.
Same `JwtAuthenticationFilter`/`SecurityConfig` pattern as User Service,
minus any signing capability — this service can verify a token came from
User Service but can never mint one itself.

**Scoring:** implemented the percentile-to-tier bucketing formula agreed on
beforehand (5 bands, linear interpolation within each band). Verified by
hand-checking one value (N=3, middle position → percentile 0.5 → 3.3) against
the actual output.

**Bug hit and fixed:** inserting a movie at position 0 while shifting
existing entries out of the way threw a unique-constraint violation on
`(user_id, rank_position)`. Cause: Hibernate executes all INSERTs before any
UPDATEs within a single flush, regardless of code call order — so the new
row landed at position 0 before the shift's UPDATE had vacated it. Fixed by
calling `saveAllAndFlush()` on the shift before the insert, forcing the
UPDATEs to hit the DB first.

**Verified:** scripted a full 8-movie ranking session against a fixed
"true preference" order, answering every comparison consistently. Final
order matched the preference list exactly; 16 total comparisons for 8
movies (close to the O(N log N) expectation for binary-insertion).
