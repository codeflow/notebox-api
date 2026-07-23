# Plan — Login & tenant-context UI (ADF Fusion theme)

**ID:** features/002-identity-tenancy-web
**User Story:** US-6.1 · **FR:** FR-17 · **Project:** notebox-web (satellite; react/next)
**Version:** v1 · **Status:** Draft · **Date:** 2026-07-22

## Origin
- **Spec:** [`features/002-identity-tenancy-web/spec.md`](spec.md) (approved 2026-07-22).
- **US / FR:** US-6.1 (a tenant member's data is isolated behind auth) — this feature is its **client half**:
  prove identity, carry the stateless token, resolve tenant **from `/me`**, never from input. Covers **FR-17**
  (client obligations of multi-tenant identity & isolation).
- **BRs bound:** BR-01 (client never chooses/sends a tenant), BR-02 (no tenant selector surfaced),
  BR-08 (no untranslated user-facing text).
- **Consumed contract (FIXED, no change):** `features/001-identity-tenancy/contracts/` — `POST /api/auth/login`
  (public → `{token, expiresAt}`), `GET /api/me` (Bearer → `{userId, tenantId, role, displayName, locale}`),
  the `Problem` envelope with the four 401 auth codes; `Problem.message` is **already localized** server-side.
- **Governing architecture:** AD-06 (UI is a separate deployable consuming synchronous REST/JSON) + BR-08/C-09
  (en + pt, never a raw key). The Java code-standards (`constitution/03`) govern the API repo only; this plan
  establishes the **equivalent web conventions**, since this is the first code in `notebox-web`.

## Context this plan resolves
`notebox-web` is **completely un-scaffolded** — it holds only the workflow harness (`.claude/`, `bin/`,
`workflow.json`, doc stubs). No `package.json`, `src/`, Next app, test runner, or lint/format config. **This
feature bootstraps the entire web project.** There is no existing React convention in the workspace to match —
this plan sets them.

## Approach (three sentences)
Bootstrap a Next.js (App Router) + TypeScript project with the ADF Fusion global stylesheet, a client-side
auth session (stateless JWT from feat-001 kept in `sessionStorage` behind a `TokenStore` seam, attached as
`Bearer` by a single `authFetch` wrapper, cleared on any 401 or logout, validated on boot via `/me`), and a
route guard splitting a public `/login` from a protected `(app)` group. The login screen validates client-side
before calling the API, shows the server's already-localized `Problem.message` verbatim on failure, and on
success renders the member's identity and tenant in the ADF branding bar sourced **only** from `/me`.
All UI reuses the `.af-*` classes verbatim; localization is a tiny hand-rolled en/pt dictionary.

### 1. Project bootstrap
- **Next.js — App Router** (not Pages). Route groups give a clean public/protected split; the root
  `layout.tsx` is the single place to import the global ADF stylesheet. Auth is browser-only, so auth-gated
  trees are Client Components (`'use client'`); the API is never called from a Server Component (no SSR of
  tenant data → no hydration mismatch, and AD-06's boundary stays clean).
- **TypeScript** `strict: true`; all API shapes and session state typed.
- **Testing — Vitest + React Testing Library + user-event + jsdom + MSW.** MSW mocks the feat-001 API at the
  network boundary so each Gherkin scenario becomes a real integration test (login 200/401, `/me` 200/401 with
  specific `Problem.code`s). Vitest over Jest: ESM/TS-native, less transform config.
- **Lint/format — ESLint (`next lint`) + Prettier.**
- **Config from env (C-05):** `NEXT_PUBLIC_API_BASE_URL`, documented in `.env.example`; no secrets in repo
  (the client holds no signing key — it never verifies the JWT).

### 2. App structure / routes
```
app/
  layout.tsx                 # root: imports adf-fusion.css + overrides; wraps <Providers>
  providers.tsx              # 'use client' — I18nProvider > AuthProvider
  (auth)/login/page.tsx      # PUBLIC — login screen
  (app)/layout.tsx           # 'use client' — RouteGuard + BrandingBar (ADF chrome)
  (app)/page.tsx             # authenticated home (minimal landing proving session/tenant)
```
Public route: `/login`. Everything under `(app)` is protected; `(app)/layout.tsx` mounts the guard and the
ADF chrome once, so every future protected screen inherits both.

### 3. Login screen
- ADF markup: centered `.af-panelBox.core` holding an `.af-form` (4-col grid) with `.af-label.req` rows for
  email and password, an `.af-buttonBar > .af-button` submit; form-level errors via `.af-messages`,
  field-level via `.af-error`/`.af-noteWindow`.
- **Client-side validation before any request:** required email + password, email shape; localized field
  messages via `t(key)`. No request is sent if invalid.
- **In-flight control:** submit disabled while the login promise is pending; no double submit.
- **On 200:** store `{token, expiresAt}`, call `/me`, populate session, redirect to intended destination (or
  `(app)` home). **On 401 `AUTH_INVALID_CREDENTIALS`:** stay, render `Problem.message` verbatim, no session.
  **On 5xx / network (no `Problem` body):** render local `login.error.generic`, no session.

### 4. API client (`lib/api/`)
- `apiClient.login(creds)` — POST `/auth/login`, no bearer (public); parses `LoginResponse`; throws
  `ApiError(problem, status)` on non-2xx.
- `apiClient.getMe()` — GET `/me` through `authFetch`.
- `authFetch(input, init?)` — the single authenticated wrapper: attaches `Authorization: Bearer <token>` from
  `TokenStore`, sends **no client-supplied tenant identifier** (C-01), parses `Problem` into `ApiError`, and on
  any **401** notifies a `sessionExpired` listener the `AuthProvider` observes to clear + redirect. This is the
  central choke point for AUTH_REQUIRED / AUTH_TOKEN_EXPIRED / AUTH_TOKEN_INVALID.

### 5. Session / token lifecycle (`lib/auth/`)
- **In-memory working copy** (context) is what every request reads; **persisted to `sessionStorage`** (behind
  `TokenStore`) only to survive a reload; rehydrated on boot then **validated by calling `/me`** before
  admitting the user. Cleared on any 401 and on logout; a token past `expiresAt` is treated as dead
  client-side (never sent). **Replace-on-login:** a new login fully replaces prior `Me`/session — no tenant/
  identity carried across logins (BR-01).

### 6. Route guard (`components/RouteGuard`)
Client-side. While `status === 'unknown'` (boot rehydrate + `/me` in flight) it renders a hydration-stable ADF
placeholder. Then: unauthenticated + protected → redirect `/login?redirect=<intended>`; authenticated +
`/login` → redirect `(app)` home; authenticated + protected → render children.

### 7. Tenant context (ADF branding bar)
`BrandingBar` renders in `(app)/layout.tsx` chrome via `.af-branding`/`.af-globalLinks`: **"Conectado como
&lt;displayName&gt;"** + tenant context, read **only** from `useAuth().me` (from `/me`), never from URL/form;
a tenant id in input is ignored (C-01). Includes a "Sair" logout link.

### 8. Logout
`useAuth().logout()` clears `TokenStore` + in-memory session and routes to `/login`; a back-navigation to a
protected route then redirects to login.

### 9. Localization (`lib/i18n/`)
Hand-rolled `{ en, pt }` dictionary + `I18nProvider` exposing `t(key, vars?)`, `locale`. Resolved locale:
`Me.locale` when signed in, else `navigator.language` (pt→pt, else en), default **en**. `t()` never returns a
raw key/blank (missing key → English fallback + dev warning). **Server `Problem.message` is shown verbatim** —
never re-keyed or re-translated.

### 10. ADF theme integration
**Copy `adf-fusion.css` UNCHANGED** into `src/styles/adf-fusion.css`; project tweaks go in a separate
`src/styles/adf-fusion.overrides.css` (never fork the base). Both imported once in `app/layout.tsx`; components
reuse `.af-*` verbatim (no Tailwind/CSS-in-JS rewrite, per the skill's framework-adaptation rule).

## Alternatives rejected
- **Token in `localStorage` (rejected) vs `sessionStorage` (chosen).** Both are equally XSS-readable, so XSS
  does not distinguish them; the deciding factor is **lifetime**. localStorage persists across restarts and
  every tab — it would retain a token beyond "until expiry", widening the theft window. sessionStorage is
  tab-scoped, cleared on tab close, and still survives the same-tab reload the spec requires. Cost: a new tab
  needs re-login (spec mandates only *reload* survival). Swappable behind `TokenStore`.
- **httpOnly cookie / BFF (rejected).** Best XSS posture, but the **fixed** feat-001 contract returns the token
  in a JSON body and authenticates via the `Authorization: Bearer` header — it never issues/reads a cookie. A
  cookie would need a Next server-side proxy to receive the token, set the cookie, and re-attach it as Bearer —
  a new stateful tier that strains AD-06. Not justified for v1; the `TokenStore` seam keeps the door open.
- **Pages Router / plain Vite+React (rejected).** Stack is fixed to react+next (Vite out). Pages Router works
  but App Router's route groups give the cleanest public/protected split and a single global-CSS mount; SSR is
  irrelevant (auth is client-only), so App Router's server-first default costs nothing once auth trees are
  `'use client'`.
- **Redux / Zustand / React Query (rejected).** One session object plus two calls; React Context + `authFetch`
  suffices and is period-appropriate. A server-cache library is premature until there are many read endpoints.
- **i18next / react-intl (rejected).** Two static locales + verbatim server messages need no ICU/async
  machinery; a tiny dictionary + context is smaller and fully covers BR-08/C-09.

## Blast radius
Greenfield in `notebox-web`; initial file tree created:
```
package.json  tsconfig.json  next.config.mjs  vitest.config.ts
eslint.config.mjs  .prettierrc  .env.example
app/layout.tsx  app/providers.tsx
app/(auth)/login/page.tsx
app/(app)/layout.tsx  app/(app)/page.tsx
components/RouteGuard.tsx  components/BrandingBar.tsx  components/LoginForm.tsx
lib/api/apiClient.ts  lib/api/authFetch.ts  lib/api/types.ts  lib/api/ApiError.ts
lib/auth/AuthProvider.tsx  lib/auth/TokenStore.ts  lib/auth/session.ts
lib/i18n/I18nProvider.tsx  lib/i18n/messages/en.ts  lib/i18n/messages/pt.ts  lib/i18n/useTranslation.ts
lib/validation/loginValidation.ts
src/styles/adf-fusion.css (copied verbatim)  src/styles/adf-fusion.overrides.css
test/  (MSW handlers, setup, *.test.tsx per Gherkin group)
```
Modified: `README.md` (running-locally), `.gitignore` (already covers `node_modules/`, `.env*`).
**Consumes feat-001's contract only — no API change.**

**Infra decision (harness):** the satellite `workflow.json` has **no harness block**; the hub's `verify` is
`mvn -B verify` (the API's). Add a satellite harness so `verify_green` has something real to run here,
mirroring the hub's signal names `[lint, typecheck, test, build]`:

| Signal | Command |
|---|---|
| `lint` | `npm run lint` (`eslint . && prettier --check .`) |
| `typecheck` | `npm run typecheck` (`tsc --noEmit`) |
| `test` | `npm run test` (`vitest run`) |
| `build` | `npm run build` (`next build`) |
| **verify** | `npm run verify` = `npm run lint && npm run typecheck && npm run test && npm run build` |

## Risk
- **Token-in-sessionStorage XSS.** Injected script could read the token. Mitigations: React default escaping
  (this feature renders no untrusted HTML — C-08 N/A), never log the token (INV-W3), short server token
  lifetime, `TokenStore` seam to migrate to cookie/BFF later. **Signal:** storage/logging review + a test that
  the token never appears in console/URL.
- **Tenant leakage across logins.** A cached `Me` surviving re-login could show tenant A to tenant B.
  Mitigation: replace-on-login (INV-W5), clear-on-logout. **Signal:** integration test — login A, logout,
  login B → branding shows B only.
- **Locale gaps / raw key on screen.** A missing `pt` key violates BR-08. **Signal:** per-locale key-coverage
  test (en/pt keysets identical) + `t()` never-returns-key contract; assert `Problem.message` is not
  re-translated.
- **SSR/hydration of an auth-gated app.** Reading sessionStorage during render would mismatch server HTML.
  Mitigation: token read only in `useEffect`; guard renders a stable placeholder until `status` resolves.
  **Signal:** `next build` + a hydration test with no console hydration warnings.
- **Known-expired token sent.** Mitigation: client-side `now >= expiresAt` check before send. **Signal:** test
  that an expired session redirects to login with `session.expired` without hitting the network.

## Reversibility
- **Next App Router — one-way.** Restructuring to Pages Router/another framework is a routing + layout rewrite.
  Justified by the route-group split and single global-CSS mount.
- **Public-route & URL structure (`/login` public, `(app)/*` protected, `?redirect=`) — one-way-ish.** Becomes
  an external contract (bookmarks, deep links). Fix the shape now.
- **Token storage — reversible in code, but a real security decision.** Swappable behind `TokenStore`;
  sessionStorage-vs-cookie is the consequential choice, chosen given the fixed Bearer contract.
- **verify contract — one-way-ish.** The signal names `[lint, typecheck, test, build]` + `npm run verify`
  become the gate the hub runs; establish deliberately, matching the hub's vocabulary.
- **Reversible:** i18n dictionary, React Context state, Vitest/MSW stack, ADF overrides file — all swappable
  without cross-cutting churn.
