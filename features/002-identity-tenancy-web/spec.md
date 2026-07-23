# Feature — Login & tenant-context UI (ADF Fusion theme)

**ID:** features/002-identity-tenancy-web
**User Story:** US-6.1
**Version:** v1
**Status:** Draft
**Date:** 2026-07-22
**Project:** notebox-web (satellite; react/next)

## Origin
- **User Story:** US-6.1 — a tenant member's data is isolated from other tenants behind authentication;
  this feature is the **web front door** that authenticates the member and renders their tenant context.
- **FRs covered:** FR-17 (multi-tenant identity & isolation) — its client half: prove identity, carry the
  stateless token, resolve the caller's tenant **from the token**, never from user input.
- **BRs bound:** BR-01 (tenant data isolation), BR-02 (every entity belongs to exactly one tenant) —
  honoured on the client by never letting the user choose or override a tenant; BR-08 (no user-facing text
  ever shown untranslated).
- **Primary source:** PRD v1 §2 (E6), §3.1 FR-17; the API contract of `feat-001-identity-tenancy`
  (`features/001-identity-tenancy/contracts/` — `/auth/login`, `/me`, the `Problem` envelope); the ADF
  Fusion design constraint (`features/002-identity-tenancy-web/DESIGN-CONSTRAINT.md`, human decision
  2026-07-22).

## Summary
Deliver the web entry point for the product: a login screen where a member authenticates with email and
password, and the surrounding session/tenant-context behaviour every other screen depends on. On success
the client holds the API's stateless token, renders the authenticated member's identity and tenant in the
application chrome, and admits the member to the protected app. On failure — bad credentials, an expired or
invalid token, or a direct visit to a protected route without a session — the client shows a localized
message and routes back to login. This feature owns only the client-side identity experience; the auth and
isolation guarantees themselves belong to the API (`feat-001`), whose contract this feature consumes.

## Scope
- **In:**
  - A login screen (email + password) that authenticates against the API and, on success, admits the
    member to the app.
  - Client-side input validation before submission (required fields; email shape) with localized
    field-level messages.
  - Holding the issued stateless token on the client and attaching it as the bearer credential on every
    subsequent API request; the session survives a page reload until the token expires.
  - Rendering the authenticated member's tenant context — display name and tenant, in the application
    branding bar — sourced **only** from the authenticated-identity endpoint, never from user input.
  - A route guard: protected screens are reachable only with a valid session; an unauthenticated visit
    redirects to login, preserving the intended destination.
  - Session termination on an authentication failure returned by any API call (missing / expired / invalid
    token): the client clears the session and returns to login.
  - Explicit logout that clears the client session and returns to login.
  - Localized presentation (en, pt) of all login/session UI strings and of server-returned error messages;
    no raw message key or blank ever shown.
  - All UI built to the **ADF Fusion** theme per `DESIGN-CONSTRAINT.md` (presentation constraint only).
- **Out:**
  - The auth and tenant-isolation behaviour itself — issuing/verifying the token, deriving tenant
    server-side, cross-tenant denial. Owned by `feat-001-identity-tenancy` (already delivered).
  - Self-service registration/signup, tenant onboarding, and user/tenant provisioning — `[TBD — OQ-11]`;
    this feature assumes members already exist.
  - Password reset / recovery, "remember me" beyond token expiry, external IdP / SSO.
  - Silent token refresh / refresh-token rotation — v1 re-authenticates on expiry (mirrors `feat-001`).
  - Administrative surfaces (user management, translation-catalog editing UI) and any role-gated feature
    UI — later features; this feature only *surfaces* the role claim, it gates nothing beyond login.
  - The locale-switcher UI and runtime translation management (E5 features); this feature consumes the
    resolved locale, it does not let the user manage translations.
  - Brute-force lockout / rate limiting (deferred hardening, server-side).

## Acceptance criteria (Gherkin)
> Endpoints and the `Problem`/`Me` shapes below are the **consumed** contract of `feat-001`, not new design
> decisions of this feature. Scenarios are written to become the executable UI/integration tests in
> `implement`.

```gherkin
Feature: A member signs in from the web (FR-17)

  Scenario: Valid credentials sign the member in
    Given the login screen with no active session
    When the member submits a correct email and password
    Then the client obtains a token and the member lands on the authenticated home screen
    And the login screen is no longer shown

  Scenario: Invalid credentials are rejected on the login screen
    Given the login screen with no active session
    When the member submits an email and password the API rejects with AUTH_INVALID_CREDENTIALS
    Then the member stays on the login screen
    And the localized message from the API error is shown
    And no session is established

  Scenario: Empty required fields block submission before any request
    Given the login screen with an empty password field
    When the member attempts to submit
    Then no authentication request is sent
    And a localized required-field message is shown on the password field

  Scenario: Malformed email is flagged before any request
    Given the login screen with "not-an-email" in the email field and a password entered
    When the member attempts to submit
    Then no authentication request is sent
    And a localized invalid-email message is shown on the email field

  Scenario: The submit control is inert while a sign-in is in flight
    Given the member has submitted valid-looking credentials
    When the authentication request has not yet resolved
    Then the submit control is disabled
    And a second submission is not sent

  Scenario: A server error during sign-in is reported without a session
    Given the login screen with no active session
    When the member submits and the API responds with a 5xx server error
    Then a localized generic error message is shown
    And the member stays on the login screen with no session established
```

```gherkin
Feature: The signed-in session carries tenant context (FR-17, BR-01, BR-02)

  Scenario: The authenticated chrome shows the member's identity and tenant
    Given a member of tenant A has signed in
    When the authenticated home screen renders
    Then the member's display name is shown in the application branding bar
    And the tenant context shown is tenant A, sourced from the authenticated-identity response

  Scenario: Tenant context is never taken from user input
    Given a signed-in member of tenant A
    When a tenant identifier for tenant B is present in the page URL or form input
    Then the rendered tenant context remains tenant A
    And the tenant identifier in the input is ignored

  Scenario: The session survives a page reload until expiry
    Given a member has signed in and the token has not expired
    When the member reloads the page
    Then the member remains signed in without re-entering credentials

  Scenario: Every API request from the app carries the bearer credential
    Given a signed-in member
    When the app requests any protected resource
    Then the request carries the member's token as its bearer credential
    And carries no client-supplied tenant identifier
```

```gherkin
Feature: Protected screens require a live session (FR-17, C-02)

  Scenario: Visiting a protected screen with no session redirects to login
    Given no active session
    When the member navigates directly to a protected screen
    Then the member is redirected to the login screen
    And the intended destination is preserved for after sign-in

  Scenario: An already-signed-in member skips the login screen
    Given a member with a live session
    When the member navigates to the login screen
    Then the member is redirected to the authenticated home screen

  Scenario: An expired token mid-session returns the member to login
    Given a signed-in member whose token has since expired
    When the app makes a request the API rejects with AUTH_TOKEN_EXPIRED
    Then the client clears the session
    And the member is returned to the login screen with a localized session-expired message

  Scenario: An invalid token returned by the API ends the session
    Given a signed-in member
    When any app request is rejected with AUTH_TOKEN_INVALID or AUTH_REQUIRED
    Then the client clears the session
    And the member is returned to the login screen

  Scenario: Logout clears the session
    Given a signed-in member
    When the member logs out
    Then the client session is cleared
    And the member is on the login screen
    And navigating back to a protected screen redirects to login
```

```gherkin
Feature: Login and session text is localized (BR-08, C-09)

  Scenario: The login screen renders in the resolved locale
    Given the resolved locale is pt
    When the login screen renders
    Then all its labels, placeholders, and controls are shown in pt
    And no raw message key or blank label is shown

  Scenario: A localized server error is displayed as received
    Given the resolved locale is pt
    When sign-in fails with AUTH_INVALID_CREDENTIALS
    Then the message shown is the pt message from the API error envelope
    And no raw message key is shown
```

## Compliance pre-flight
Checklist copied from `constitution/02-compliance.md`, marked for a **web client** consuming the API:

- **C-01 · Tenant isolation** — **applies (client obligation).** The UI never offers a tenant selector and
  never sends a client-supplied tenant identifier; tenant context is rendered only from the
  authenticated-identity response. *Evidence:* the "tenant context is never taken from user input" and
  "carries no client-supplied tenant identifier" scenarios.
- **C-02 · Authenticated by default** — **applies.** Every screen except login requires a live session; the
  route guard redirects unauthenticated access. *Evidence:* the "protected screens require a live session"
  scenarios.
- **C-03 · Least-privilege authorization** — **not applicable (this feature).** No admin/config surface is
  exposed here; the role claim is only surfaced, not acted on. Role-gated UI arrives with the features that
  own those surfaces, and server-side authorization remains the real control (`feat-001` C-03).
- **C-04 · Personal data minimization** — **applies.** The client stores only the token plus the minimal
  identity it renders (display name, tenant, role); the password is never persisted or logged, and the
  token is never written to logs. *Evidence:* no-PII/secret-in-logs review of the client + password-field
  handling.
- **C-05 · Secrets never committed** — **applies (partial).** The web client holds no signing key (it never
  verifies the token); the API base URL/config comes from environment, none in the repo. *Evidence:*
  `.env.example` entry for the API base + repo secret scan.
- **C-06 · Encryption in transit** — **applies.** Credentials and the token travel only over TLS/HTTPS in
  deployed environments. *Evidence:* deployment/ingress config (HTTPS-only).
- **C-07 · Image upload safety** — **not applicable.** This feature uploads/serves no images.
- **C-08 · Rich-text sanitization** — **not applicable.** This feature renders no rich-text/HTML content.
- **C-09 · Localization completeness** — **applies.** All login/session UI strings resolve in en + pt, and
  server error messages (already localized by the API) are shown verbatim; no raw key/blank ever reaches
  the screen. *Evidence:* the localization scenarios + a per-locale coverage check of client strings.
- **C-10 · Audit trail for irreversible & admin actions** — **not applicable (this feature).** The client
  performs no irreversible or administrative server action; authentication attempts are audited server-side
  by `feat-001`.
- **C-11 · Data retention & deletion path** — **not applicable (this feature).** No account/tenant data is
  managed here; logout only clears the local session (no server-stored PII introduced). Account lifecycle
  depends on provisioning `[TBD — OQ-11]`.

> Accessibility of the UI is a `notebox-web` concern (per `02-compliance.md`) and is handled by the
> satellite's standards; it is not a numbered compliance item and is not gated here.

## Out of scope
See **Scope · Out** above. Explicitly deferred: registration/provisioning (OQ-11), password reset, SSO,
silent/refresh-token renewal, admin & role-gated UI, locale switching / translation management, and any
change to the API's auth or isolation behaviour (owned by `feat-001`).

## Open Questions
- **OQ-11** — Tenant & user provisioning mechanism (how members and tenants are created). **Non-blocking**
  for this feature, which — like `feat-001` — assumes members already exist; it blocks a future
  provisioning/onboarding UI, not this login experience. See `catalogs/open-questions.md`.

_No new Open Question is opened by this spec. The token-storage mechanism (e.g. where the client keeps the
token) and its XSS/CSRF trade-offs are a `plan`-level "how" decision, not an unsettled requirement._
