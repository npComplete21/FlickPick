# User Service – 00: Scaffolding

**Date:** 2026-08-05

Generated via Spring Initializr (`start.spring.io/starter.zip`): Spring Boot
4.1.0, Java 21, Maven (with wrapper). Package: `com.flickpick.userservice`.

**Dependencies:** web, actuator, data-jpa, postgresql, security, validation.
Security is included now (even though no login endpoint exists yet) since
JWT auth is this service's next build step and we wanted its default-secured
behavior visible from the start.

**Config (`application.yml`):** runs on port 8081, connects to
`localhost:5433/flickpick_user` (its own Postgres container, see root
`docker-compose.yml`). `management.endpoint.health.show-details: always` so
the DB connectivity component is visible in `/actuator/health`.

**Verified:** `./mvnw spring-boot:run` boots, connects to Postgres (Hikari
pool + Hibernate both log successful connection), and
`GET /actuator/health` returns `200` with `"db":{"status":"UP"}` —
unauthenticated, because Spring Security's auto-config leaves health/info
endpoints open by default even though every other endpoint will require
login once we add one. No real auth logic yet — that's the next step.
