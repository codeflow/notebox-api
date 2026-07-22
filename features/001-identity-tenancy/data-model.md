# Data model — Identity & multi-tenant isolation

**Feature:** features/001-identity-tenancy · binds **BR-01, BR-02**

## Entities

### Tenant
The tenancy root (organization / workspace). Not tenant-scoped itself — it *is* the scope.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | surrogate id |
| name | String(120) | not null | display name |
| slug | String(60) | not null, **unique** | stable identifier |
| createdAt | Instant | not null | audit |

### User
Tenant-owned identity. **Every User belongs to exactly one Tenant (BR-02).**

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | surrogate id |
| tenantId | UUID | **not null**, FK → Tenant(id) | ownership (BR-02) |
| email | String(254) | not null, **globally unique** | login identifier |
| passwordHash | String(255) | not null | **Argon2id**; never serialized (C-04) |
| role | enum {MEMBER, ADMIN} | not null, default MEMBER | authorization claim (C-03) |
| localePreference | String(5) | nullable | i18n locale (OQ-07); e.g. `en`, `pt` |
| displayName | String(120) | not null | shown in UI |
| active | boolean | not null, default true | soft disable |
| createdAt | Instant | not null | audit |

## Invariants
- **INV-1 (BR-02):** `User.tenantId` is never null; a User cannot exist without a Tenant.
- **INV-2 (BR-01):** every read/write of a User is filtered by the caller's tenant (enforced by
  `TenantScopedRepository`, AD-03) — a User is only visible within its own tenant.
- **INV-3 (C-04):** `passwordHash` is never included in any API response or log.
- **INV-4:** `role ∈ {MEMBER, ADMIN}`.
- **INV-5:** `email` is unique across all tenants (login resolves to exactly one User).

## Migration — V1__identity.sql (Flyway)
- Create table `tenant` (id, name, slug UNIQUE, created_at).
- Create table `app_user` (id, tenant_id FK→tenant, email UNIQUE, password_hash, role, locale_preference,
  display_name, active, created_at). *(`user` is reserved in MySQL → table named `app_user`.)*
- Index `app_user(tenant_id)` to back the tenant-scoped queries.

## Notes / boundaries to later features
- Annotation/Task/Group entities (later features) follow the same pattern: a mandatory `tenant_id` and
  access only via `TenantScopedRepository`.
- Provisioning of Tenants/Users (signup/onboarding) is **out of scope** — see OQ-11. For v1, users are
  seeded (test fixtures + a bootstrap seed); no self-service signup endpoint here.
