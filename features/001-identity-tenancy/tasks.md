# Tasks — Identity & multi-tenant isolation (JWT)

**Feature:** features/001-identity-tenancy · US-6.1 · FR-17 · BR-01, BR-02
**Plan:** `plan.md` (approved 2026-07-22). Decomposition only — the design is decided.
Every task ships its own tests and cites the Gherkin scenario it makes pass.
Verify signal: `mvn -B verify` (per-task narrowed with `-Dtest=...`).

Ordered by dependency, then risk (the task most likely to invalidate the plan goes early).

- [x] **T-01 · Project skeleton & Quarkus build**
      - files: `pom.xml`, `src/main/resources/application.properties`, package roots `com.notebox.api.{api,application,domain,infrastructure}`
      - covers: plan D-1, D-8 (foundation enabler for all scenarios); wires Hibernate ORM, RESTEasy, ArC, SmallRye JWT, Quarkus Redis, Flyway, MySQL datasource
      - depends: —
      - parallel: no (all tasks build on it; owns `pom.xml`)
      - verify: `mvn -B compile` (app assembles; config loads)

- [x] **T-02 · Tenant & User entities + Flyway V1 migration**
      - files: `domain/Tenant.java`, `domain/User.java`, `domain/Role.java`, `src/main/resources/db/migration/V1__identity.sql`, `domain/UserTest.java`
      - covers: BR-02, data-model INV-1…INV-5 · underpins all isolation scenarios
      - depends: T-01
      - parallel: no (touches migrations/build)
      - verify: `mvn -B test -Dtest=UserTest` (invariants: tenantId not null, role enum, email unique)

- [x] **T-03 · JWT validation filter + request-scoped TenantContext**
      - files: `infrastructure/security/TenantContext.java` (+ impl), `infrastructure/security/JwtTenantFilter.java`, `application.properties` (mp.jwt public key), `infrastructure/security/JwtTenantFilterTest.java`
      - covers: FR-17, AD-12 · scenarios: "Missing token is rejected", "Expired token is rejected", "Token with an invalid signature is rejected", "Effective tenant comes from the token, not from request input"
      - depends: T-01
      - parallel: no (shares `application.properties` with T-01/T-04)
      - verify: `mvn -B test -Dtest=JwtTenantFilterTest`

- [x] **T-04 · Login endpoint: Argon2id verify + RS256 JwtIssuer**
      - files: `application/auth/JwtIssuer.java` (+ impl), `application/auth/PasswordHasher.java` (Argon2id), `api/AuthResource.java`, `api/dto/LoginRequest.java`, `api/dto/LoginResponse.java`, `api/AuthResourceTest.java`
      - covers: FR-17 · scenarios: "Valid credentials yield a signed token", "Invalid credentials are rejected"
      - depends: T-02, T-03
      - parallel: no
      - verify: `mvn -B test -Dtest=AuthResourceTest`

- [x] **T-05 · TenantScopedRepository base + UserRepository (AD-03 choke point)**
      - files: `infrastructure/persistence/TenantScopedRepository.java`, `infrastructure/persistence/UserRepository.java`, `infrastructure/persistence/TenantScopedRepositoryTest.java` (incl. cross-tenant negative case)
      - covers: BR-01, BR-02, AD-03, NFR-01 · scenarios: "Member reads a resource owned by their own tenant", "Cross-tenant read is denied without disclosing existence", "Cross-tenant write is denied and leaves data unchanged"
      - depends: T-02, T-03
      - parallel: no
      - verify: `mvn -B test -Dtest=TenantScopedRepositoryTest`

- [x] **T-06 · Error envelope + ExceptionMappers + MessageResolver (en/pt seeds)**
      - files: `api/error/Problem.java`, `api/error/*ExceptionMapper.java`, `infrastructure/i18n/MessageResolver.java` (+ impl), `src/main/resources/messages_en.properties`, `messages_pt.properties`, `api/error/ErrorMappingTest.java`, `infrastructure/i18n/MessageResolverTest.java`
      - covers: AD-11, AD-05, BR-08, C-09 · scenario: "Error message honours the request locale"
      - depends: T-01, T-03
      - parallel: no
      - verify: `mvn -B test -Dtest=MessageResolverTest,ErrorMappingTest`

- [x] **T-07 · Identity endpoints (/me, /users/{id}) + end-to-end isolation tests**
      - files: `api/MeResource.java`, `api/UserResource.java`, `api/dto/UserDto.java` (no passwordHash), `api/dto/UserPatch.java`, `api/UserResourceIT.java`
      - covers: FR-17, BR-01, C-04 · scenarios: all four isolation scenarios end-to-end + "Effective tenant comes from the token"
      - depends: T-04, T-05, T-06
      - parallel: no
      - verify: `mvn -B verify` (integration, full green)

## Coverage check — every scenario has a task
| Scenario | Task(s) |
|----------|---------|
| Valid credentials yield a signed token | T-04 |
| Invalid credentials are rejected | T-04 |
| Missing token is rejected | T-03 |
| Expired token is rejected | T-03 |
| Token with an invalid signature is rejected | T-03 |
| Member reads a resource owned by their own tenant | T-05, T-07 |
| Cross-tenant read is denied without disclosing existence | T-05, T-07 |
| Cross-tenant write is denied and leaves data unchanged | T-05, T-07 |
| Effective tenant comes from the token, not from request input | T-03, T-07 |
| Error message honours the request locale | T-06 |

**Uncovered scenarios:** none.

## Dependency chain
```
T-01 ─┬─ T-02 ─┬─ T-04 ─┐
      ├─ T-03 ─┼─ T-05 ─┼─ T-07
      └─ T-06 ─┘        │
              (T-06 also depends on T-03)
```
Risk-first within the chain: **T-03 (JWT)** and **T-05 (isolation choke point)** are the plan-critical
tasks — done early so a wrong assumption surfaces while the plan is still cheap to change.

**Parallelism:** none marked for isolated worktrees — the tasks share `pom.xml`/`application.properties`
and the same fresh module, so parallel worktrees would conflict on merge. Executed sequentially.
