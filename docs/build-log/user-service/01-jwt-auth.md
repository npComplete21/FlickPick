# User Service – 01: JWT Auth

**Date:** 2026-08-06

**Endpoints built:** `POST /api/auth/signup`, `POST /api/auth/login` (both
public), `GET /api/users/me` (requires a valid bearer token — built purely
to prove the auth chain works end to end, not because we need it yet).

**Design decisions:**
- **RS256, not HS256.** User Service holds the only private key and signs
  tokens; Import/Ranking Services will later hold only the public key to
  verify. No shared secret to distribute or leak across three services.
- **No refresh tokens yet.** Single access token, 24h expiry (dev-friendly).
  Real rotation/revocation is real complexity that doesn't teach anything
  new about *this* system's boundaries — deferred as deliberate future work.
- **Keys never committed.** Generated via a checked-in `generate-keys.sh`
  script into `src/main/resources/certs/`, which is gitignored. Production
  would use a secrets manager; this is the equivalent local-dev discipline.
- **Same error for "no such user" and "wrong password"** (`AuthService`),
  to avoid leaking which emails are registered via response differences.
- **Password hashing:** BCrypt via Spring Security's `PasswordEncoder`.

**Two real bugs hit and fixed:**
1. `RsaKeyConversionServicePostProcessor` (the intended way to load PEM keys
   as `@Value`-injected `RSAPrivateKey`/`RSAPublicKey`) no longer exists in
   Spring Security 7 (ships with Boot 4) — removed between major versions.
   Replaced with manual PEM parsing via plain JDK `KeyFactory` +
   `PKCS8EncodedKeySpec`/`X509EncodedKeySpec` (`JwtKeyConfig.java`) — more
   verbose, but has no framework-version dependency to break again.
2. Spring Boot's internal error handling forwards thrown exceptions to
   `GET /error` to render them. That forwarded request re-enters the *same*
   Spring Security filter chain as a fresh, unauthenticated request — our
   `anyRequest().authenticated()` rule was rejecting it, silently
   overwriting intended statuses (e.g. our 409 on duplicate signup) with an
   empty 403. Fixed by explicitly `permitAll()`-ing `/error`.

Also added a custom `authenticationEntryPoint` returning 401 instead of
Spring Security's default 403 for missing/invalid tokens — 403 is meant for
"authenticated but not permitted," 401 for "who even are you," and with no
login form/HTTP Basic configured, Security's out-of-the-box default is 403
for both.

**Verified (full matrix, all correct):** signup → 200 + token · duplicate
signup → 409 · wrong password → 401 · valid login → 200 + token ·
`/me` with valid token → 200 + correct user, no password hash exposed ·
`/me` with no token → 401 · `/me` with garbage token → 401 · malformed email
→ 400 (bean validation).
