# Tasks — Login & tenant-context UI (ADF Fusion theme)

**Feature:** features/002-identity-tenancy-web · US-6.1 · FR-17 · BR-01, BR-02, BR-08 · @notebox-web
**Plan:** [`plan.md`](plan.md) (approved 2026-07-22). Decomposition only — the design is decided.
Every task ships its own tests and cites the Gherkin scenario it makes pass.
Verify signal (established by T-01): `npm run verify` = `lint && typecheck && test && build`; per-task narrowed with
`npm run test -- <path>`.

Ordered by dependency, then risk (the task most likely to invalidate the plan goes early). Scenario labels
reference [`spec.md`](spec.md): **A** = "A member signs in", **B** = "The signed-in session carries tenant
context", **C** = "Protected screens require a live session", **D** = "Login and session text is localized".

- [x] **T-01 · Scaffold the Next.js + TypeScript app, ADF stylesheet & verify harness**
      - files: `package.json`, `tsconfig.json`, `next.config.mjs`, `vitest.config.ts`, `eslint.config.mjs`,
        `.prettierrc`, `.env.example`, `app/layout.tsx` (imports the ADF css), `app/providers.tsx` (skeleton),
        `src/styles/adf-fusion.css` (copied **verbatim** from the skill), `src/styles/adf-fusion.overrides.css`,
        `test/setup.ts`; **satellite** `../notebox-web/workflow.json` harness block (`verify` + signals)
      - covers: plan §1, §10 (foundation enabler for all scenarios); establishes `npm run verify`
      - depends: —
      - parallel: no (owns `package.json` + shared config; every task builds on it)
      - verify: `npm run verify` (empty app assembles: lint + typecheck + test(0) + build green)

- [x] **T-02 · i18n provider (en/pt) with never-key resolution & coverage**
      - files: `lib/i18n/messages/en.ts`, `lib/i18n/messages/pt.ts`, `lib/i18n/I18nProvider.tsx`,
        `lib/i18n/useTranslation.ts`, `lib/i18n/i18n.test.ts`
      - covers: BR-08, C-09 · scenario: D "The login screen renders in the resolved locale" (locale resolution +
        `t()` never returns a raw key/blank); test asserts **en/pt keysets are identical**
      - depends: T-01
      - parallel: yes
      - verify: `npm run test -- lib/i18n`

- [x] **T-03 · Login field validation (pure)**
      - files: `lib/validation/loginValidation.ts`, `lib/validation/loginValidation.test.ts`
      - covers: scenarios A "Empty required fields block submission before any request", A "Malformed email is
        flagged before any request" — returns localizable message keys, sends no request
      - depends: T-01
      - parallel: yes
      - verify: `npm run test -- lib/validation`

- [x] **T-04 · TokenStore (sessionStorage persistence seam)**
      - files: `lib/auth/TokenStore.ts`, `lib/auth/TokenStore.test.ts`
      - covers: INV-W3 · scenario: B "The session survives a page reload until expiry" (persistence half:
        get/set/clear round-trips a `StoredToken`; nothing else persisted)
      - depends: T-01
      - parallel: yes
      - verify: `npm run test -- lib/auth/TokenStore`

- [x] **T-05 · API client + `authFetch` (Bearer, no tenant id, Problem→ApiError, 401 signal)**
      - files: `lib/api/types.ts`, `lib/api/ApiError.ts`, `lib/api/authFetch.ts`, `lib/api/apiClient.ts`,
        `lib/api/authFetch.test.ts`, `test/msw/handlers.ts`
      - covers: C-01, INV-W1 · scenario: B "Every API request from the app carries the bearer credential"
        (attaches Bearer, sends **no** client-supplied tenant identifier); emits `sessionExpired` on any 401
        (the choke point later consumed by C-3/C-4)
      - depends: T-04
      - parallel: no
      - verify: `npm run test -- lib/api`

- [x] **T-06 · AuthProvider + `useAuth` (session state machine)**
      - files: `lib/auth/AuthProvider.tsx`, `lib/auth/session.ts`, `lib/auth/AuthProvider.test.tsx`
      - covers: INV-W4, INV-W5 · scenarios: A "Valid credentials sign the member in", A "Invalid credentials are
        rejected on the login screen", A "A server error during sign-in is reported without a session", B "The
        session survives a page reload until expiry" (boot rehydrate + `/me` validation), C "An expired token
        mid-session returns the member to login", C "An invalid token returned by the API ends the session"
        — login (store→`getMe`→authenticated), logout, clear-on-401, `now>=expiresAt` guard, replace-on-login
      - depends: T-02, T-04, T-05
      - parallel: no
      - verify: `npm run test -- lib/auth/AuthProvider`

- [x] **T-07 · RouteGuard (redirects + intended-destination preservation)**
      - files: `components/RouteGuard.tsx`, `components/RouteGuard.test.tsx`
      - covers: C-02 · scenarios: C "Visiting a protected screen with no session redirects to login" (preserves
        `?redirect=`), C "An already-signed-in member skips the login screen"; `unknown` renders a
        hydration-stable placeholder
      - depends: T-06
      - parallel: no
      - verify: `npm run test -- components/RouteGuard`

- [x] **T-08 · LoginForm + `/login` page (ADF markup, wired validation, in-flight, verbatim error)**
      - files: `components/LoginForm.tsx`, `app/(auth)/login/page.tsx`, `components/LoginForm.test.tsx`
      - covers: scenarios A "Valid credentials sign the member in", A "Invalid credentials are rejected on the
        login screen", A "Empty required fields…", A "Malformed email…", A "The submit control is inert while a
        sign-in is in flight", A "A server error during sign-in…", D "The login screen renders in the resolved
        locale", D "A localized server error is displayed as received" (`Problem.message` verbatim)
      - depends: T-02, T-03, T-05, T-06
      - parallel: yes
      - verify: `npm run test -- components/LoginForm`

- [x] **T-09 · `(app)` chrome: RouteGuard mount + BrandingBar tenant context + home + logout**
      - files: `app/(app)/layout.tsx`, `components/BrandingBar.tsx`, `app/(app)/page.tsx`,
        `components/BrandingBar.test.tsx`
      - covers: BR-01, C-01 · scenarios: B "The authenticated chrome shows the member's identity and tenant"
        (`.af-branding` "Conectado como …" from `/me`), B "Tenant context is never taken from user input", C
        "Logout clears the session" (the "Sair" link)
      - depends: T-06, T-07
      - parallel: yes
      - verify: `npm run test -- components/BrandingBar`

## Coverage & dependency summary
- **9 tasks.** Every one of the spec's **17 scenarios** is covered (A×6, B×4, C×5, D×2); no scenario left
  uncovered. T-01 is the only non-scenario task — deliberate scaffolding infra that enables all others.
- **Dependency chain:** `T-01 → {T-02, T-03, T-04}`; `T-04 → T-05 → T-06`; `T-06 → T-07`;
  `{T-03,T-05,T-06} → T-08`; `{T-06,T-07} → T-09`.
- **Parallelisable (worktree):** T-02, T-03, T-04 (independent leaf files after T-01); T-08, T-09 (distinct
  `app/(auth)` vs `app/(app)` trees) run in parallel once their deps land. All others are serial (shared
  config or direct dependency).
