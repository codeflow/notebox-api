# Plan — Identity & multi-tenant isolation (JWT)

**Feature:** features/001-identity-tenancy · **US-6.1** · **FR-17** · **BR-01, BR-02**
**Spec:** `features/001-identity-tenancy/spec.md` (approved 2026-07-22)
**Date:** 2026-07-22

## Origin
Realizes the approved spec: authenticate via stateless JWT, establish a request-time tenant context, and
enforce tenant isolation on every access. As the first feature in a greenfield repo, it also lays the
**project skeleton** all later features inherit.

## Approach (three sentences)
Stand up a **Quarkus** Jakarta EE application with the layered package structure from AD-01, exposing a
login endpoint that verifies Argon2id-hashed credentials and issues an **RS256-signed MicroProfile JWT**
carrying the user id, tenant id, and role. Every other endpoint is protected by MicroProfile JWT
validation; a `@RequestScoped` `TenantContext` is populated from the verified token by a JAX-RS
`ContainerRequestFilter`, and a `TenantScopedRepository` base (AD-03) injects a mandatory tenant predicate
so no query crosses tenants. Cross-cutting error handling is a set of `ExceptionMapper`s returning one
localized problem envelope (AD-11, BR-08), and the `User` entity itself serves as the tenant-owned resource
that the isolation scenarios test against.

## Key decisions & reversibility

| # | Decision | Reversibility | Why |
|---|----------|---------------|-----|
| D-1 | **Quarkus** runtime (RESTEasy, ArC, Hibernate ORM, SmallRye JWT, Quarkus Redis) | **one-way** | Human decision 2026-07-22; container-native, MP-JWT built-in for OQ-08. |
| D-2 | **RS256** asymmetric JWT; claims `sub`=userId, `tenant`=tenantId, `role`, `iss`, `iat`, `exp` | **one-way** (public contract; web + future services validate) | MP-JWT default; public key validates without sharing the signing key (C-05). |
| D-3 | A user belongs to **exactly one tenant**; **email globally unique** | **one-way** (schema) | Login by email resolves to one user+tenant; satisfies BR-02. |
| D-4 | **Argon2id** password hashing | reversible (rehash on next login) | Human decision 2026-07-22; OWASP first choice. |
| D-5 | **`TenantScopedRepository` base** as the single isolation choke point (AD-03) | **one-way** (every feature builds on it) | Structural isolation beats per-endpoint discipline. |
| D-6 | Auth API shape: `POST /api/auth/login`, `GET /api/me`, `GET/PATCH /api/users/{id}`; one problem envelope | **one-way** (web consumes it) | Minimal surface that proves auth + isolation; `User` is the tenant-owned test resource. |
| D-7 | Access-token TTL default **60 min**, no refresh token in v1 | reversible (config value) | Spec deferred refresh; re-authenticate on expiry. |
| D-8 | Package root `com.notebox.api` with `api / application / domain / infrastructure` | reversible | AD-01 layering. |

## Alternatives rejected
- **Server-side HTTP sessions (`@SessionScoped`/`@Stateful`)** — rejected by OQ-08 (stateless JWT); would reverse AD-06.
- **WildFly / Payara full app server** — heavier runtime and slower dev loop than Quarkus for a greenfield containerized API; Quarkus bundles MP-JWT, CDI, Hibernate, JAX-RS, and Redis coherently.
- **Symmetric HS256 JWT** — rejected in favour of RS256 so validators need only the public key, not the signing secret (MP-JWT default, better rotation story).
- **Separate `Credential` entity** — rejected; a single Argon2id `passwordHash` column on `User` is sufficient for v1 and simpler.

## Data model
See `data-model.md`. Entities: **Tenant**, **User** (tenant-owned, BR-02). Migration V1 creates both tables.

## Contracts
See `contracts/`:
- `openapi.yaml` — `POST /api/auth/login`, `GET /api/me`, `GET /api/users/{id}`, `PATCH /api/users/{id}`, and the shared error schema.
- `interfaces.md` — `TenantContext`, `JwtIssuer`, `TenantScopedRepository<T>`, `MessageResolver`, and the exception → HTTP mapping.

## Blast radius
Greenfield — this feature **creates the project foundation**, so its blast radius is forward-looking:
- **New:** `pom.xml` (Quarkus BOM + deps), `application.properties`, package skeleton, `Tenant`/`User` entities, `TenantContext`, JWT filter + `JwtIssuer`, `TenantScopedRepository` base, `ExceptionMapper`s + problem envelope, `MessageResolver` + seed messages (en/pt) for `AUTH_*` codes, Flyway migration V1, and the CI already wired (`mvn -B verify`).
- **Every later feature depends on:** `TenantContext`, `TenantScopedRepository`, the error envelope, `MessageResolver`, and the JWT filter. Changing any of these after this ships is expensive — hence they are one-way (D-2, D-5, D-6).
- **`notebox-web` (feat-002)** consumes `POST /api/auth/login` + the token/claims contract (D-2, D-6).

## Risk
- **A new repository omits the tenant predicate → isolation breach (BR-01).** Signal: mandatory cross-tenant negative test per tenant-owned repository (NFR-01, C-01); the `TenantScopedRepository` base makes the safe path the default path.
- **JWT signing key mismanaged/absent.** Signal: application fails fast at startup if the key is missing; key from secret manager (C-05), never committed.
- **Client-supplied tenant spoofing.** Mitigated by D-5/filter: tenant comes only from the verified token (spec scenario), never from request input.
- **Quarkus learning curve / build config.** Signal: `mvn -B verify` green in CI; the skeleton is small.

## Compliance carry-through
C-01, C-02, C-03, C-04, C-05, C-06, C-09, C-10 all apply (see spec pre-flight); each is realized by a
named artifact above and verified by a test in `tasks`/`implement`.
