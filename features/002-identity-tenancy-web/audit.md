# Audit — Login & tenant-context UI (ADF Fusion theme)

**ID:** features/002-identity-tenancy-web · **Date:** 2026-07-22 · **Auditor model:** opus·xhigh (delegated,
adversarial) · **Verify:** `npm run verify` green (clean → lint ✔ · typecheck ✔ · **54 tests / 9 files** ✔ · `next build` ✔).

**Verdict: `pass`.** Adversarial review could not prove the feature "not done". Three findings were raised;
**all three were fixed and covered by tests before this verdict** (details below). No constitution or
invariant violation survived scrutiny.

## Traceability — all 17 scenarios covered by honest tests
| # | Scenario | Test | Verdict |
|---|---|---|---|
| A1 | Valid creds sign in | LoginForm A1 · AuthProvider "signs in" | covered |
| A2 | Invalid creds rejected | LoginForm A2/D2 · AuthProvider "rejects invalid" | covered |
| A3 | Empty field blocks request | LoginForm A3 (`loginCalled===false`) · validation test | covered (honest — MSW flag) |
| A4 | Malformed email blocks request | LoginForm A4 · validation test | covered (honest) |
| A5 | Submit inert in-flight | LoginForm A5 (`disabled` asserted) | covered |
| A6 | 5xx generic, no session | LoginForm A6 · `messageFor` generic · AuthProvider "server error" | covered |
| B1 | Chrome shows identity + tenant | BrandingBar B1 | covered |
| B2 | Tenant never from input | BrandingBar B2 (structural — fails if it read input) | covered |
| B3 | Survives reload until expiry | AuthProvider boot rehydrate · TokenStore round-trip | covered |
| B4 | Every request bearer, no tenant id | authFetch B4 (Bearer set, `x-tenant-id` null, no `tenant` in URL) | covered (strong) |
| C1 | Protected + no session → login, preserve dest | RouteGuard C1 | covered |
| C2 | Signed-in on /login → home | RouteGuard C2 + redirect-target tests | covered |
| C3 | Expired mid-session → clear + login + **session-expired message** | AuthProvider 401 test + **`messageFor` session-ended tests (added)** | covered |
| C4 | Invalid / AUTH_REQUIRED token ends session | AuthProvider 401 branch + `messageFor` all-codes test | covered |
| C5 | Logout clears session | AuthProvider logout · BrandingBar "Sair" | covered |
| D1 | Login renders in resolved locale | LoginForm D1 (pt) · I18nProvider test | covered |
| D2 | Server error verbatim | LoginForm A2/D2 · `messageFor` verbatim test | covered (strong) |

## Constitution / invariants (grepped, not assumed)
- **INV-W1 / BR-01 / BR-02 / C-01** — tenant & identity read only from `useAuth().me` (`BrandingBar.tsx`);
  `authFetch` sends only `Authorization`+`Accept`, no tenant id (tested B4/B2). **Holds.**
- **INV-W2 / C-04** — password lives only in `LoginForm` local state; `TokenStore.set` persists only
  `{token,expiresAt}` (tested); no `console.*` in feature code. **Holds.**
- **INV-W3** — token only in header / sessionStorage / memory; never logged, never in a URL. **Holds.**
- **INV-W5 / BR-01** — replace-on-login proven by the A→logout→B test (shows B only, never A). **Holds.**
- **INV-W6 / BR-08 / C-09** — `pt` typed `Record<MessageKey,string>` (compile-time parity) + keyset
  coverage test; server `Problem.message` shown verbatim, never re-keyed. **Holds.**

## Compliance pre-flight — evidence present
C-01 ✔ · C-02 ✔ (RouteGuard C1/C2) · C-04 ✔ · C-05 ✔ (`.env.example`, no secret; client holds no key) ·
C-06 (HTTPS — deploy-time, nothing in code contradicts) · C-09 ✔. Not-applicable items (C-03, C-07, C-08,
C-10, C-11) correctly justified — no admin/upload/rich-text/irreversible/PII-managing surface in this client.

## Scope vs blast radius
Matches the plan. Benign deviations: `.eslintrc.json` (Next's `next lint` expects it) rather than
`eslint.config.mjs`; an extra `I18nProvider.test.tsx` (more coverage); a portable `clean` step added to the
`verify` chain (clears stale `.next/types`); RouteGuard mounted in the provider tree (not `(app)/layout`) and
provider nesting `AuthProvider > I18nProvider(locale=me.locale) > Suspense > RouteGuard` — deliberate and
coherent with the plan's intent. No unexplained files; the temporary root `app/page.tsx` scaffold was removed
when the `(app)` group landed.

## Open Questions
**OQ-11** (provisioning) correctly left **open and out-of-scope** — grep of the web tree for
`provision|signup|register` returns nothing; no provisioning behaviour was smuggled in, no OQ resolved by
implementer assumption.

## Findings raised and resolved
1. **Hydration mismatch (medium)** — `navigator.language` was read during render, risking a server/first-client
   text mismatch on the loading placeholder for a `pt` browser, contradicting the plan's "hydration-stable"
   claim. **Fixed:** `I18nProvider` now reads `navigator.language` only in a post-mount `useEffect`; server and
   first client render both resolve deterministically (explicit `locale` prop still authoritative immediately).
2. **C3 message clause untested (medium, confirmed)** — the session-ended → `session.expired` mapping in
   `LoginForm.messageFor` had zero coverage, so a regression would silently show the wrong message. **Fixed:**
   `messageFor` exported and tested for all three session-ended codes (→ localized session-expired, server text
   suppressed), the verbatim branch, and the generic fallback.
3. **Open-redirect backslash bypass (low-medium, security)** — `safeRedirect` blocked `//host` but not `/\host`
   / `\/host`, which browsers normalize to a protocol-relative off-origin URL. **Fixed:** `safeRedirect` now
   also rejects any target containing a backslash; parametrized tests cover `//`, `/\` and `\/` variants.

## GitHub
Pending (batched with publish, on human confirmation): comment the audit verdict on issue #1
(`wf github comment feat-002-identity-tenancy-web.audit --event audit`). Nothing merges or closes here.
