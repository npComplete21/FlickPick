# 00 – Planning: Repo Structure & Build Order

**Date:** 2026-08-05

## Decision: Monorepo
One repo, one subfolder per deployable (frontend, user-service, import-service,
ranking-service). Each service keeps its own pom.xml, Dockerfile, and Postgres
instance — deployability is enforced by folder/build/DB separation, not by git
repo boundaries.

**Alternative considered:** one repo per service (closer to real microservice
orgs). Rejected: that pattern mainly isolates different *teams'* release
cadences, which doesn't apply solo, and would add real cross-repo PR friction
for zero learning benefit here.

## Decision: Build order — walking skeleton
1. Scaffolding (health checks, docker-compose Postgres, frontend shell)
2. User Service: real JWT auth
3. Ranking Service: comparison queue + binary-insertion ranking, seeded with
   fake movies (no real import yet)
4. Import Service: Letterboxd RSS + export ZIP importers, TMDb resolution,
   wired into step 3's queue
5. Depth pass: pause/resume, refresh diffing, following-ready schema
6. Future, not built now: API Gateway

**Alternative considered:** bottom-up (finish each service fully before
starting the next). Rejected: the main learning goal is inter-service
communication and network boundaries, and bottom-up defers exactly that to
the end, where integration issues are hardest to trace.
