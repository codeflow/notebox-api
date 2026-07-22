# Audit — Identity & multi-tenant isolation (JWT)

**Feature:** features/001-identity-tenancy · US-6.1 · FR-17 · BR-01, BR-02
**Date:** 2026-07-22 · **Auditor model:** opus/xhigh
**Verify:** `mvn -B verify` green — 24 tests, 0 failures.

## Verdict: **PASS**

The feature's core — stateless JWT auth and tenant isolation — is coherent across spec → test → code and
holds every bound BR/AD. **F1 and F4 were fixed during the audit** (re-verified green, 24 tests). F2 and F3
remain as tracked low-severity follow-ups (non-blocking).

**Post-fix status:** F1 ✅ resolved — JWT key locations profile-gated (`%dev`/`%test` use the dev keys,
`%prod` requires `MP_JWT_VERIFY_PUBLICKEY_LOCATION` / `SMALLRYE_JWT_SIGN_KEY_LOCATION` from the env),
`.env.example` added. F4 ✅ resolved — `UserResourceTest.readsOwnTenantUser` now asserts `passwordHash` is
absent from the response.

## 1. Traceability — every scenario → test → code
All 10 spec scenarios trace to a real test exercising real code:

| Scenario | Test | Code |
|----------|------|------|
| Valid credentials → token | AuthResourceTest.validCredentialsYieldToken | AuthResource → AuthService → JwtIssuerImpl |
| Invalid credentials | AuthResourceTest.invalidPassword / unknownEmail | AuthService + ApiExceptionMapper |
| Missing token | JwtTenantFilterTest.missingToken + ErrorMappingTest.missingTokenYieldsAuthRequired | UnauthorizedExceptionMapper |
| Expired token | JwtTenantFilterTest.expiredToken + ErrorMappingTest.expiredTokenYieldsExpiredCode | AuthenticationFailedExceptionMapper |
| Invalid signature | JwtTenantFilterTest.invalidSignature + ErrorMappingTest.invalidSignatureYieldsInvalidCode | AuthenticationFailedExceptionMapper |
| Own-tenant read | TenantScopedRepositoryTest.readsOwnTenant + UserResourceTest.readsOwnTenantUser | TenantScopedRepository.findByIdInTenant |
| Cross-tenant read denied (404) | TenantScopedRepositoryTest.crossTenantReadReturnsEmpty + UserResourceTest.crossTenantReadReturnsNotFound | TenantScopedRepository + UserResource |
| Cross-tenant write denied, unchanged | UserResourceTest.crossTenantWriteIsDeniedAndLeavesDataUnchanged | UserResource.update |
| Effective tenant from token | JwtTenantFilterTest.tenantContextComesFromTokenNotFromInput | JwtTenantFilter |
| Localized error | ErrorMappingTest.errorMessageHonoursRequestLocale | MessageResolver + LocaleResolver |

No broken links.

## 2. Scenario honesty — would the test fail on regression?
- **Cross-tenant read/write** tests use a real login token, real HTTP, real MySQL, and verify the *other*
  tenant's data is untouched (login as B, assert displayName still "Name"). Dropping the tenant predicate
  would flip these to 200/leaked data → red. Honest.
- **Tenant-from-token** passes a spoofed `tenantOverride` query param and asserts the token's tenant wins.
  Reading the param would fail it. Honest.
- The repository test mocks only `TenantContext` (the caller's tenant, legitimately injected); the actual
  query runs against the real DB. Not a mock-asserting-a-mock.
No theater found.

## 3. Constitution — bound rules and boundaries (grepped, not assumed)
- **BR-01/BR-02** — all user access goes through `TenantScopedRepository` (tenant predicate on every query);
  cross-tenant → empty/404. The one un-scoped query (`CredentialLookup.findByEmail`) is the documented
  AD-03 login exception and returns only the matching user's own record. **Held.**
- **BR-08** — no hardcoded user-facing message literals (grep clean); all via `MessageResolver`, en/pt
  seeded, English fallback. **Held.**
- **AD-01** — `domain/` imports only JPA/Hibernate annotations, no infra/api. **Held.**
- **AD-02** — `EntityManager` appears only in `infrastructure/persistence`. **Held.**
- **AD-03** — single choke point; `UserRepository` extends the base. **Held.**
- **AD-06 / AD-12** — no `@SessionScoped`/`@Stateful` (grep clean); stateless JWT. **Held.**
- **AD-07** — `LoginRequest`/`UserPatch` Bean-Validated at the edge. **Held.**
- **AD-09** — no manual transaction control (grep clean); `@Transactional` on application/resource methods. **Held.**
- **AD-11** — resources throw `ApiException`; all formatting in `ExceptionMapper`s. **Held.**
- **AD-05** — resolved via `MessageResolver` (boundary held), but the concrete store is properties files,
  not the MySQL-backed catalog AD-05 specifies → **Finding F2**.

## 4. Compliance evidence
- **C-01** ✓ cross-tenant tests. **C-02** ✓ auth-by-default + 401 test. **C-06** n/a locally (TLS at deploy).
- **C-03** — role issued as a claim; no admin-only surface in this feature to test yet (that arrives with
  US-5.2). Partial-by-design, acceptable.
- **C-04** — `UserDto`/`MeDto` structurally exclude `passwordHash` (no field). No explicit "absence" test →
  **Finding F4** (low).
- **C-05** — **evidence claimed (`.env.example` + secret scan) does not exist; a private key IS committed**
  → **Finding F1**.

## 5. Scope vs plan blast radius
Files created match the plan's forward-looking blast radius (skeleton, entities, TenantContext, JWT filter,
JwtIssuer, repository base, error envelope, MessageResolver, Flyway V1). Additions — `CredentialLookup`,
`AuthService`, DTOs, `MeResource`/`UserResource`, `LocaleResolver` — are all within the feature's described
surface (login + identity endpoints). **Redis was deferred** (no task caches; AD-08) and was disclosed. No
scope creep. Dev RSA keys are new files not itemized in the plan (see F1).

## 6. Open Questions
- **OQ-11** (provisioning) opened at spec, recorded in the catalog, non-blocking — this feature assumes
  users exist. Not closed by implementer assumption.
- No OQ was silently closed. The two deviations (Redis deferral, properties-based messages) were disclosed,
  not buried.

---

## Findings (ranked)

### F1 — MEDIUM · C-05 · JWT signing key defaults to a committed key, no `.env.example` — ✅ FIXED
`smallrye.jwt.sign.key.location=privateKey.pem` and `mp.jwt.verify.publickey.location=publicKey.pem` are set
for **all profiles**, and `privateKey.pem` is committed under `src/main/resources`. **Failure scenario:** a
production deploy that does not override `SMALLRYE_JWT_SIGN_KEY_LOCATION` signs tokens with a private key
that is public in the repo — anyone can forge a valid token for any tenant, defeating BR-01/BR-02. The
compliance pre-flight claimed `.env.example` as evidence; it is absent.
**Fix:** gate the committed keys to `%dev`/`%test`, require env-provided key locations in `%prod`, add
`.env.example` documenting `SMALLRYE_JWT_SIGN_KEY_LOCATION`, `MP_JWT_VERIFY_PUBLICKEY_LOCATION`, and the DB
credentials. Recommended **before publish**.

### F2 — LOW · AD-05 · messages come from properties files, not the MySQL-backed catalog
AD-05 specifies a MySQL `Message` store with a runtime CRUD editor; this feature uses resource bundles. The
`MessageResolver` abstraction is in place, so swapping the impl is a one-class change. The MySQL catalog +
editor is the job of **US-5.2 (i18n feature)**; building it here would be scope creep. Boundary (resolve via
`MessageResolver`, no literals) holds. Track for US-5.2.

### F3 — LOW · AD-12 · framework-default scoping / field injection
JAX-RS resources and providers rely on Quarkus default scoping rather than explicit `@ApplicationScoped`,
and `TenantScopedRepository` uses field injection (`@Inject`) rather than the preferred constructor
injection (an abstract-base CDI constraint). Style-level vs the standard; no behavioural impact.

### F4 — LOW · C-04 · no explicit "passwordHash absent" test — ✅ FIXED
The DTOs cannot serialize `passwordHash` (no field), but a regression test asserting its absence would lock
the guarantee. Add a one-line assertion to `UserResourceTest`.

## GitHub
On this pass, comment the verdict on issue #1 (`wf github comment feat-001-identity-tenancy.audit
--event audit`) — with confirmation. Nothing merges here; the PR is the `publish`/`review` steps.
