# Constitution — Architecture

> Architectural decisions that apply to EVERY feature. Changes require revisiting the related BR
> via the PRD process. Language: English.

**Last sync with PRD:** pre-PRD — grounded in `definitions/INTAKE.md`, `00-principles.md`, and session
decisions of 2026-07-22.
**Project type:** `api` · **Stack:** `Java, Jakarta EE, JPA, CDI, MySQL`

---

## Stack

| Layer | Technology | Notes |
|---|---|---|
| Language | Java | fixed at setup |
| Platform | Jakarta EE | JAX-RS (REST), CDI (injection), Bean Validation |
| Runtime | **Quarkus** | container-native; RESTEasy (JAX-RS), ArC (CDI), Hibernate ORM, SmallRye MicroProfile JWT, Redis client. Decided 2026-07-22 (feat-001 plan). |
| Auth | Stateless JWT (RS256), MicroProfile JWT | OQ-08; keys from secret manager (C-05) |
| Persistence | JPA (Hibernate provider) over MySQL | single relational store (system of record) |
| Cache | Redis | cache-aside, tenant-namespaced keys — AD-13 |
| Transactions | Jakarta Transactions (JTA) | declarative `@Transactional` — AD-09 |
| DI / IoC | CDI | constructor injection; explicit scopes — AD-01, AD-12 |
| Eventing | CDI Events (`Event`/`@Observes`) | in-process change notifications — AD-10 |
| Cross-cutting HTTP | JAX-RS `ContainerResponseFilter` + `ExceptionMapper` | response/error shaping — AD-11 |
| API docs | OpenAPI | kept in sync per endpoint change |
| Consumer | `notebox-web` (react/next) | separate deployable satellite; consumes this API |

## Infrastructure — the home for "which DB / which service"

> Canonical infra decisions. Operational wiring (connection strings, credentials) lives in
> `.env.example` + a secret manager — never hardcode it here.

| Concern | Choice | Provider / service | Status | NFR |
|---|---|---|---|---|
| Relational DB | **MySQL** | TBD environment (local now) | confirmed (K1) | NFR-TBD |
| Binary / image storage | **BLOB in MySQL** | — (in-database) | confirmed (D6) | NFR-TBD |
| Object storage | **none** (deliberately, D6) | — | confirmed — not used | — |
| Cache / ephemeral | **Redis** | TBD (e.g. self-hosted / managed) | confirmed (session 2026-07-22) — AD-13 | NFR-TBD |
| Messaging / push | **none** | — | not used | — |
| Hosting / compute | container (assumed) | TBD | Open Question → G-hosting | NFR-TBD |
| Secrets | secret manager / env | TBD | to confirm at deploy | — |

> Undecided choices stay as Open Questions until fixed — do not invent.

---

## Decisions

### AD-01 — Layered architecture with inward dependency
**Context.** A single team, one deployable API, a rich domain (typed annotations, derived task metrics)
that must stay testable independent of Jakarta and MySQL.
**Decision.** Four layers: `api` (JAX-RS resources / DTOs) → `application` (use-case services,
transactions) → `domain` (entities, invariants, value objects) ← `infrastructure` (JPA repositories,
persistence adapters). Dependencies point inward toward `domain`.
**Consequence.** Domain logic (BR-03, BR-06, BR-07) is unit-testable without a container or DB; wiring
lives at the edges. Costs some mapping boilerplate (DTO ↔ entity).
**Boundary.** No type under `domain/` imports from `infrastructure/`, `api/`, `jakarta.ws.rs`, or
`jakarta.persistence.*` beyond JPA mapping annotations. Checkable by an ArchUnit rule / package-import
grep.

### AD-02 — All persistent state is written only through repositories
**Context.** State integrity (BR-03, BR-05) must be enforced at one seam, not scattered.
**Decision.** The JPA `EntityManager` is used exclusively inside `infrastructure` repository adapters;
`application` and `api` never touch it directly.
**Consequence.** Transaction and tenant-scoping logic has a single home; easy to audit.
**Boundary.** `EntityManager` / `jakarta.persistence` query APIs appear only in `infrastructure/`. No
`EntityManager` reference in `api/` or `application/`. Grep-checkable.

### AD-03 — Tenant scoping is enforced at the repository choke point
**Context.** BR-01/BR-02 demand that no query ever crosses a tenant boundary, and a per-endpoint manual
filter is too easy to forget.
**Decision.** A request-scoped `TenantContext` (CDI) carries the caller's tenant. Every repository over a
tenant-owned entity extends a base that injects a mandatory tenant predicate; there is no code path that
reads or writes a tenant-owned entity without it.
**Consequence.** Isolation is structural, not per-feature discipline. New tenant-owned entities must opt
into the base repository.
**Boundary.** Every tenant-owned entity (annotation type, annotation, field, task, subtask, group, card,
image, and any future one) is fetched only via the tenant-scoped repository base; a query on such an
entity without a tenant predicate is a violation. Enforced by test + code-review rule; the entity set is
listed in `domain/`.

### AD-04 — Images are stored as BLOBs and served through a dedicated binary endpoint
**Context.** D6 fixes image bytes in MySQL, but list endpoints must stay light (K5 thumbnails, C3/C14
grids render many rows).
**Decision.** Binary bytes live in a dedicated column/entity (`@Lob`), with content-type and size
metadata. JSON payloads carry an image **reference** (id + metadata), never the base64 bytes. Bytes are
retrieved via a separate `GET …/image/{id}` endpoint that streams them.
**Consequence.** Grids and detail JSON stay small; thumbnails are a rendering concern for `notebox-web`.
Large blobs still pressure MySQL — bounded by AD-07 validation.
**Boundary.** No REST list/detail JSON response embeds raw image bytes; image binaries are returned only
from the dedicated binary endpoint with a binary content type. Checkable by response-schema review.

### AD-05 — All user-facing system strings resolve through a message catalog
**Context.** BR-08 requires every emitted string localizable (en + pt), editable at runtime (C32).
**Decision.** A `Message` store (key + locale → value) backed by MySQL, resolved through a single
`MessageResolver`. Validation/error/notification text references a key; the catalog is CRUD-editable via
a dedicated admin endpoint. Request locale comes from an explicit resolution order (settled at PRD, G10).
**Consequence.** No recompile to change wording; translators work through the API. Adds a resolution
lookup on user-facing responses.
**Boundary.** User-facing message text is never a string literal in `api/`, `application/`, or `domain/`
responses — it is always a catalog key resolved by `MessageResolver`. Enforced by code-review rule +
message-key lint. (Domain-content values the user typed — type/field/group names — are exempt: D5.)

### AD-06 — Synchronous REST/JSON; the UI is a separate deployable
**Context.** The front end is the `notebox-web` satellite (K2); this repo is a stateless API.
**Decision.** All interaction is synchronous HTTP/JSON over JAX-RS. No server-rendered UI, no async
messaging, no server push in this repo. `notebox-web` owns all rendering (datagrids, popups, rich-text
editor, badges, sliders).
**Consequence.** Clear contract seam; the API is independently testable and versionable. Any future
real-time need is a new, explicit decision.
**Boundary.** This repo contains no UI templating/rendering and no message-broker client; the only inbound
surface is JAX-RS resources returning JSON (+ the binary image endpoint). Grep-checkable.

### AD-07 — Bean Validation guards the contract at the API edge
**Context.** BR-03/BR-04 invariants and blob-size safety (AD-04) must be enforced before persistence.
**Decision.** Request DTOs use Jakarta Bean Validation; annotation-value conformance to its type (BR-03),
the closed field-type set (BR-04), and image size bounds are validated in `application` before write.
**Consequence.** Invalid data is rejected at the edge with localized messages (AD-05); domain stays clean.
**Boundary.** No write use-case persists a tenant-owned entity without passing validation; unbounded image
uploads are rejected. Checkable by test.

### AD-08 — Deliberately NOT abstracted yet
**Context.** Avoid premature frameworks the brief does not require.
**Decision.** No object-storage abstraction, no plugin system for field types (the set is closed, BR-04),
no generic multi-datastore abstraction, no **external** message broker / event bus. Add any of these only
when a concrete feature forces the decision. (In-process **CDI events** are used — AD-10 — and are not an
external event bus. **Caching is used** — AD-13.)
**Consequence.** Less indirection now; a future need reopens the specific decision as an Open Question.
**Boundary.** No object-storage SDK, external message broker, or field-type plugin SPI enters the codebase
without a new AD recorded here.

### AD-09 — Declarative transaction demarcation
**Context.** State-integrity rules (BR-03, BR-05) and multi-write use cases need reliable commit/rollback,
and hand-rolled transaction handling is error-prone (session 2026-07-22 decision).
**Decision.** Transactions are demarcated declaratively with `@Transactional` (Jakarta Transactions) on
`application`-layer service methods: commit on normal return, rollback on runtime and domain exceptions.
`rollbackOn` / `dontRollbackOn` are set explicitly where the default (rollback on unchecked only) is wrong.
**Consequence.** One transactional boundary per use case; no bean manages its own transaction.
**Boundary.** No manual transaction control (`EntityManager.getTransaction()`, `UserTransaction`) anywhere;
a write use case is a `@Transactional` application method. Grep-checkable.

### AD-10 — Intra-application change notifications use CDI Events
**Context.** Some state changes must notify other components without hard coupling — e.g. completing or
adding a subtask must recompute the parent task's derived status (BR-06) and dates (BR-07) (session
2026-07-22 decision).
**Decision.** Producers fire `jakarta.enterprise.event.Event<T>`; consumers react with `@Observes`
(synchronous, in-transaction) or `@ObservesAsync` where the reaction may be out-of-band. Events model
domain facts (e.g. `SubtaskCompleted`, `SubtaskAdded`), not commands.
**Consequence.** "On update" reactions are decoupled and independently testable; ordering/transaction
semantics of `@Observes` must be understood (it runs in the firing transaction).
**Boundary.** Cross-component "react to a change" wiring is done via CDI events where it decouples
otherwise-unrelated services — not by one service directly calling another's internals.

### AD-11 — Cross-cutting response & error handling via JAX-RS providers
**Context.** Responses need uniform shaping (correlation id, standard envelope/headers) and a single
localized error shape (AD-05, BR-08); duplicating this per resource is the failure mode (session
2026-07-22 decision — the Spring `ResponseBodyAdvice`/`@ControllerAdvice` analogue).
**Decision.** A `ContainerResponseFilter` applies cross-cutting response concerns on the way out; an
`ExceptionMapper` per domain-exception family maps to the uniform problem response with a machine `code`
and a catalog-resolved localized `message`. Resources return domain/DTO results and do not format errors.
**Consequence.** Consistent envelope and error contract across every endpoint; the OpenAPI doc documents it
once.
**Boundary.** Response enveloping and exception-to-HTTP mapping live only in JAX-RS filters/`ExceptionMapper`
providers — never inlined or duplicated in resource methods. Grep-checkable.

### AD-12 — Dependency injection, IoC, and explicit bean scopes (CDI)
**Context.** The app is wired with CDI; scopes must be deliberate, and the correct scope depends on whether
a component holds state and for how long (session 2026-07-22 decision). Note the taxonomy: **CDI scopes**
are `@ApplicationScoped`, `@RequestScoped`, `@SessionScoped`, `@ConversationScoped`, `@Dependent`; **EJB**
adds `@Stateless`, `@Stateful`, `@Singleton`; `@Context` is JAX-RS *injection*, not a scope.
**Decision.** All collaborators are obtained by **injection** (constructor injection preferred); managed
beans are never instantiated with `new`. Every managed bean declares an explicit scope by role:
- `@ApplicationScoped` — stateless services, repositories, `MessageResolver`, JAX-RS resource classes (the default).
- `@RequestScoped` — per-request context: `TenantContext`, request-locale holder.
- `@SessionScoped` / EJB `@Stateful` — **only** for genuinely conversational, multi-request state, used
  deliberately and justified in the feature spec.
- `@Stateless` (EJB) is treated as equivalent to an `@ApplicationScoped` stateless service; pick one style
  per component and stay consistent.
**Consequence.** Scope intent is explicit and reviewable. **Tension with AD-06 (stateless REST):** the
default is request/application-scoped and token-based; adopting `@SessionScoped`/`@Stateful` introduces
server-side session state, which must be an explicit, justified choice (see Open Questions) rather than a
default — token vs server-session auth is decided at PRD.
**Boundary.** No managed bean without an explicit scope; no `new` for a collaborator that should be injected;
`@SessionScoped`/`@Stateful` usage names its justification in the owning feature spec. Reviewable + partly
grep-checkable.

### AD-13 — Caching with Redis (cache-aside; MySQL stays the source of truth)
**Context.** Hot, read-heavy, rarely-changing data — the i18n message catalog (AD-05), annotation-type
schemas (BR-03), possibly derived task metrics — benefits from caching (session 2026-07-22 decision).
**Decision.** Redis is the cache, used **cache-aside**: read from cache, fall back to MySQL on a miss and
populate; **invalidate on write**. MySQL remains the system of record — the cache is never authoritative and
its loss only costs latency. Cache access is confined to the `infrastructure` layer behind a caching port,
so `application`/`domain` are unaware of it. Entries carry a TTL.
**Consequence.** Lower read latency and DB load; adds an invalidation obligation on every write path and a
new operational dependency (Redis availability — a miss must degrade to MySQL, never to an error).
**Boundary (tenant safety — protects BR-01):** every cache key for tenant-owned data is namespaced by
tenant id; no cache key mixes tenants, and a cache lookup never returns another tenant's value. Redis client
usage lives only in `infrastructure/`; a cache is never read as the source of truth for a write decision.
Grep-checkable (client confined to infra) + test (tenant-namespaced keys, miss-degrades-to-DB).

---

## Data model (high level)

Tenant-owned aggregate roots (each carries a tenant boundary — AD-03):

- **AnnotationType** `1—*` **TypeField** (field: name, fieldType ∈ closed set, icon image ref, visibleForViewing) `1—*` **FieldOption** (label + optional badge colour) for list/choice types — options live on the field definition (OQ-03, 2026-07-22).
- **Annotation** (of an AnnotationType) `1—*` **AnnotationValue** (per TypeField).
- **Task** `1—*` **Subtask**; Task has priority (enum), derived status % (BR-06) and derived dates (BR-07); optional **Card** inline value object (id + optional URL) on Task and Subtask (OQ-06, 2026-07-22); optional rich-text **details**.
- **Group** organizes Annotations/Tasks in the nav tree (G5 fixes multiplicity/nesting/shared-vs-separate).
- **Image** (BLOB + metadata) referenced by type icons, image fields, rich-text embeds (AD-04).
- **Message** (key, locale, value) — the i18n catalog (AD-05).
- **Tenant** / **User** — the tenancy root; users belong to tenants (D4).

## External integrations

| System | Purpose | Status |
|---|---|---|
| MySQL | sole persistent store (relational + BLOBs + message catalog) | confirmed |
| `notebox-web` | front-end consumer of this API | confirmed (satellite) |
| Card URLs | outbound hyperlinks only (C21) — not an integration | confirmed, no coupling |

## Open architectural decisions (→ Open Questions at `catalogs`)
- Request-locale resolution order and missing-translation fallback (INTAKE G10) — bounds AD-05.
- Hosting/compute target and secrets provider — infra table.
- Card as shared entity vs inline value object (INTAKE G7) — shapes the data model above.
- Group semantics: nesting, multi-membership, shared vs per-domain (INTAKE G5).
- ~~Auth model: token-based stateless vs server-side session~~ **RESOLVED (2026-07-22, OQ-08): stateless
  signed JWT.** `@SessionScoped`/`@Stateful` beans are not used; AD-06 stands, AD-12 default confirmed.
