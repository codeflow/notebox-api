# Feature — Define annotation types with icon and typed fields

**ID:** features/003-annotation-types
**User Story:** US-1.1
**Version:** v1
**Status:** Draft
**Date:** 2026-07-23

## Origin
- **User Story:** US-1.1 — *As a tenant member, I want to define an annotation type with an icon and typed fields (incl. list options), so I capture a category of notes consistently.*
- **FRs covered:** FR-01 (CRUD annotation types), FR-02 (field declaration: name, field type ∈ 7, optional icon, visible flag + defaults), FR-03 (options on List/Single/Multiple fields), FR-07 (image storage — scoped here to **type and field icons**).
- **BRs bound:** BR-03 (the type *is* the schema of its annotations), BR-04 (field types are a closed set of 7), BR-05 (type deletion is explicit and irreversible).
- **Primary source:** PRD v1 §3.1 (FR-01, FR-02, FR-03, FR-07), §3 Validation baseline (OQ-09), §4; INTAKE C1–C12, K3, K4, D2, D6.
- **Added by human decision (2026-07-23):** a **"Secret" flag** on Text/Free text field definitions. Only the *flag declaration* lands here; the encrypt-at-rest + role-gated audited reveal of values is a new requirement (future FR/AD/compliance/BR) deferred to US-2.1, tracked in **OQ-15**.

## Summary
A tenant member can create, read, update and delete **annotation types** — the reusable schemas that
later drive annotation records (US-2.1) and their grids (US-1.2/US-2.2). A type has a name (unique within
the tenant), an optional icon image, and an **ordered** list of field definitions. Each field declares a
name, exactly one of the seven field types, an optional icon, and a "visible for viewing" flag. List,
Single choice and Multiple choice fields carry an ordered set of predefined options (List options may also
carry a badge colour from a fixed palette). This feature delivers **only the type-definition contract**; it
stores no annotation records and renders no UI.

## Scope
- **In:**
  - CRUD of annotation types scoped to the caller's tenant, behind JWT auth (feat-001 infrastructure).
  - Type attributes: name (required, unique per tenant), optional icon image, ordered field list.
  - Field declaration: name (required), field type (required) ∈ {Text, List, Number, Free text, Single
    choice, Multiple choice, Image}, optional icon, "visible for viewing" flag whose default is **per field
    type** (D2: Text/List/Number → `true`; Free text/Single choice/Multiple choice/Image → `false`) when
    omitted, field order preserved as declared.
  - Number fields: optional per-field `min` / `max` bounds.
  - **"Secret" flag** on **Text** and **Free text** fields only: a boolean declared on the field
    definition (defaults to `false`). This feature only *declares and stores* the flag on the type
    schema — see Out of scope for what it later drives. *(Origin: human decision 2026-07-23; tracked in
    [TBD — OQ-15].)*
  - Options for **List / Single choice / Multiple choice** fields: ordered, each with a label; **List**
    options additionally allow an optional badge colour from the fixed palette {red, green, blue, black,
    gray, yellow}.
  - Icon images (type icon and field icon): stored as MySQL BLOBs with content-type/size metadata,
    retrieved via a dedicated binary endpoint, with thumbnail rendering support. Accepted formats
    PNG/JPEG/GIF/WebP, max 5 MB (NFR-04).
  - Localized validation/error messages (en/pt) for every rejection (C-09).
  - Audit record for the irreversible type delete (C-10, BR-05).
  - OpenAPI documentation for every endpoint (NFR-06).
- **Out:**
  - Annotation **records** and their values — FR-04, US-2.1. Includes uploading **Image-field values** and
    rich-text embeds (FR-07's other cases); only type/field **icons** are handled here.
  - **Listing/detail behaviour** that consumes the "visible for viewing" flag — FR-05, US-1.2 / US-2.2.
    This feature only *declares and stores* the flag and its default.
  - Groups and navigation tree — FR-08/09, E3.
  - The type-builder **UI** — feat-004-annotation-types-web (notebox-web).
  - **Encryption, decryption and reveal of Secret field VALUES.** Storing a Secret value encrypted at
    rest, and revealing it in cleartext only to an elevated role with an audit entry, operate on
    annotation **records** (FR-04) — deferred to **US-2.1**, and gated on the crypto mechanism & key
    management in **[TBD — OQ-15]** (which also requires a PRD v2 + constitution AD/compliance/BR before
    implementation). The **UI** (Secret checkbox in the builder; masked-by-default display with a reveal
    icon in the field and in the datatable) belongs to **notebox-web** (feat-004 / US-2.2). feat-003
    ships only the flag on the type schema.
  - **Rich-text / WYSIWYG editing.** The "Free text" field type (C9) is a plain **textarea**; the
    WYSIWYG rich-text editor (C27 — font styles, colour, bold, embedded images) is **Task details**
    (FR-14 → US-4.2, epic E4), a separate feature. It is not a field type and is not delivered here.
  - Deleting a type that already **owns annotation records** (cascade vs block) — no records can exist until
    US-2.1, so the policy is deferred there. Tracked as **[TBD — OQ-14]**.
  - Redis caching of type schemas (NFR-03) — a non-functional optimization the `plan` addresses; reads must
    be correct regardless of cache, so it is not a behavioural criterion of this spec.
  - Pagination of the type list — NFR-08 targets annotation/task lists, not type lists; a tenant's type
    count is small and bounded by usage.

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-01 Manage annotation types
  Scenario: Create a type with ordered fields
    Given an authenticated member of tenant A
    When they create a type "RabbitMQ" with fields [URL (Text), Environment (Single choice), Description (Free text)] in that order
    Then the type is persisted and a type id is returned
    And reading the type returns its fields in the order URL, Environment, Description

  Scenario: Reject a type with no name
    Given an authenticated member of tenant A
    When they create a type with an empty name
    Then the request is rejected with error code annotation.type.name.required (Bean Validation)
    And the message is localized to the caller's locale

  Scenario: Type name is unique within a tenant
    Given tenant A already has a type named "RabbitMQ"
    When a member of tenant A creates another type named "RabbitMQ"
    Then the request is rejected with AnnotationTypeNameTakenException (code annotation.type.name.taken, 409)

  Scenario: The same type name may exist in two different tenants
    Given tenant A has a type named "RabbitMQ"
    When a member of tenant B creates a type named "RabbitMQ"
    Then the type is created successfully for tenant B

  Scenario: Update reorders fields
    Given tenant A has a type "RabbitMQ" with fields [URL, Environment, Description]
    When a member of tenant A updates it to order [Description, URL, Environment]
    Then reading the type returns fields in the order Description, URL, Environment

  Scenario: Delete a type is explicit and irreversible
    Given tenant A has a type "RabbitMQ" with no annotation records
    When a member of tenant A deletes the type
    Then the type and its field/option definitions are removed
    And an audit entry records who deleted what and when
    And a subsequent read of the type returns not found

Feature: FR-02 Declare typed fields
  Scenario: All seven field types are accepted
    Given an authenticated member of tenant A
    When they create a type whose fields cover Text, List, Number, Free text, Single choice, Multiple choice, Image
    Then the type is created and every field records its declared type

  Scenario: Reject an unknown field type
    Given an authenticated member of tenant A
    When they declare a field of type "Colour"
    Then the request is rejected with error code annotation.field.type.unknown (@ValidFieldType)

  Scenario: Reject a field with no name
    Given an authenticated member of tenant A
    When they declare a field with an empty name
    Then the request is rejected with error code annotation.field.name.required (Bean Validation)

  Scenario Outline: Visible-for-viewing default depends on the field type
    Given an authenticated member of tenant A
    When they declare a "<fieldType>" field without specifying its "visible for viewing" flag
    Then the stored field has visible = <default>

    Examples:
      | fieldType       | default |
      | Text            | true    |
      | List            | true    |
      | Number          | true    |
      | Free text       | false   |
      | Single choice   | false   |
      | Multiple choice | false   |
      | Image           | false   |

  Scenario: Number field carries optional bounds
    Given an authenticated member of tenant A
    When they declare a Number field "Port" with min 1 and max 65535
    Then the bounds are persisted on the field

  Scenario: Reject a Number field whose min exceeds its max
    Given an authenticated member of tenant A
    When they declare a Number field with min 10 and max 5
    Then the request is rejected with error code annotation.field.number.bounds.invalid (@NumberBoundsValid)

Feature: Secret flag on text fields (human decision 2026-07-23; value encryption deferred to US-2.1)
  Scenario: Mark a Text field as Secret
    Given an authenticated member of tenant A
    When they declare a Text field "API key" with secret = true
    Then the stored field has secret = true

  Scenario: A Free text field may also be Secret
    Given an authenticated member of tenant A
    When they declare a Free text field "Notes" with secret = true
    Then the stored field has secret = true

  Scenario: Secret defaults to false when omitted
    Given an authenticated member of tenant A
    When they declare a Text field without specifying secret
    Then the stored field has secret = false

  Scenario: Reject Secret on a non-text field
    Given an authenticated member of tenant A
    When they declare a Number field with secret = true
    Then the request is rejected with error code annotation.field.secret.not_allowed (@SecretAllowedForFieldType)

Feature: FR-03 Options on choice fields
  Scenario: Define ordered List options with badge colours
    Given an authenticated member of tenant A
    When they declare a List field "Status" with options [("open", green), ("blocked", red), ("done", blue)] in that order
    Then the options are persisted in the declared order with their colours

  Scenario: Single choice options need no colour
    Given an authenticated member of tenant A
    When they declare a Single choice field "Environment" with options ["dev", "staging", "prod"]
    Then the options are persisted in order

  Scenario: Reject a badge colour outside the palette
    Given an authenticated member of tenant A
    When they declare a List field with an option coloured "purple"
    Then the request is rejected with error code annotation.field.option.colour.invalid (@BadgeColourAllowed)

  Scenario: Reject options on a non-choice field
    Given an authenticated member of tenant A
    When they declare a Text field carrying selectable options
    Then the request is rejected with error code annotation.field.options.not_allowed (@OptionsAllowedForFieldType)

Feature: FR-07 Type and field icon images
  Scenario: Upload and retrieve a type icon
    Given an authenticated member of tenant A
    When they set a 200 KB PNG as the icon of type "RabbitMQ"
    Then the icon is stored as a binary with its content type
    And it is retrievable from the dedicated binary endpoint with content type image/png
    And the response carries the size metadata the client needs to render a thumbnail
    # Thumbnail downscaling is a notebox-web rendering concern (AD-04); the API serves the original + metadata.

  Scenario: Reject an oversize icon
    Given an authenticated member of tenant A
    When they upload a 6 MB PNG as a type icon
    Then the request is rejected with ImageTooLargeException (code annotation.image.too_large)

  Scenario: Reject a disallowed icon format
    Given an authenticated member of tenant A
    When they upload an SVG file as a type icon
    Then the request is rejected with UnsupportedImageTypeException (code annotation.image.type.unsupported)

Feature: NFR-01 / C-01 Tenant isolation
  Scenario: A foreign tenant cannot read another tenant's type
    Given tenant A owns type "RabbitMQ"
    When a member of tenant B requests that type by id
    Then the response is not found and no type data is disclosed

  Scenario: A foreign tenant cannot modify or delete another tenant's type
    Given tenant A owns type "RabbitMQ"
    When a member of tenant B attempts to update or delete it
    Then the request is rejected with no change to tenant A's type

Feature: C-02 Authenticated by default
  Scenario: Reject an unauthenticated request
    Given no authentication token
    When a client calls any annotation-type endpoint
    Then the request is rejected with 401 Unauthorized
```

## Compliance pre-flight
Checklist copied from `constitution/02-compliance.md`; each item marked.

- **C-01 · Tenant isolation** — **applies.** Every type endpoint reads/writes tenant-owned data.
  *Evidence:* cross-tenant read/update/delete tests (scenarios under NFR-01) + tenant-scoped repository
  choke point (AD-03).
- **C-02 · Authenticated by default** — **applies.** New endpoints. *Evidence:* 401-on-unauthenticated
  test; foreign-tenant 404/403 tests.
- **C-03 · Least-privilege authorization** — **not applicable.** US-1.1 grants type definition to any
  *tenant member* (PRD §2 persona, G-01); it is not an admin/config surface like the i18n catalog (C-03's
  named trigger). Revisit if a role gate is later introduced.
- **C-04 · Personal data minimization** — **applies (minimal).** Type payloads carry no user PII; the only
  identity data stored is the tenant scope (already enforced by C-01) and a `created_by`/`deleted_by`
  actor reference for the audit trail. *Evidence:* response-schema review confirms no PII in type payloads.
- **C-05 · Secrets never committed** — **not applicable.** No new credential or connection string; DB
  access reuses feat-001's env-sourced configuration.
- **C-06 · Encryption in transit** — **applies.** Deployed endpoints. *Evidence:* inherited TLS ingress
  configuration (unchanged by this feature).
- **C-07 · Image upload safety** — **applies.** Type/field icons are image binaries. *Evidence:*
  content-type validation (PNG/JPEG/GIF/WebP) + 5 MB size bound tests; binary endpoint serves a correct
  image content type, never inline HTML.
- **C-08 · Rich-text sanitization** — **not applicable.** No rich-text/WYSIWYG content in type definitions
  (that is task details, FR-14/US-4.2).
- **C-09 · Localization completeness** — **applies.** Feature emits validation/error messages. *Evidence:*
  message-catalog coverage check for every emitted key in en + pt.
- **C-10 · Audit trail for irreversible & admin actions** — **applies.** Type delete is irreversible
  (BR-05). *Evidence:* audit-log entry (who/what/when) + test on delete.
- **C-11 · Data retention & deletion path** — **not applicable.** This feature manages neither accounts nor
  tenants; type deletion is covered by BR-05 + C-10, not by an account-data retention path.

## Open Questions
- **OQ-14** *(tactical, deferred to US-2.1)*: When a member deletes an annotation **type that owns
  annotation records**, is the delete **blocked** while records exist, or does it **cascade** and delete
  the records too? No records can exist until FR-04/US-2.1, so this spec deletes only empty types; the
  policy must be settled before US-2.1 implements record deletion. See `catalogs/open-questions.md`.
- **OQ-15** *(important, deferred to US-2.1)*: Secret text fields — the encryption-at-rest **mechanism**
  (algorithm) and **key management** (single app key vs per-tenant; key storage; rotation) are undecided,
  and the capability needs a **PRD v2 + constitution AD/compliance/BR** before US-2.1 implements value
  encryption and the role-gated audited reveal. **Does not block feat-003**, which stores only the flag.
  Decided so far: applies to Text + Free text; reveal = elevated role + audit (C-03, C-10).

_The three standing OQs (OQ-11 provisioning, OQ-12 password-reset, OQ-13 remember-me) are auth-scoped and
do not affect this feature._
