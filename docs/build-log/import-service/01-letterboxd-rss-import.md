# Import Service – 01: Letterboxd RSS Import

**Date:** 2026-08-18

**Endpoints built:** `POST /api/imports` (body `{letterboxdUsername}`) and
`GET /api/imports` (this user's import history). Both require a valid bearer
token — Import Service gained the `security` starter and the same
public-key JWT verification Ranking Service uses.

Also added to Ranking Service: `POST /api/movies`, which Import Service
calls to push films into the ranking catalog.

## The actual problem this step solves

Letterboxd parsing is the easy part. The real question was: **how does a
movie imported in one service become rankable in another, with no shared
database?** Ranking Service's queue reads its own `movies` table; Import
Service has its own DB on a different port.

**Chosen: Import Service pushes to Ranking Service.** Ranking Service stays
the owner of its catalog and its read path stays a local query, so
`next-comparison` is unaffected when Import Service is down. The cost is
movie metadata living in two databases.

**Rejected: Ranking Service pulls from Import Service.** One source of
truth, but it puts a network hop on the hot path and makes ranking
hard-fail whenever Import Service is unavailable — exactly the coupling the
DB-per-service split exists to prevent.

## Design decisions

- **No TMDb API key needed after all.** The plan called for TMDb resolution,
  which requires an account. Inspecting a real feed first showed Letterboxd
  already embeds `<tmdb:movieId>` (namespace `https://themoviedb.org`) on
  every film item — 100% coverage across the three feeds sampled. That gives
  canonical ids for dedup for free. A TMDb API key would now only buy richer
  metadata, not identity.
- **Dedup key is the TMDb id**, at both layers: `(user_id, tmdb_id)` unique
  in Import Service, and a unique `tmdb_id` on Ranking Service's `movies`.
  Title matching was the fallback plan and is much weaker — remakes share
  titles.
- **Import Service forwards the user's JWT** rather than holding a
  credential of its own, so Ranking Service authenticates the same human who
  asked for the import. One auth mechanism instead of two. The limit: this
  only works while acting on a live user request — a background refresh job
  would need a real service identity.
- **Duplicated `MovieUpsertRequest` record** in both services instead of a
  shared DTO module. A shared jar would couple their release cycles, which
  is what the service split was meant to avoid.
- **Seeded fake movies now sort last.** `findUnrankedForUser` orders
  `tmdbId IS NULL` last, so imported films come first. Without it the 8
  seeds hold ids 1-8 and anyone importing their history would rank 8 films
  they may never have seen before reaching their own. The seeder is kept
  (not deleted) so a brand-new user with no Letterboxd account still has
  something to rank.
- **XXE hardening on the parser.** The feed is third-party XML, so external
  DTD/schema resolution is disabled — otherwise a hostile feed could make
  the parser read local files or reach internal URLs.

## Things the real feed taught us (that guessing would have missed)

1. **Half of a Letterboxd feed isn't films.** 50 of 100 items are lists and
   other activity ("CHRISTOPHER NOLAN FILMS: RANKED", "Refn") carrying no
   `letterboxd:filmTitle`. Parsing naively would have imported list names as
   movies. Items are skipped unless they have both a film title and a TMDb
   id.
2. **`memberRating` is optional** (46 of 50 items in one feed) — plenty of
   watches are logged unrated, so it's nullable.
3. **The same film can appear twice** in one feed, since a rewatch is its
   own entry. The batch is collapsed by TMDb id before hitting the DB;
   otherwise the `(user_id, tmdb_id)` constraint would reject the whole
   import. In testing, a 50-film feed yielded 49 distinct films.

## One bug hit and fixed

`RestClient.Builder` is not an injectable bean here — no
`RestClientAutoConfiguration` ships in any Boot 4.1 jar this project's
starters pull in, so the service failed to start with "required a bean of
type 'RestClient$Builder' that could not be found". Replaced with
`RestClient.create()` from spring-web's own static factory, which has no
auto-configuration dependency to break. Same reasoning as the manual PEM
parsing in `JwtKeyConfig` (see `user-service/01-jwt-auth.md`).

## Verified

Via curl: import → 2 films found/2 new/2 added to catalog · re-import of the
same user → 0 new, 2 already known (idempotent) · `GET /api/imports` returns
full metadata · catalog rows confirmed in Postgres with TMDb ids and posters,
seeds still null.

In-browser: signed up fresh → Import tab → unknown username shows "No public
Letterboxd feed found for that username." → real username imports 49 films
with posters, years and ratings → Compare screen serves those films (with
real poster art, previously always blank) ahead of the seeds → Rankings shows
them ordered with scores. No console or network errors beyond the deliberate
404 test.

## Not done

- **Export ZIP importer** — RSS only for now, so imports are capped at the
  ~50 most recent films. The ZIP path needs multipart upload, ZIP/CSV
  handling and a frontend file picker.
- **Refresh diffing** — re-importing is idempotent, but there's no notion of
  "what's new since last time" beyond skipping known films. `ImportedFilm`
  exists partly to make that possible later.
- **Letterboxd ratings are ignored for ranking.** They're stored, but a
  user's existing star rating doesn't seed their position in the ranked
  list; every imported film still gets placed by pairwise comparison.
