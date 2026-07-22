# Feature — Identity & multi-tenant isolation (JWT)

**ID:** features/001-identity-tenancy
**User Story:** US-6.1
**Version:** v1
**Status:** Draft
**Date:** 2026-07-22

## Origin
- **User Story:** US-6.1 — a tenant member's data is isolated from other tenants behind authentication.
- **FRs covered:** FR-17.
- **BRs bound:** BR-01 (tenant data isolation), BR-02 (every entity belongs to exactly one tenant).
- **Primary source:** PRD v1 §2 (E6), §3.1 FR-17; decisão humana 2026-07-22 (OQ-08 stateless JWT).

## Summary
Establish the authentication and tenant-isolation foundation the rest of the product depends on. A caller
proves identity with valid credentials and receives a stateless, signed token that carries their identity
and their tenant. Every subsequent request is authenticated from that token — no server session — and every
access to tenant-owned data is authorized against the caller's tenant, so no principal can read or modify
another tenant's data. This feature provides the request-time tenant context and isolation guarantee; other
features build on it.

## Scope
- **In:**
  - Authenticating valid credentials and issuing a signed token that identifies the member and their tenant and carries an expiry.
  - Rejecting authentication with invalid credentials.
  - Authenticating every protected request from the token: reject missing, malformed, expired, or invalid-signature tokens.
  - Deriving the effective tenant and identity **from the token**, never from client-supplied request input.
  - Authorizing access to tenant-owned data so cross-tenant reads and writes are denied without disclosing existence.
  - Distinguishing member vs administrator role as a claim, for later features to authorize on.
- **Out:**
  - The web login UI and tenant-context rendering — delivered by `feat-002-identity-tenancy-web`.
  - Self-service registration/signup, tenant onboarding, and user provisioning — see `[TBD — OQ-11]`; this feature assumes members already exist.
  - Password reset / recovery flows, external IdP / SSO.
  - Refresh-token rotation — v1 uses short-lived access tokens; re-authentication on expiry (revisit later).
  - Brute-force lockout / rate limiting — deliberately deferred to a later hardening feature.
  - Administrative surfaces (e.g. translation-catalog editing) — those live in their own features and only consume the role claim established here.

## Acceptance criteria (Gherkin)
```gherkin
Feature: Authentication issues a stateless token (FR-17)

  Scenario: Valid credentials yield a signed token
    Given a member of tenant A with correct credentials
    When they authenticate
    Then they receive a signed token identifying the member and tenant A
    And the token carries an expiry timestamp

  Scenario: Invalid credentials are rejected
    Given a member of tenant A
    When they authenticate with an incorrect password
    Then authentication is rejected with error AUTH_INVALID_CREDENTIALS
    And no token is issued

Feature: Every protected request is authenticated from the token (FR-17)

  Scenario: Missing token is rejected
    Given a protected resource
    When a request arrives with no token
    Then the request is rejected as unauthenticated with error AUTH_REQUIRED

  Scenario: Expired token is rejected
    Given a token whose expiry has passed
    When it is used on a protected request
    Then the request is rejected as unauthenticated with error AUTH_TOKEN_EXPIRED

  Scenario: Token with an invalid signature is rejected
    Given a token whose signature does not verify
    When it is used on a protected request
    Then the request is rejected as unauthenticated with error AUTH_TOKEN_INVALID

Feature: Tenant isolation on every access (BR-01, BR-02)

  Scenario: Member reads a resource owned by their own tenant
    Given a member of tenant A and a resource owned by tenant A
    When they request it with a valid token
    Then the resource is returned

  Scenario: Cross-tenant read is denied without disclosing existence
    Given a resource owned by tenant B
    When a member of tenant A requests it with a valid token
    Then the request is rejected as not found
    And no attribute of the resource is included in the response

  Scenario: Cross-tenant write is denied and leaves data unchanged
    Given a resource owned by tenant B
    When a member of tenant A attempts to modify it with a valid token
    Then the request is rejected as not found
    And the resource owned by tenant B is unchanged

  Scenario: Effective tenant comes from the token, not from request input
    Given a valid token for tenant A
    When a request carries a tenant identifier for tenant B in its input
    Then the effective tenant is A
    And the tenant identifier in the input is ignored

Feature: Authentication errors are localized (BR-08)

  Scenario: Error message honours the request locale
    Given the request locale resolves to pt
    When authentication fails with invalid credentials
    Then the error AUTH_INVALID_CREDENTIALS carries a message in pt
    And no raw message key is returned
```

## Compliance pre-flight
Checklist copied from `constitution/02-compliance.md`, marked per item:

- **C-01 · Tenant isolation** — **applies.** The core guarantee of this feature (isolation scenarios).
- **C-02 · Authenticated by default** — **applies.** This feature establishes it; protected requests require a valid token.
- **C-03 · Least-privilege authorization** — **applies.** Member vs administrator role is issued as a claim; admin-only surfaces authorize on it in their own features.
- **C-04 · Personal data minimization** — **applies.** Credentials and minimal user/tenant identity are handled; no incidental PII in tokens or logs.
- **C-05 · Secrets never committed** — **applies.** The token signing key comes from env / secret manager; never in the repo.
- **C-06 · Encryption in transit** — **applies.** Credentials and tokens travel only over TLS.
- **C-07 · Image upload safety** — **not applicable.** This feature handles no images.
- **C-08 · Rich-text sanitization** — **not applicable.** This feature handles no rich text.
- **C-09 · Localization completeness** — **applies.** Auth error/validation messages are localized en + pt (see localization scenario).
- **C-10 · Audit trail for irreversible & admin actions** — **applies.** Authentication failures and (later) admin actions are recorded who/what/when.
- **C-11 · Data retention & deletion path** — **applies (partial).** Credential/user data needs a deletion/anonymization path; full account lifecycle depends on provisioning `[TBD — OQ-11]`.

## Out of scope
See **Scope · Out** above. Explicitly deferred: registration/provisioning (OQ-11), refresh tokens,
brute-force lockout, password reset, SSO, and all web UI (feat-002).

## Open Questions
- **OQ-11** — Tenant & user provisioning mechanism (how members and tenants are created). Non-blocking for
  this feature's auth/isolation behaviour, which assumes members exist; blocks an end-to-end login flow and
  a future provisioning feature. See `catalogs/open-questions.md`.
