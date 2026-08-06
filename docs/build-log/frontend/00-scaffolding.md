# Frontend – 00: Scaffolding

**Date:** 2026-08-06

Scaffolded via `npm create vite@latest frontend -- --template react-ts`.
Default template landing page replaced with a minimal `App.tsx` that fetches
`/actuator/health` from all three backend services on mount and renders a
status dot per service (checking/up/down), using a typed `Status` union and
a `ServiceCheck` interface — the same DTO-shape idea from Java, just on the
frontend. Dev server runs on the Vite default, port 5173.

**CORS gotcha hit and fixed:** added a `WebMvcConfigurer` CORS bean to each
service first, but `/actuator/health` still blocked the browser — actuator
endpoints are served through their own `WebMvcEndpointHandlerMapping`, not
the regular dispatcher, so standard `addCorsMappings()` doesn't cover them.
Fixed by adding `management.endpoints.web.cors.allowed-origins` to each
service's `application.yml` instead. The `WebMvcConfigurer` beans stay in
place for the real `@RestController` endpoints we build next.

**Verified:** all three services report `UP` on the rendered page, fetched
live across origins from the browser, with a full end-to-end trace done
covering the actuator handler mapping and each service's own DB health
check.
