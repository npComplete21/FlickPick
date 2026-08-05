# Import Service – 00: Scaffolding

**Date:** 2026-08-05

Generated via Spring Initializr: Spring Boot 4.1.0, Java 21, Maven (with
wrapper). Package: `com.flickpick.importservice`.

**Dependencies:** web, actuator, data-jpa, postgresql, validation. No
`security` dependency yet — this service doesn't issue tokens, only
validates them when we wire up the Letterboxd/TMDb import flow later.

**Config (`application.yml`):** runs on port 8082, connects to
`localhost:5434/flickpick_import` (its own Postgres container). Same
`show-details: always` health config as User Service.

**Verified:** `./mvnw spring-boot:run` boots and connects to Postgres;
`GET /actuator/health` returns `200` with `"db":{"status":"UP"}`.
