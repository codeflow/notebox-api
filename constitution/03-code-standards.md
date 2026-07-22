# Constitution — Code Standards

> Only the conventions a machine cannot infer from surrounding code. Everything a formatter/linter can
> enforce is delegated to tooling wired into `verify` (`mvn -B verify`). Docs/comments language: English.

## General
- **Match surrounding code:** naming, structure, comment density, idioms.
- **Smallest diff** that satisfies the task; no dead code or commented-out blocks left behind.
- **Tests with code:** every behavioural change ships a test; Gherkin scenarios become executable tests.

## Identifier language — `code_naming: english`
All identifiers are in **English**, including domain terms: `AnnotationType`, `TypeField`, `Annotation`,
`Task`, `Subtask`, `Group`, `Card`, `MessageResolver`, `TenantContext`. No project-language identifiers.
A half-translated identifier (`buscarAnnotation`, `criarTask`) is a violation. Domain nouns (keep this
list in sync with the catalog): **Tenant, User, AnnotationType, TypeField, FieldType, Annotation,
AnnotationValue, Group, Task, Subtask, Card, Image, Message, Locale**.

## Naming that carries meaning
- **Java conventions:** `camelCase` fields/variables (`annotationType`) and methods (`setCadastro()`),
  `PascalCase` types and files (`CadastroProdutos.java`), `UPPER_SNAKE_CASE` constants and enum values.
  (Enforced by Checkstyle — see Tooling.) Identifiers stay English (see Identifier language).
- **`FieldType` enum values:** `TEXT, LIST, NUMBER, FREE_TEXT, SINGLE_CHOICE, MULTIPLE_CHOICE, IMAGE`
  (the closed set of BR-04 — no additions without a BR change).
- **`Priority` enum values:** `LOW, MEDIUM, HIGH, CRITICAL` (D7).
- **i18n message keys:** lowercase, dot-namespaced `domain.area.detail`, e.g.
  `annotation.type.name.required`, `task.subtask.date.invalid`. Every key in code has a matching row per
  locale in the `Message` catalog (BR-08). Keys are referenced, never inlined as literals (AD-05).
- **Card code format** is data, not an identifier: uppercase project prefix + `-` + number (`TST-3456`);
  validated, not hardcoded (C21, G7).

## Field declaration order (reviewer-enforced — a formatter cannot express this)
Within a class, order instance/static fields:
1. **Group by data type.** Fields of the same declared type sit together.
2. **Alphabetical by field name** within each type group.
3. **One blank line between type groups** (and only between groups).

```java
private String annotationType;
private String storeType;

private int flagControlType;
private int flagStatus;

private boolean isActive;
private boolean isSave;
```

If **no type appears more than once** (each type is used by a single field), collapse everything into one
block, alphabetical by name, no blank-line separators:

```java
private boolean isActive;
private int flagStatus;
private String annotationType;
```

## Import order (Checkstyle `CustomImportOrder` enforces it — see Tooling)
1. **Group by top-level package family**, one blank line between groups; alphabetical within a group.
2. If **no package family appears more than once**, collapse into a single alphabetical block.

```java
import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
```
Single-per-family case:
```java
import java.util.List;
import jakarta.transaction.Transactional;
```

## Comments — `comments: minimal`, docs `javadoc` + `openapi`
- One short comment (Javadoc) **above each public class/method** stating what it does and why it exists;
  no inline comments except to state a constraint the code cannot express. Narrating the obvious
  (`i++; // increment`) is a violation.
- **Public surface carries Javadoc** with `@param`/`@return`/`@throws` filled, in English.
- **Every HTTP endpoint is covered by the OpenAPI contract**, updated in the *same feature* that changes
  the endpoint. A drifted spec fails the audit.

## Errors & logging
- **Domain** throws domain exceptions (e.g. `TenantIsolationException`, `AnnotationSchemaException`); it
  never returns HTTP concerns. **`api`** maps exceptions to HTTP responses.
- **One error shape** on the wire: a problem response with a stable machine `code` and a **localized**
  `message` resolved through the catalog (AD-05, BR-08). Never leak stack traces, SQL, or entity internals.
- Exceptions are never swallowed silently. **Structured logging with a correlation id**; never log secrets,
  PII, or another tenant's data.

## Test conventions
- Tests mirror the source package under `src/test/java`. Class `FooTest`; method
  `methodName_condition_expectedResult`.
- **"Unit"** = no container, no DB — domain and application logic (BR-03, BR-06, BR-07 are unit-tested).
- Repository and endpoint tests are **integration** tests and must include a **cross-tenant negative case**
  (C-01/AD-03) wherever they touch tenant-owned data.

## Forbidden (each maps to an architecture boundary — the audit greps these)
- ❌ `EntityManager` / `jakarta.persistence` query use outside `infrastructure/` (AD-02).
- ❌ A repository query on a tenant-owned entity without a tenant predicate / not via the tenant-scoped
  base (AD-03, BR-01/BR-02).
- ❌ A `domain/` type importing `infrastructure/`, `api/`, or `jakarta.ws.rs` (AD-01).
- ❌ A user-facing message as a string literal instead of a catalog key (AD-05, BR-08).
- ❌ Raw image bytes (base64) embedded in list/detail JSON instead of the dedicated binary endpoint (AD-04).
- ❌ Unsanitized rich-text HTML stored or returned (C-08).
- ❌ Hardcoded config/secrets; config comes from env / `.env.example` (C-05).
- ❌ Manual transaction control (`EntityManager.getTransaction()`, `UserTransaction`) — transactions are
  declarative `@Transactional` on application methods (AD-09).
- ❌ Response enveloping or exception-to-HTTP formatting inlined in a resource method instead of a JAX-RS
  `ContainerResponseFilter` / `ExceptionMapper` (AD-11).
- ❌ A managed bean without an explicit CDI scope, or `new` for a collaborator that should be injected (AD-12).
- ❌ Redis/cache client used outside `infrastructure/`, a cache key for tenant data not namespaced by tenant,
  or a cache read treated as the source of truth for a write (AD-13, BR-01).

## Tooling — make it enforceable, don't rely on memory
Mirror the above in the toolchain wired into `verify` / CI so violations fail the build:
- **Spotless** (formatter) + **Checkstyle** (naming, Javadoc-on-public-surface, **`CustomImportOrder`** for
  the import grouping above) — configs to be added with the first `pom.xml`; wire as the `lint` signal
  (currently TODO in CI). **Field declaration order is not expressible in a standard formatter** → it is
  enforced by review and the `/audit` step.
- **Bean Validation** enforces contract invariants at the edge (AD-07).
- `/audit` cross-checks code against this file and the forbidden list.

## Definition of Done (code)
- [ ] `verify` green (`mvn -B verify`: compile + test + package).
- [ ] Follows these standards, the architecture boundaries, and the compliance checklist.
- [ ] Traceable to a spec item (FR/BR/AD/US).
- [ ] No secrets committed.
