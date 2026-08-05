# 02 – Backend Scaffolding & docker-compose

**Date:** 2026-08-05

All three services generated fresh via Spring Initializr rather than
hand-written, on Spring Boot 4.1.0 (the brief said 3.x, but 3.x is no longer
offered by Initializr at all — Boot 4 shipped Nov 2025. Since we're starting
from zero, none of Boot 4's ~115 breaking changes are a migration cost for
us; the one hard requirement, Java 21 minimum, already matched our setup).

**Port / DB assignment:**
| Service | App port | Postgres host port | DB name |
|---|---|---|---|
| user-service | 8081 | 5433 | flickpick_user |
| import-service | 8082 | 5434 | flickpick_import |
| ranking-service | 8083 | 5435 | flickpick_ranking |

**docker-compose.yml** runs only the three Postgres instances, one container
each, on distinct host ports — not the services themselves. Services run
natively via each `./mvnw spring-boot:run`, connecting to `localhost:<port>`.
This follows directly from the earlier decision to install Node/use the
Maven wrapper for a fast native dev loop; we'll containerize the services
themselves (via Dockerfiles) as a separate step when preparing for
deployment, not now.

**Bug hit and fixed:** Initializr's version metadata listed Boot 4.1 as
`4.1.0.RELEASE`, which isn't a real Maven Central artifact — the `.RELEASE`
suffix was dropped from actual published versions back at Boot 2.4. Fixed by
correcting the parent version to `4.1.0` in all three `pom.xml` files.

**Verified:** all three services boot via their wrapper, connect to their
own Postgres container, and return `200` from `/actuator/health` with the
`db` component `UP`.
