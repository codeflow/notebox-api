# Feature — Annotation type builder UI (ADF Fusion theme)

**ID:** features/004-annotation-types-web
**User Story:** US-1.1
**Version:** v1
**Status:** Draft
**Date:** 2026-07-23
**Project:** notebox-web (satellite; react/next)

## Origin
- **User Story:** US-1.1 — *As a tenant member, I want to define an annotation type with an icon and typed
  fields (incl. list options), so I capture a category of notes consistently.* This feature is the **web
  half** of US-1.1: the client type-builder experience. The server half is `feat-003-annotation-types`,
  whose REST contract this feature consumes.
- **FRs covered (client obligations only):** FR-01 (CRUD annotation types through the UI), FR-02 (declare
  typed fields: the seven field types, "visible for viewing" defaults, Number bounds, Secret flag), FR-03
  (options on List/Single choice/Multiple choice fields, List badge colours), FR-07 (upload & render type
  and field **icons**; client-side pre-validation and thumbnail rendering).
- **BRs bound:** BR-03 (the type *is* the schema of its annotations — the builder edits that schema),
  BR-04 (field types are a closed set of seven — the builder offers exactly those), BR-05 (type deletion
  is explicit and irreversible — the confirmation is the UI's job), BR-08 (no user-facing text ever shown
  untranslated — all builder strings and surfaced server errors localized en/pt).
- **Primary source:** PRD v1 §3.1 (FR-01, FR-02, FR-03, FR-07), §3 Validation baseline (OQ-09); the API
  contract this UI consumes — `features/003-annotation-types/contracts/rest-api.md` (CRUD
  `/api/annotation-types`, `/api/images` upload + binary GET, the `AnnotationTypeInput`/`AnnotationTypeDto`/
  `ImageRefDto` payloads, the field-type and badge-colour enums, the `validation.failed` envelope with
  per-field `violations[]`, and the D2 per-field-type `visibleForViewing` defaults); the feat-002 web
  infrastructure (`features/002-identity-tenancy-web`); the **ADF Fusion** design constraint
  (`features/002-identity-tenancy-web/DESIGN-CONSTRAINT.md`, human decision 2026-07-22).
- **Inherited human decision (2026-07-23):** the **"Secret" flag** on Text/Free text field definitions
  (feat-003). The builder exposes the checkbox to *declare* the flag only; masked display and role-gated
  reveal of Secret **values** are out of scope (US-2.2/US-2.1, gated on OQ-15).

## Summary
Deliver the annotation type builder in notebox-web: the screens through which a tenant member creates,
lists, views, edits and deletes annotation **types** and their ordered field definitions. The builder lets
the member name a type, attach a type icon, and assemble an ordered list of typed fields — choosing one of
the seven field types per field, setting per-field options (visible-for-viewing, Number bounds, the Secret
flag where allowed, choice options with List badge colours, and a field icon) — then persists it by driving
the feat-003 REST API. It surfaces the API's validation and business errors against the right form controls,
localizes every string in en/pt, renders in the ADF Fusion theme, and lives behind the existing route
guard. This feature owns only the **client** experience; all validation rules, persistence, tenant
isolation and auditing are the API's (feat-003), consumed here, never re-specified.

## Scope
- **In:**
  - **Builds on feat-002 infrastructure (consumed, not re-specified):** the type-builder screens sit behind
    the existing **route guard** (`components/RouteGuard`, C-02); every API call goes through the existing
    **API client** (`lib/api` — `apiClient`/`authFetch` attaches the bearer token automatically, `ApiError`
    surfaces the `Problem`/`validation.failed` envelope); tenant context comes from the live **session**
    (`lib/auth`) and is never user-supplied (C-01); all strings use the existing **i18n** catalogs
    (`lib/i18n`, `messages/en.ts` + `messages/pt.ts`); the UI is themed with the mandated **ADF Fusion**
    stylesheet (`src/styles/adf-fusion.css`).
  - **Type CRUD (FR-01):** create a type (name + optional icon + an ordered list of fields); list the
    tenant's types; open a type to view its full schema; edit a type (rename; add / remove / **reorder**
    fields and options — persisted as a full `PUT` replace of the definition); delete a type behind an
    explicit, irreversible confirmation step (BR-05).
  - **Typed fields (FR-02):** the builder offers exactly the **seven** field types (Text, List, Number,
    Free text, Single choice, Multiple choice, Image); each field has a name and a field type; the
    **visible-for-viewing** control reflects the per-field-type default (D2) when the member has not set it;
    Number fields expose optional min/max inputs; the **Secret** checkbox is offered **only** on Text and
    Free text fields (declaration only); an optional **field icon** may be attached per field.
  - **Options on choice fields (FR-03):** for List / Single choice / Multiple choice fields the builder lets
    the member define an **ordered** set of options (each with a label); **List** options additionally allow
    a **badge colour** chosen from the fixed palette {red, green, blue, black, gray, yellow}; the options
    editor is not offered for non-choice fields.
  - **Icons (FR-07):** upload a type icon and per-field icons — the client sends the raw bytes to the image
    endpoint and references the returned image id on the type/field; **client-side pre-validation** of
    content type (PNG/JPEG/GIF/WebP) and size (≤ 5 MB) before upload, rejecting locally with a localized
    message; graceful display of the API's `annotation.image.too_large` / `annotation.image.type.unsupported`
    if the server rejects; the stored icon is rendered as a **thumbnail** (client-side downscale rendering,
    AD-04).
  - **Error surfacing:** the API's field-level `violations[]` are surfaced against the corresponding form
    controls; business errors (duplicate name → `annotation.type.name.taken`; stale id →
    `annotation.type.not_found`) are surfaced as localized messages on the form. The client re-runs no
    server rule — it displays the server's outcome.
  - **Localization (BR-08, C-09):** every builder label, placeholder, control and confirmation string
    resolves in en + pt; server-returned error messages (already localized by the API) are shown verbatim;
    no raw message key or blank ever reaches the screen.
  - **Accessibility & ADF Fusion theme:** the builder meets the satellite's accessibility standard and is
    presented in the ADF Fusion theme (presentation constraint only).
- **Out:** see **Out of scope** below.

## Acceptance criteria (Gherkin)
> The endpoints (`/api/annotation-types` CRUD, `/api/images` upload + binary GET), the
> `AnnotationTypeInput`/`AnnotationTypeDto`/`ImageRefDto` shapes, the `fieldType` and `badgeColour` enums,
> the `validation.failed` envelope with per-field `violations[]`, and the D2 `visibleForViewing` defaults
> named below are the **consumed** contract of `feat-003` (`features/003-annotation-types/contracts/rest-api.md`),
> not new design decisions of this feature. These scenarios become the executable UI/integration tests in
> `implement` (vitest + @testing-library/react + msw), driving a mocked API.

```gherkin
Feature: Create, list and view annotation types from the web (FR-01)

  Scenario: Create a type with an ordered list of fields
    Given a signed-in member of tenant A on the type builder
    When they name the type "RabbitMQ" and add fields [URL (Text), Environment (Single choice), Description (Free text)] in that order
    And they save the type
    Then the client sends one create request whose fields are ordered URL, Environment, Description
    And on success the new type appears in the tenant's type list
    And opening it shows its fields in the order URL, Environment, Description

  Scenario: The type list shows the tenant's types
    Given tenant A has types "RabbitMQ" and "Postgres"
    When the member opens the type list
    Then both "RabbitMQ" and "Postgres" are listed
    And each row is sourced from the type-list response, not from user input

  Scenario: View a type's full schema
    Given tenant A has a type "RabbitMQ" with three fields
    When the member opens that type
    Then every field is shown with its name, field type, and its per-field settings
    And the fields are shown in their stored order

  Scenario: Edit reorders fields and saves a full replacement
    Given the member is editing type "RabbitMQ" with fields [URL, Environment, Description]
    When they reorder the fields to [Description, URL, Environment] and save
    Then the client sends one replace request carrying the whole definition in the new order
    And on success reopening the type shows fields in the order Description, URL, Environment

  Scenario: Add and remove fields while editing
    Given the member is editing type "RabbitMQ" with fields [URL, Environment, Description]
    When they remove "Environment", add a Number field "Port", and save
    Then the client sends one replace request whose fields are [URL, Description, Port]
    And on success reopening the type shows exactly those three fields

  Scenario: Delete a type requires an explicit irreversible confirmation
    Given the member is viewing type "RabbitMQ"
    When they choose to delete it
    Then a confirmation that names the type and states the action is irreversible is shown
    And no delete request is sent until the member confirms
    When the member confirms
    Then the client sends the delete request
    And on success the type no longer appears in the type list

  Scenario: Cancelling the delete confirmation sends no request
    Given the member has opened the delete confirmation for type "RabbitMQ"
    When they cancel the confirmation
    Then no delete request is sent
    And the type still appears in the type list

  Scenario: A duplicate type name is surfaced on the form (error path)
    Given tenant A already has a type named "RabbitMQ"
    And the member is creating another type named "RabbitMQ"
    When they save and the API rejects it with annotation.type.name.taken
    Then the member stays on the builder with their input intact
    And the localized annotation.type.name.taken message is shown against the name field
    And no new type is added to the list

  Scenario: Editing a type that was deleted elsewhere is surfaced (error path)
    Given the member is editing a type whose id the API no longer resolves
    When they save and the API rejects it with annotation.type.not_found
    Then the localized annotation.type.not_found message is shown
    And the member is not left believing the change was saved
```

```gherkin
Feature: Declare typed fields in the builder (FR-02)

  Scenario: The builder offers exactly the seven field types
    Given a signed-in member adding a field
    When they open the field-type chooser
    Then the only choices are Text, List, Number, Free text, Single choice, and Multiple choice, and Image
    And no other field type can be chosen

  Scenario Outline: Visible-for-viewing shows the per-field-type default until the member changes it
    Given a signed-in member adding a "<fieldType>" field
    When they have not set the "visible for viewing" control
    Then the control shows <default>
    And if left unchanged the saved field carries visibleForViewing = <default>

    Examples:
      | fieldType       | default |
      | Text            | true    |
      | List            | true    |
      | Number          | true    |
      | Free text       | false   |
      | Single choice   | false   |
      | Multiple choice | false   |
      | Image           | false   |

  Scenario: Number field exposes min and max inputs
    Given a signed-in member adding a Number field "Port"
    When the field type is Number
    Then min and max inputs are available on that field
    And entering min 1 and max 65535 sends those bounds on save

  Scenario: A server-rejected Number bound is surfaced on the field (error path)
    Given the member has a Number field with min 10 and max 5
    When they save and the API returns a violation annotation.field.number.bounds.invalid for that field
    Then the localized message is shown against that Number field
    And the type is not saved

  Scenario: The Secret checkbox is offered only on Text and Free text fields
    Given a signed-in member adding a field
    When the field type is Text or Free text
    Then a Secret checkbox is available on that field
    When the field type is Number, List, Single choice, Multiple choice, or Image
    Then no Secret checkbox is shown on that field

  Scenario: A field-level validation violation is surfaced against the right field (error path)
    Given the member is saving a type whose second field has an empty name
    When the API responds with a validation.failed envelope whose violation targets fields[1].name with code annotation.field.name.required
    Then the localized message is shown against that field's name control, not against the type name
    And the type is not saved
```

```gherkin
Feature: Options on choice fields in the builder (FR-03)

  Scenario: Define ordered List options with badge colours
    Given a signed-in member adding a List field "Status"
    When they add options [("open", green), ("blocked", red), ("done", blue)] in that order
    And they save
    Then the client sends those options in the declared order each with its palette colour
    And reopening the field shows the options in that order with those colours

  Scenario: Single and Multiple choice options carry no badge colour control
    Given a signed-in member adding a Single choice field "Environment"
    When they add options ["dev", "staging", "prod"]
    Then no badge-colour control is shown on those options
    And on save the options are sent in order with no colour

  Scenario: The badge-colour chooser offers only the fixed palette
    Given a signed-in member adding a badge colour to a List option
    When they open the colour chooser
    Then the only choices are red, green, blue, black, gray, and yellow
    And no other colour can be chosen

  Scenario: Options editor is not offered for non-choice fields
    Given a signed-in member adding a Text field
    When the field type is Text
    Then no options editor is shown for that field

  Scenario: A rejected option is surfaced against that option (error path)
    Given the member is saving a List field whose option has an empty label
    When the API returns a violation annotation.field.option.label.required for that option
    Then the localized message is shown against that option row
    And the type is not saved
```

```gherkin
Feature: Type and field icons in the builder (FR-07)

  Scenario: Upload a type icon and render it as a thumbnail
    Given a signed-in member editing type "RabbitMQ"
    When they choose a 200 KB PNG as the type icon
    Then the client uploads the bytes to the image endpoint and receives an image id
    And that image id is referenced on the type when the type is saved
    And the icon is rendered as a downscaled thumbnail in the builder

  Scenario: Upload a per-field icon
    Given a signed-in member adding a field
    When they choose a valid PNG as the field icon
    Then the client uploads it, receives an image id, and references that id on the field on save

  Scenario: Oversize icon is rejected client-side before any upload (error path)
    Given a signed-in member choosing a type icon
    When they pick a 6 MB PNG
    Then no upload request is sent
    And a localized "image too large" message is shown

  Scenario: Unsupported icon format is rejected client-side before any upload (error path)
    Given a signed-in member choosing a type icon
    When they pick an SVG file
    Then no upload request is sent
    And a localized "unsupported image type" message is shown

  Scenario: A server image rejection is surfaced gracefully (error path)
    Given a file that passes client-side pre-validation
    When the upload is rejected by the API with annotation.image.too_large or annotation.image.type.unsupported
    Then the localized server message is shown
    And the type is not saved with a broken icon reference
```

```gherkin
Feature: The builder requires a live session and never supplies a tenant (C-01, C-02)

  Scenario: Visiting the builder without a session redirects to login
    Given no active session
    When the member navigates directly to the type builder
    Then the route guard redirects them to the login screen
    And no annotation-type request is sent

  Scenario: Every builder request carries the bearer token and no client tenant identifier
    Given a signed-in member of tenant A
    When the builder reads or writes annotation types or images
    Then each request carries the member's token as its bearer credential
    And carries no client-supplied tenant identifier
    And the types shown are only tenant A's, as returned by the API
```

```gherkin
Feature: The builder is localized (BR-08, C-09)

  Scenario: The builder renders in the resolved locale
    Given the resolved locale is pt
    When the type builder renders
    Then all its labels, field-type names, controls, and the delete confirmation are shown in pt
    And no raw message key or blank label is shown

  Scenario: A localized server error is shown as received
    Given the resolved locale is pt
    When a save fails with annotation.type.name.taken
    Then the message shown is the pt message from the API error envelope
    And no raw message key is shown
```

## Compliance pre-flight
Checklist copied from `constitution/02-compliance.md`, marked for a **web client** (the type builder)
consuming the feat-003 API. Deltas from the feat-002 exemplar are called out inline.

- **C-01 · Tenant isolation** — **applies (client obligation).** The builder never offers a tenant selector
  and never sends a client-supplied tenant identifier; the type list and every read/write are scoped by the
  API from the bearer token. *Evidence:* the "carries no client-supplied tenant identifier" and
  "types shown are only tenant A's" scenarios. *(Same as feat-002.)*
- **C-02 · Authenticated by default** — **applies.** Every builder screen sits behind the existing route
  guard; an unauthenticated visit redirects to login and sends no request. *Evidence:* the "visiting the
  builder without a session redirects to login" scenario. *(Same as feat-002.)*
- **C-03 · Least-privilege authorization** — **not applicable (this feature).** US-1.1 grants type
  definition to any *tenant member* (matching feat-003 C-03); the builder is not an admin/config surface and
  gates nothing on an elevated role. *(Same as feat-002.)*
- **C-04 · Personal data minimization** — **applies.** The builder stores no user PII; type/field payloads
  carry only schema data plus opaque image ids, and neither the token nor any identity is written to logs.
  *Evidence:* no-PII review of the builder's requests/state. *(Same as feat-002.)*
- **C-05 · Secrets never committed** — **applies (partial).** The builder holds no signing key; the API base
  URL/config comes from environment (reuses feat-002's `.env.example` entry), none in the repo. *Evidence:*
  `.env.example` reuse + repo secret scan. *(Same as feat-002.)*
- **C-06 · Encryption in transit** — **applies.** All builder traffic — including raw image-byte uploads —
  travels only over TLS/HTTPS in deployed environments. *Evidence:* deployment/ingress config (HTTPS-only,
  inherited). *(Same as feat-002.)*
- **C-07 · Image upload safety** — **applies. ← FLIPPED vs feat-002 (was not applicable there).** The
  builder uploads type and field **icons**, so the client obligation applies: **content-type
  (PNG/JPEG/GIF/WebP) and size (≤ 5 MB) pre-validation before upload**, with a localized local rejection,
  and graceful surfacing of the API's `annotation.image.too_large` / `annotation.image.type.unsupported`.
  The **API remains authoritative** — the client pre-check is a UX guard, not the security control (that is
  feat-003 C-07). Icons are rendered as thumbnails from the served binary (AD-04). *Evidence:* the FR-07
  scenarios (client-side reject-before-upload for oversize and unsupported type; graceful server-rejection
  surfacing).
- **C-08 · Rich-text sanitization** — **not applicable.** The builder edits a type *schema*; it renders no
  rich-text/HTML content. The "Free text" field is a plain field declaration, not a WYSIWYG surface
  (rich text is Task details, FR-14/US-4.2). *(Same as feat-002.)*
- **C-09 · Localization completeness** — **applies.** All builder strings (labels, field-type names,
  options, the delete confirmation, client-side image rejections) resolve in en + pt, and server error
  messages are shown verbatim; no raw key/blank ever reaches the screen. *Evidence:* the localization
  scenarios + a per-locale coverage check of the builder's client strings. *(Same as feat-002.)*
- **C-10 · Audit trail for irreversible & admin actions** — **not applicable (this client).** The type
  delete is irreversible, but the **audit entry is written server-side by feat-003** (C-10 there); the
  client's obligation is the explicit irreversible **confirmation** (BR-05), which is covered by the FR-01
  delete scenarios, not an audit log. *(Same reasoning shape as feat-002: irreversible action audited by
  the API, not the client.)*
- **C-11 · Data retention & deletion path** — **not applicable (this feature).** The builder manages no
  accounts or tenants; deleting a type is a tenant-owned schema deletion handled by the API (BR-05 + C-10
  server-side), not an account-data retention path. *(Same as feat-002.)*

> Accessibility of the builder is a `notebox-web` satellite standard (per `02-compliance.md`), handled by
> the satellite's own standards; it is not a numbered compliance item and is not gated here.

## Out of scope
Stated explicitly to stop scope drift during `implement`:
- Annotation **records** and their values, including entering/editing field values and uploading
  **Image-field** values — FR-04, US-2.1. This feature edits only the type *schema*.
- The **listing / detail grids** that consume the "visible for viewing" flag — FR-05, US-1.2 / US-2.2. The
  builder only *sets* the flag; it renders no annotation grid.
- **Masked display + role-gated reveal of Secret VALUES** — the builder only *declares* the Secret flag on
  Text/Free text fields; masking values and the reveal affordance are US-2.2 (UI) / US-2.1 (API), gated on
  OQ-15.
- **Rich-text / WYSIWYG editing** — FR-14 / US-4.2 (Task details); "Free text" here is a plain field
  declaration, not an editor.
- **Groups and the navigation tree** — FR-08 / FR-09, US-3.1.
- **Any change to the API's behaviour** — all validation rules (name uniqueness, closed field-type set,
  options/Secret/bounds constraints, image type/size), tenant isolation, and the delete audit are owned by
  **feat-003** and merely *surfaced/consumed* here; this spec re-specifies none of them.
- Server-side image processing / thumbnail generation — the API serves the original binary + metadata;
  downscaled rendering is the client's concern (AD-04).

## Open Questions
No new Open Question is opened by this spec. The two known OQs are **inherited from feat-003 and
non-blocking** for this builder:
- **OQ-14** *(deferred to US-2.1)* — deleting a type that owns annotation records (block vs cascade). No
  records exist until US-2.1, so this builder deletes only empty types; the policy does not block the UI.
- **OQ-15** *(deferred to US-2.1)* — Secret-value encryption mechanism & key management, plus masked
  display / role-gated reveal of Secret values. This builder only *declares* the Secret flag, so OQ-15 does
  not block it.

_The auth-scoped standing OQs (OQ-11 provisioning, OQ-12 password-reset, OQ-13 remember-me) do not affect
this feature. No row needs to be added to `catalogs/open-questions.md` — no new OQ is raised._
