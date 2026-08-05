# Ranking Service – 00: Scaffolding

**Date:** 2026-08-05

Generated via Spring Initializr: Spring Boot 4.1.0, Java 21, Maven (with
wrapper). Package: `com.flickpick.rankingservice`.

**Dependencies:** web, actuator, data-jpa, postgresql, validation. Same
reasoning as Import Service — no `security` dependency yet, since this
service will validate incoming JWTs later rather than issue them.

**Config (`application.yml`):** runs on port 8083, connects to
`localhost:5435/flickpick_ranking` (its own Postgres container). Same
`show-details: always` health config as the other two services.

**Verified:** `./mvnw spring-boot:run` boots and connects to Postgres;
`GET /actuator/health` returns `200` with `"db":{"status":"UP"}`.
