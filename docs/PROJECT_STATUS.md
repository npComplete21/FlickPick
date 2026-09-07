# FlickPick — Project Status

_Last updated: 2026-09-07_

## Where we are

The walking skeleton runs on **real data**: sign up, import a Letterboxd
history, rank those actual films through pairwise comparisons (with real
poster art), and see a scored ranked list — in the browser, across all three
services.

Step 4's RSS import is merged (PR #6). An unplanned follow-up — rating-seeded
ranking — is built but **not committed**, on branch
`feature/rating-seeded-ranking`.

## Build order progress

- [x] Step 1: Scaffolding — merged (PR #1, #2)
- [x] Step 2: User Service auth — merged (PR #3)
- [x] Step 3: Ranking Service — backend merged (PR #4); frontend flow merged (PR #5)
- [~] Step 4: Import Service — Letterboxd **RSS** import merged (PR #6);
  **export-ZIP importer not built**
- [~] Unplanned: rating-seeded ranking + per-user libraries — built,
  uncommitted on `feature/rating-seeded-ranking`
- [ ] Step 5: Depth pass (pause/resume UX, refresh diffing, following-ready schema)
- [ ] Step 6 (future, not yet): API Gateway

## What's done (merged to main)

- **Monorepo** with one folder per deployable (frontend, user-service,
  import-service, ranking-service), each independently buildable/deployable
  via its own pom.xml/Dockerfile — see docs/build-log/00-repo-structure-and-build-order.md
- **Three Spring Boot 4.1 services + React/TS frontend**, each backend
  service on its own Postgres instance via docker-compose — see
  docs/build-log/02-backend-scaffolding-and-docker-compose.md and
  docs/build-log/frontend/00-scaffolding.md
- **User Service**: signup, login, `GET /api/users/me` (protected). JWTs
  signed with RS256 — User Service holds the private key, other services
  only ever get the public key, so only User Service can ever mint a token
  — see docs/build-log/user-service/01-jwt-auth.md
- **Ranking Service**: binary-insertion placement of a new movie into a
  user's sorted ranking (O(log N) comparisons via `PendingInsertion`
  tracking search bounds across requests, which is what makes pause/resume
  possible). Rank position → 0.0-5.0 score via percentile-to-tier bucketing
  (rejected pure linear scaling — too harsh at the bottom of a list). JWT
  verified via User Service's public key, zero network calls back to User
  Service. 8 fake seed movies stand in for real data until Import Service
  exists — see docs/build-log/ranking-service/01-comparison-queue-and-scoring.md
- **Frontend auth + ranking flow**: login/signup, pairwise comparison, and
  ranked-list screens, plus a typed `api/` layer. View switching is plain
  `useState` and data fetching is plain `fetch` — deliberately no router and
  no query library for three screens. JWT lives in `localStorage`; a 401
  from either service logs the user out. See
  docs/build-log/frontend/01-auth-and-ranking-flow.md

- **Import Service**: `POST /api/imports` pulls a public Letterboxd RSS feed,
  parses out watched films, stores them, and **pushes** them into Ranking
  Service's catalog via `POST /api/movies` — forwarding the user's own JWT
  rather than holding a service credential. Dedup is by TMDb id, which the
  RSS feed supplies directly (`<tmdb:movieId>`), so no TMDb API key is
  needed. Chose push over pull so the comparison queue stays a local query
  and doesn't fail when Import Service is down. See
  docs/build-log/import-service/01-letterboxd-rss-import.md

## In progress / not yet done for the current step

Step 4 is functional but partial: **only the RSS importer exists**, so
imports are capped at a user's ~50 most recent films. The export-ZIP
importer (full history) would need multipart upload, ZIP/CSV parsing, and a
frontend file picker.

Also unbuilt: refresh diffing (re-import is idempotent but has no "what's
new since last time").

Uncommitted on `feature/rating-seeded-ranking`: imported star ratings now
narrow the binary-search window (231 → 91 comparisons on a real 49-film
import, −61%), plus `UserLibraryEntry` scoping so a user only ranks films
*they* imported rather than the whole shared catalog. Verified end to end;
see docs/build-log/ranking-service/02-rating-seeded-ranking.md for the
measurements and the three bugs it surfaced.

## Immediate next action

Commit and open a PR for `feature/rating-seeded-ranking`, then decide
between building the export-ZIP importer to finish Step 4 (now viable, since
the comparison burden scales) or moving to Step 5's depth pass.

## Gotcha worth knowing before running locally

`user-service`'s RSA keys are gitignored and generated per-machine via
`user-service/generate-keys.sh`. `ranking-service`'s `public.pem` **is**
committed, so regenerating User Service's keypair silently breaks token
verification in Ranking Service until you re-copy the public key over. This
already caused a debugging detour once (see the frontend build log). Import
Service will need the same public key once it verifies JWTs — worth
automating at that point rather than hand-copying to a second service.

## Open decisions / deliberately deferred

- **Refresh tokens**: not built. Access tokens are a single 24h-lived JWT
  with no rotation/revocation. Flagged explicitly as future work once the
  rest of the system exists.
- **API Gateway**: deliberately not built yet — frontend calls each service
  directly. Planned as a deliberate future milestone (likely Spring Cloud
  Gateway) once all three services work end-to-end.
- **Following/social schema**: User Service's data model is meant to not
  make this painful later, but this has not actually been stress-tested by
  building the feature.
- **Async/message queue**: explicitly not adopted — services communicate
  over plain REST for now, to be revisited only if a concrete case makes
  REST clearly wrong.

## Key design decisions worth remembering

- **Monorepo over multi-repo** — service boundaries enforced by
  folder/build/DB separation, not git repo boundaries; multi-repo's benefit
  (independent team release cadences) doesn't apply solo. See
  docs/build-log/00-repo-structure-and-build-order.md
- **Walking-skeleton build order over bottom-up** — thin, real, end-to-end
  slice first (auth → fake-data ranking) rather than finishing each service
  in isolation, so cross-service integration issues (the actual learning
  goal) surface early. Same doc as above.
- **RS256 over HS256 for JWT signing** — only User Service can ever issue a
  token; every other service verifies with a public key it's safe to
  distribute freely. See docs/build-log/user-service/01-jwt-auth.md
- **Percentile-to-tier score bucketing over linear scaling** — keeps the
  middle of someone's ranked list in a respectable score range instead of a
  mathematically "correct" but harsh one. See
  docs/build-log/ranking-service/01-comparison-queue-and-scoring.md
- **Ranking Service stores a plain `userId`, not a foreign key to `User`**
  — Ranking Service doesn't own User data and won't join across a network
  boundary for it; it trusts a valid JWT instead.
