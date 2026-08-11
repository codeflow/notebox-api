# Feature — Annotation records: CRUD conforming to type, with secret-value encryption & audited reveal

**ID:** features/005-annotation-records-notebox-api
**User Story:** US-2.1
**Version:** v2 — adds F4 PUT-secret semantics (2026-07-25) and OQ-17/OQ-18 type-edit guards (2026-08-03)
**Status:** Approved (human approval 2026-08-03; v1 approved 2026-07-24, reopened by audit verdict 2026-07-25)
**Date:** 2026-08-03

## Origin
- **User Story:** US-2.1 — *As a tenant member, I want to create/edit/delete annotations of a type, so I record real data.*
- **FRs covered:** FR-04 (CRUD annotation records conforming to their type), FR-06 (explicit, irreversible delete of a record), FR-18 (secret field **values** encrypted at rest + role-gated audited reveal).
- **BRs bound:** BR-03 (an annotation conforms to its type — the type *is* the schema), BR-05 (destructive deletions are explicit and irreversible), BR-10 (secret field values are confidential at rest).
- **ADs bound:** AD-03 (tenant-scoped repository choke point), AD-14 (encryption at rest: AES-256-GCM, app master key from secret manager, key-version rotation, crypto confined to `infrastructure/`).
- **Primary source:** PRD v2 §3.1 (FR-04, FR-06, FR-18), §3 Validation baseline (OQ-09, OQ-14), §4 acceptance (FR-04, FR-18); constitution BR-03/BR-05/BR-10, AD-14, C-12.
- **Consumes:** feat-003-annotation-types contract — the `AnnotationType` schema (fields with `fieldType` ∈ 7, `numberMin/max`, `secret`, ordered `options` with labels/badge colours). A record's values are validated against that schema.
- **Refines (OQ-14, 2026-07-24):** deleting an annotation **type** is blocked while it still owns records. feat-003 deletes only empty types; this feature adds the record-existence guard to the type-delete path.
- **Refines (OQ-17, 2026-08-03, audit F5):** editing a type's fields while records exist **preserves field identity** (and therefore all values) for unchanged fields and identical resends; **removing or retyping a field is blocked** while records exist; adding a field stays allowed. Full type-evolution/migration is a future feature.
- **Refines (OQ-18, 2026-08-03, audit F12):** a field's **Secret flag cannot change** — in either direction — while the field still has values; the values must be cleared explicitly first (BR-10 can never be violated by a flip).

## Summary
A tenant member creates, reads, updates and deletes **annotation records** — instances of an annotation
type from feat-003. A record carries a **name** and a **value per defined field**, each value conforming to
its field's type (BR-03): text for Text/Free text, a bounded number for Number, a reference to a predefined
option for List/Single choice, a set of predefined options for Multiple choice, and an image reference for
Image. Values are accepted **only for fields the type defines**, and every rejection carries a localized
message. Deleting a record is explicit and irreversible (BR-05), with an audit entry. Values of fields the
type flagged **Secret** (Text/Free text only) are stored **encrypted at rest** (AES-256-GCM, AD-14), returned
**masked** on ordinary reads, and disclosed in cleartext only through a dedicated **reveal** action available
to an elevated role, with each reveal audited (BR-10, FR-18, C-12). This feature is the **API contract only**;
it renders no UI, and the multi-record grid/listing projection (FR-05) belongs to US-2.2.

## Scope
- **In:**
  - **Create / read-one / update / delete** an annotation record of a given type, scoped to the caller's
    tenant behind JWT auth (feat-001), through the tenant repository choke point (AD-03, C-01).
  - Record attributes: a required **name**; an unordered set of **values**, at most one per defined field.
  - **Conformance validation (BR-03)** on create and update:
    - a value may target **only a field the type defines** (unknown field → rejected);
    - each value must match its field's `fieldType` (e.g. non-numeric into Number → rejected);
    - **Number** values must respect the field's `numberMin`/`numberMax` bounds when present;
    - **List / Single choice** values must reference **one** of the field's predefined options; **Multiple
      choice** values reference a set of them; an unknown option is rejected;
    - **Image** values reference an uploaded image (the feat-003 binary-upload mechanism, AD-04);
    - a field with no value is allowed unless a future per-field "required" rule says otherwise (none defined
      today — validation baseline lists no per-field requiredness for record values).
  - **Explicit, irreversible delete** of a record (FR-06, BR-05) with an audit entry (C-10).
  - **Secret values (FR-18, BR-10, AD-14, C-12):**
    - a value written to a **Secret**-flagged field is persisted **encrypted** (AES-256-GCM, random per-value
      IV, app master key from the secret manager, key-version tag) — never as cleartext;
    - ordinary reads of the record return Secret values **masked** (a placeholder, never the cleartext);
    - a dedicated **reveal** action returns the cleartext of a specific Secret value **only** to a caller
      holding the elevated **reveal role** (grounded as the Tenant administrator — see Open Questions), and
      **every reveal writes an audit entry** (who revealed which value, when).
  - **Type-delete guard (OQ-14):** deleting an annotation type is rejected while it owns ≥1 record.
  - **Type field-edit guard (OQ-17, audit F5):** editing a type preserves the identity of unchanged fields —
    resending an identical definition (or renaming the type) never orphans a record's values; **removing or
    retyping a field while the type owns ≥1 record is rejected** with a localized error; adding a new field
    remains allowed.
  - **Secret-flag flip guard (OQ-18, audit F12):** changing a field's Secret flag — in either direction — is
    rejected while the field still has values; the values must be removed explicitly first, so no
    cleartext-at-rest state can ever form (BR-10).
  - Localized (en/pt) validation/error messages for every rejection (C-09), with specific domain exceptions
    and dot-namespaced keys per constitution 03-code-standards.
  - OpenAPI documentation for every endpoint (NFR-06).
- **Out:**
  - **Multi-record listing / grid projection** returning only visible-for-viewing columns, and the all-fields
    detail projection — **FR-05, US-2.2**. This feature's read returns a single record; the visible/detail
    column semantics and pagination (NFR-08) are US-2.2.
  - The **annotation-record UI** (create/edit form, datatable, masked value with a reveal icon) —
    feat-006-annotation-records-notebox-web.
  - **Annotation types** themselves (CRUD, fields, options, icons) — delivered by feat-003. This feature only
    *adds* the record-existence guards to the existing type-delete and type-edit paths (OQ-14, OQ-17, OQ-18).
  - **Full type-evolution / migration semantics** — migrating or nulling values when a field is removed or
    retyped, encrypt/decrypt migration on a Secret-flag flip, and evolution rules for mutations *inside* a
    preserved field (option-list edits, Number-bound changes — which keep feat-003's existing replace
    semantics for now) — deferred to a dedicated future feature *(decisões humanas 2026-08-03, OQ-17/OQ-18)*.
  - **Groups / navigation** placement of records — FR-08/09, E3.
  - **Master-key rotation tooling / KMS** — AD-14 fixes the scheme (single app master key from the secret
    manager, key-version tagged); operational rotation procedure is deploy-time, not a behavioural criterion
    here. Reads must decrypt any still-supported key version.
  - **Redis caching** of records (NFR-03) — a plan-level optimization; correctness must not depend on it.
  - Rich-text / WYSIWYG values — Free text is plain text; WYSIWYG is Task details (US-4.2). (C-08 n/a here.)

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-04 Create and read annotation records conforming to their type
  Scenario: Create a record with values for defined fields
    Given tenant A has a type "RabbitMQ" with fields URL (Text), Port (Number 1..65535), Environment (Single choice: dev, staging, prod)
    And an authenticated member of tenant A
    When they create a record named "prod-broker" with URL="amqp://h", Port=5672, Environment=prod
    Then the record is persisted and a record id is returned
    And reading the record returns name "prod-broker" and those three values

  Scenario: Reject a value for a field the type does not define
    Given tenant A has a type "RabbitMQ" with fields URL, Port, Environment
    When a member of tenant A creates a record with a value for field "Region"
    Then the request is rejected with error code annotation.record.field.unknown
    And the message is localized to the caller's locale

  Scenario: Reject a value whose data does not match the field type
    Given tenant A has a type "RabbitMQ" with a Number field "Port"
    When a member of tenant A creates a record with Port="not-a-number"
    Then the request is rejected with error code annotation.record.value.type_mismatch

  Scenario: Reject a Number value outside the field bounds
    Given tenant A has a type with a Number field "Port" bounded 1..65535
    When a member of tenant A creates a record with Port=70000
    Then the request is rejected with error code annotation.record.value.number.out_of_bounds

  Scenario: Choice value must reference a predefined option
    Given tenant A has a type with a Single choice field "Environment" (dev, staging, prod)
    When a member of tenant A creates a record with Environment="qa"
    Then the request is rejected with error code annotation.record.value.option.unknown

  Scenario: Multiple choice accepts several predefined options
    Given tenant A has a type with a Multiple choice field "Tags" (urgent, ops, db)
    When a member of tenant A creates a record with Tags=[ops, db]
    Then the record persists both selected options

  Scenario: Reject a record with no name
    Given an authenticated member of tenant A
    When they create a record with an empty name
    Then the request is rejected with error code annotation.record.name.required

  Scenario: Update changes a record's values
    Given tenant A has a record "prod-broker" with Port=5672
    When a member of tenant A updates Port to 5673
    Then reading the record returns Port=5673

Feature: FR-06 Explicit, irreversible delete of a record
  Scenario: Delete removes the record and audits it
    Given tenant A has a record "prod-broker"
    When a member of tenant A deletes it
    Then the record and its values are removed
    And an audit entry records who deleted which record and when
    And a subsequent read of the record returns not found

Feature: FR-18 Secret values are encrypted at rest and revealed under audit
  Scenario: A secret value is stored as ciphertext, never cleartext
    Given tenant A has a type with a Text field "API key" flagged Secret
    When a member of tenant A creates a record with "API key"="s3cr3t-token"
    Then the stored value is ciphertext (AES-256-GCM) carrying an IV and key-version, not "s3cr3t-token"

  Scenario: Ordinary read masks a secret value
    Given tenant A has a record whose Secret field "API key" holds a value
    When a member of tenant A reads that record without requesting a reveal
    Then the "API key" value is returned masked and never in cleartext

  Scenario: Reveal returns cleartext to the elevated role and is audited
    Given tenant A has a record whose Secret field "API key" holds "s3cr3t-token"
    And a caller of tenant A holding the reveal role
    When they request the reveal of that record's "API key"
    Then the cleartext "s3cr3t-token" is returned
    And an audit entry records who revealed which value on which record and when

  Scenario: Reveal is denied to a plain member
    Given tenant A has a record whose Secret field "API key" holds a value
    And an authenticated member of tenant A without the reveal role
    When they request the reveal of that record's "API key"
    Then the request is rejected with error code annotation.record.secret.reveal.forbidden
    And no cleartext is returned

Feature: FR-18 PUT preserves a secret value unless erasure is explicit (human decision 2026-07-25, audit F4)
  Scenario: Renaming a record preserves its secret value
    Given tenant A has a record whose Secret field "API key" holds "s3cr3t-token"
    When a member of tenant A updates the record's name, omitting "API key" from the values
    Then the update succeeds
    And revealing "API key" afterwards still returns "s3cr3t-token"

  Scenario: Echoing back a masked secret value is a no-op, not an error
    Given tenant A has a record whose Secret field "API key" holds "s3cr3t-token"
    When a member of tenant A submits an update echoing "API key" exactly as the masked read returned it (text = null, no clearSecret)
    Then the update succeeds
    And revealing "API key" afterwards still returns "s3cr3t-token"

  Scenario: Sending a new value re-encrypts the secret
    Given tenant A has a record whose Secret field "API key" holds "s3cr3t-token"
    When a member of tenant A updates "API key" to "rotated-token"
    Then the stored value is ciphertext of the new value under the active key version
    And revealing "API key" returns "rotated-token"

  Scenario: Erasing a secret requires the explicit flag and is audited
    Given tenant A has a record whose Secret field "API key" holds "s3cr3t-token"
    When a member of tenant A updates "API key" with clearSecret = true
    Then the stored secret is erased
    And an audit entry records who erased which secret value on which record and when
    And revealing "API key" afterwards is rejected with error code annotation.record.reveal.not_secret

Feature: OQ-14 Type deletion is blocked while records exist
  Scenario: Deleting a type that owns records is rejected
    Given tenant A has a type "RabbitMQ" with at least one record
    When a member of tenant A deletes the type "RabbitMQ"
    Then the request is rejected with error code annotation.type.has_records
    And the type and its records are unchanged

  Scenario: Deleting a type with no records still succeeds
    Given tenant A has a type "Empty" with no records
    When a member of tenant A deletes the type "Empty"
    Then the type is deleted (feat-003 behaviour preserved)

Feature: OQ-17 Type field edits preserve values and block destructive changes while records exist (human decision 2026-08-03, audit F5)
  Scenario: Resending an identical type definition preserves every record's values
    Given tenant A has a type "RabbitMQ" with fields URL, Port, Environment and a record "prod-broker" holding values for all three
    When a member of tenant A updates the type "RabbitMQ" resending exactly the same definition
    Then the update succeeds
    And reading "prod-broker" still returns its three values

  Scenario: Renaming a type preserves its records' values
    Given tenant A has a type "RabbitMQ" with a record "prod-broker" holding values
    When a member of tenant A updates the type changing only its name to "RabbitMQ brokers"
    Then the update succeeds
    And reading "prod-broker" still returns its values

  Scenario: Adding a field to a populated type is allowed
    Given tenant A has a type "RabbitMQ" with at least one record
    When a member of tenant A updates the type adding a new Text field "VHost"
    Then the update succeeds
    And existing records keep their values, with no value for "VHost"

  Scenario: Removing a field is rejected while records exist
    Given tenant A has a type "RabbitMQ" with field "Port" and at least one record
    When a member of tenant A updates the type omitting "Port" from the fields
    Then the request is rejected with error code annotation.type.field.has_records
    And the type definition and every record's values are unchanged

  Scenario: Changing a field's type is rejected while records exist
    Given tenant A has a type "RabbitMQ" with a Number field "Port" and at least one record
    When a member of tenant A updates the type changing "Port" to a Text field
    Then the request is rejected with error code annotation.type.field.has_records
    And the type definition and every record's values are unchanged

  Scenario: Removing a field from a type with no records still succeeds
    Given tenant A has a type "Empty" with field "Port" and no records
    When a member of tenant A updates the type omitting "Port" from the fields
    Then the update succeeds (feat-003 behaviour preserved)

Feature: OQ-18 A field's Secret flag cannot change while it holds values (human decision 2026-08-03, audit F12)
  Scenario: Flagging a field Secret is rejected while it holds values
    Given tenant A has a type with a Text field "Notes", not Secret, and a record holding Notes="hello"
    When a member of tenant A updates the type flagging "Notes" as Secret
    Then the request is rejected with error code annotation.type.field.secret_flip.has_values
    And "Notes" remains readable on ordinary reads

  Scenario: Un-flagging a Secret field is rejected while it holds values
    Given tenant A has a type with a Secret Text field "API key" and a record holding an encrypted value for it
    When a member of tenant A updates the type removing the Secret flag from "API key"
    Then the request is rejected with error code annotation.type.field.secret_flip.has_values
    And the stored value remains encrypted at rest and masked on ordinary reads

  Scenario: Flipping the Secret flag succeeds once the field's values are cleared
    Given tenant A has a type with a Text field "Notes" and no record holds a value for "Notes"
    When a member of tenant A updates the type flagging "Notes" as Secret
    Then the update succeeds
    And a value subsequently written to "Notes" is stored encrypted and masked on ordinary reads

Feature: NFR-01 / C-01 Tenant isolation on records
  Scenario: A member cannot create a record for another tenant's type
    Given tenant B owns a type "RabbitMQ"
    When a member of tenant A creates a record of tenant B's "RabbitMQ"
    Then the API responds 404/403 and no record is created

  Scenario: A member cannot read, update, or delete another tenant's record
    Given tenant A owns record X
    When a member of tenant B requests, updates, or deletes record X
    Then the API responds 404/403 and no data of X is returned or changed
```

## Compliance pre-flight
Checklist copied from `constitution/02-compliance.md`, each item marked:

- **C-01 · Tenant isolation** — **applies.** Records are tenant-owned; all access via the AD-03 choke point. *Evidence:* cross-tenant authorization tests (create/read/update/delete/reveal).
- **C-02 · Authenticated by default** — **applies.** All record endpoints require auth + tenant context. *Evidence:* 401/403 test on unauthenticated/foreign-tenant access.
- **C-03 · Least-privilege authorization** — **applies.** The **reveal** action is elevated (reveal role), beyond plain tenant membership. *Evidence:* role-based authz test (reveal allowed for the role, forbidden for a plain member).
- **C-04 · Personal data minimization** — **not applicable.** The feature stores user-authored **domain** values, not system-collected user/tenant identity PII; ownership metadata is already covered by feat-001. (Sensitive user-entered secrets are handled by C-12, not C-04.)
- **C-05 · Secrets never committed** — **applies.** The AES master key is loaded from the secret manager, never in repo/image/logs. *Evidence:* `.env.example` key entry + secret scan; no key in logs.
- **C-06 · Encryption in transit** — **applies (inherited).** All endpoints served over TLS in deployed envs. *Evidence:* deployment/ingress config (cross-cutting, not feature-specific).
- **C-07 · Image upload safety** — **applies when Image-field values are set.** Image values reuse feat-003's validated binary upload/serve (AD-04, size/content-type bound). *Evidence:* reuse of the validated image path + content-type assertion.
- **C-08 · Rich-text sanitization** — **not applicable.** Free text is plain text; WYSIWYG rich text is Task details (US-4.2), not annotation values.
- **C-09 · Localization completeness** — **applies.** Every validation/error message resolves in en + pt. *Evidence:* message-catalog coverage check per locale.
- **C-10 · Audit trail for irreversible & admin actions** — **applies.** Record delete (BR-05) and every secret **reveal** are audited. *Evidence:* audit-log entry + test for delete and reveal.
- **C-11 · Data retention & deletion path** — **applies (record-level).** The explicit record delete (FR-06) is the removal path for record data. *Evidence:* delete removes record + values; documented in this spec. (Tenant/account-level retention remains a provisioning-feature concern.)
- **C-12 · Encryption at rest for secret values** — **applies (core).** Secret values stored AES-256-GCM encrypted; reveal role-gated + audited. *Evidence:* ciphertext-at-rest test (stored bytes ≠ plaintext) + reveal authorization/audit test.

## Out of scope
See **Scope · Out** above — chiefly: the multi-record **listing/detail projection** (FR-05, US-2.2), the
**web UI** (feat-006), annotation **type** CRUD (feat-003), and **groups/navigation** (E3).

## Open Questions / decisions

- **PUT semantics for Secret values — DECIDED (human decision 2026-07-25, raised by audit finding F4).**
  Omitting a Secret field from a `PUT` **preserves** the stored ciphertext; erasing it requires an
  explicit `clearSecret: true` and is **audited**; sending a new `text` re-encrypts under the active key
  version; echoing back the masked `text: null` is a **no-op preserve**, never a `type_mismatch` error.
  Rationale: BR-05 (destructive deletion is explicit) + BR-10 (accountability), and it makes feat-006's
  edit form implementable. Contract updated in `contracts/rest-api.md`; 4 new scenarios above.

- **Type field edits vs existing records — DECIDED (human decision 2026-08-03, OQ-17, raised by audit
  finding F5).** The type update **preserves field identity** — and therefore every record's values — for
  unchanged fields, identical resends and type renames; **removing or retyping a field while the type owns
  ≥1 record is rejected** with a localized error; **adding a field remains allowed**. The interim guard
  ships in this feature (symmetric to OQ-14's delete guard); full type-evolution/migration semantics are a
  dedicated future feature. Rationale: audit F5 proved the previous behaviour destroyed every value of every
  record on an *identical* PUT — silent, irreversible data loss on the most innocent input.

- **Secret-flag flip — DECIDED (human decision 2026-08-03, OQ-18, raised by audit finding F12).** Changing
  a field's Secret flag — in either direction — is **rejected while the field still has values**, with a
  localized error; the values must be cleared explicitly first. No cleartext-at-rest state can ever form
  (BR-10 holds structurally). Migration-on-flip (encrypt existing values when flagging, audited decrypt when
  un-flagging) deferred as a possible future feature.

- **Reveal-role binding — DECIDED (human decision 2026-07-24, at spec approval).** The elevated "reveal role"
  of FR-18 / BR-10 / AD-14 is the **Tenant administrator** (the only elevated persona in the PRD; C-03
  precedent for elevated actions). Not a dedicated secret-reveal permission. The reveal scenarios' "reveal
  role" therefore binds to Tenant administrator. No open questions remain for this feature.
