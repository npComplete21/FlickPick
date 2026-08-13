# Frontend – 01: Auth + Ranking Flow

**Date:** 2026-08-11

**Built:** Login/signup screen, pairwise-comparison screen, ranked-list
screen, wired against User Service and Ranking Service directly (no
gateway). `App.tsx` holds the JWT in state (persisted to `localStorage`) and
switches between views with a plain `useState` — no router, no data-fetching
library. Both were deliberate choices over `react-router-dom` and TanStack
Query: the app has three screens and the existing health-check page already
used plain `fetch`, so adding either would be a new pattern to learn without
a concrete problem it solves yet.

**Files added:**
- `src/api/{config,client,auth,ranking,types}.ts` — service base URLs, a
  small `ApiError`/`requestJson`/`requestVoid` fetch wrapper, and typed
  calls for signup/login/next-comparison/compare/rankings.
- `src/screens/{LoginScreen,CompareScreen,RankingsScreen,HealthScreen}.tsx`
  — `HealthScreen` is the old `App.tsx` body, extracted unchanged so the
  three new screens and the original one follow the same shape.

**Three real bugs hit and fixed:**
1. **`erasableSyntaxOnly` rejects constructor parameter properties.** This
   project's `tsconfig` restricts TS to syntax that erases cleanly to JS —
   `constructor(public readonly status: number, ...)` doesn't qualify.
   Rewrote `ApiError` with an explicit field + assignment in the
   constructor body instead.
2. **CORS preflight rejected with 401, reported by the browser as a CORS
   failure.** Both `RankingService` and `UserService`'s `SecurityConfig`
   had `anyRequest().authenticated()` with no exemption for `OPTIONS`.
   Spring Security's filter chain runs *before* Spring MVC's CORS handling
   (`WebConfig`), and a preflight `OPTIONS` request never carries the
   `Authorization` header — so the authenticated-only rule rejected the
   preflight itself, and the browser surfaced the failed real request as a
   generic CORS error instead of a 401. Fixed by permitting
   `HttpMethod.OPTIONS` on `/**` before the catch-all rule, in both
   services. Same shape of bug as the `/error`-forwarding issue documented
   in `docs/build-log/user-service/01-jwt-auth.md` — Security's filter
   chain intercepting a request Spring MVC was never meant to guard.
3. **Stale `ranking-service` public key.** `ranking-service/src/main/resources/certs/public.pem`
   predated this session (left over from a prior key generation) and didn't
   match a freshly generated `user-service` private key, so tokens minted
   by User Service failed verification. Fixed by re-copying User Service's
   current public key over — this is exactly the manual step
   `generate-keys.sh`'s comment describes ("Public key will eventually be
   copied to Import/Ranking Services"); there's no automation for it yet.

**Also fixed:** `.app-nav button.active { color: #111 }` was invisible —
`index.css` sets `color-scheme: light dark`, so the browser defaults to a
dark background, and near-black active-tab text disappeared against it.
Changed to the same blue already used for the signup/login toggle link,
which reads on both.

**Verified in-browser (Claude Browser tool, not just curl):** signup → lands
on Compare screen · repeated comparisons advance through the binary-insertion
queue with different pairs each time · Rankings screen shows the ordered
list with correct scores · Health tab still shows live service status ·
log out clears the token and returns to Login · log back in with the same
credentials succeeds · wrong password shows "Invalid email or password."
without crashing.

**Not done:** Import Service still has no real data, so the comparison pool
is still the 8 seed movies. No loading skeleton/spinner beyond a plain
"Loading…" string. No handling yet for a token that expires mid-session
beyond the existing 401 → logout path already wired into `CompareScreen`
and `RankingsScreen`.
