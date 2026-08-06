# 03 – docker-compose Project Naming & Worktrees

**Date:** 2026-08-06

`docker-compose.yml` declares no explicit project name, so Compose defaults
to the basename of the directory `docker compose up` is run from — that's
why containers are named `initial-poc-user-db-1` etc. (this worktree's
folder is `initial-poc`).

**Why this matters for a multi-worktree setup:** running `docker compose up`
from a different directory (e.g. the main `FlickPick` checkout, a different
worktree) creates a *separate* project — different container names,
different named volumes, so a different (initially empty) set of Postgres
data — even with an identical compose file. On top of that, our host ports
(5433/5434/5435) are hardcoded, so two different-directory projects still
can't run simultaneously regardless of naming: whichever starts second gets
a port-already-allocated error.

**Decision:** always run `docker compose up -d` from this worktree
(`.claude/worktrees/initial-poc`) specifically, not from the main checkout
or any other worktree, so we consistently reuse the same containers/volumes
rather than accidentally spinning up a fresh, empty database elsewhere.
