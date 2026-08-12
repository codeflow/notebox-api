# Feature — Annotation record editor + secret masking & audited reveal (ADF Fusion theme)

**ID:** features/006-annotation-records-notebox-web
**User Story:** US-2.1
**Version:** v2
**Status:** Draft
**Date:** 2026-08-11
**Changed in v2:** OQ-19 decided (2026-08-11) — Free-text annotation **values are rich text**. C-08
sanitization flips to *applies*, the Free-text control becomes a WYSIWYG editor, and a new Feature block
covers rich-text behaviour. See **Dependency** under Open Questions.
**Project:** notebox-web (satellite; react/next)

## Origin
- **User Story:** US-2.1 — *As a tenant member, I want to create/edit/delete annotations of a type, so I
  record real data.* This feature is the **web half** of US-2.1: the client experience for a type's
  records. The server half is `feat-005-annotation-records-notebox-api`, merged to `develop` 2026-08-11,
  whose REST contract this feature consumes.
- **FRs covered (client obligations only):** FR-04 (create, read, update and delete annotation **records**
  that conform to their type, through the UI), FR-06 (delete is an explicit operation — *the confirmation
  is a UI concern*, PRD §3.1), FR-18 (secret values shown **masked**, cleartext only through the
  role-gated, audited **reveal** action).
- **BRs bound:** BR-03 (a record conforms to its type — the editor offers one control per defined field and
  nothing else), BR-05 (record deletion is explicit and irreversible — the confirmation is the UI's job),
  BR-10 (secret values stay confidential — the client never displays or caches cleartext outside an
  explicit reveal), BR-08 (no user-facing text ever shown untranslated — all strings and surfaced server
  errors localized en/pt).
- **Primary source:** PRD v2 §3.1 (FR-04, FR-06, FR-18 — FR-18 is bound to US-2.1 by PRD v2, which is why
  masked display and reveal are *this* feature's client obligation and not US-2.2's); the API contract this
  UI consumes — `features/005-annotation-records-notebox-api/contracts/rest-api.md` (the five
  `/api/annotation-records` endpoints, the `AnnotationRecordInput` / `AnnotationRecordDto` /
  `RevealResponse` payloads, the **PUT secret semantics** decided 2026-07-25, and the error/code table);
  the feat-002 web infrastructure (`features/002-identity-tenancy-web`) and the feat-004 builder patterns
  (`features/004-annotation-types-web`); the **ADF Fusion** design constraint
  (`features/002-identity-tenancy-web/DESIGN-CONSTRAINT.md`, human decision 2026-07-22) and the satellite's
  design handoff (`notebox-web/design/handoff/`, screens **12** annotation detail and **13** annotation
  create/edit).
- **Deciding human decision (2026-08-11, OQ-19):** a Free-text field's **value is rich text** (WYSIWYG
  HTML — styles, colour, bold, underline, lists, quote, link, inline code, syntax-highlighted code block,
  embedded images), matching design handoff screen 13, and is **sanitized on input/output to an
  allow-list** as task details are (C-08, PRD v2 §3.1). This spec is v2 because of it.
- **Inherited human decisions (consumed, not re-decided):** the **PUT secret semantics** (2026-07-25) —
  omitted secret field preserves the ciphertext, a masked echo (`text: null`) is a no-op preserve, and
  `clearSecret: true` erases under audit; **OQ-17/OQ-18** (2026-08-03) — the type-edit guards, whose 409s
  this UI only surfaces.

## Summary
Deliver the annotation **record** experience in notebox-web: the screens through which a tenant member
creates a record of a type, views its values, edits it, and deletes it behind an explicit irreversible
confirmation. The editor derives one control per field from the type's definition — text, number with the
field's bounds, choice controls offering exactly the type's options, a **rich-text editor** for Free-text
fields, and image upload — so a record can only ever be shaped like its type. Secret values are shown **masked**; an elevated member can
**reveal** one, which the API audits, and the cleartext re-masks on navigation. Every rule — conformance,
bounds, tenant isolation, encryption, the reveal role gate and the audit trail — belongs to feat-005 and is
merely surfaced here, never re-implemented. This feature owns only the **client** experience.

## Scope
- **In:**
  - **Builds on existing infrastructure (consumed, not re-specified):** the record screens sit behind the
    existing **route guard** (`components/RouteGuard`, C-02); every call goes through the existing **API
    client** (`lib/api` — bearer token attached automatically, `ApiError` surfacing the
    `Problem`/`validation.failed` envelope); tenant and role come from the live **session** (`lib/auth`,
    `Me.role`) and are never user-supplied (C-01); all strings use the existing **i18n** catalogs
    (`lib/i18n`); presentation is the mandated **ADF Fusion** theme.
  - **Record create (FR-04):** from a chosen type, a form rendering **one control per defined field, in the
    type's stored field order**, with the control determined by the field type — Text input, **Free text
    rich editor** (see the rich-text block below), Number input honouring the field's min/max, Single choice and List offering exactly the type's
    options (List options rendered as their badge colours), Multiple choice allowing several, Image upload
    producing an image reference. A record has a required **name** (≤ 120 chars).
  - **Rich-text Free-text values (FR-04, C-08, OQ-19):** the Free-text control is a WYSIWYG editor
    offering bold, italic, underline, strikethrough, colour, ordered/unordered lists, quote, link, inline
    code, a **code block with a language selection and syntax highlighting**, and embedded images
    (uploaded through the same image path and pre-validated like any image value). The stored value
    carries markup; the client **sanitizes on render to an allow-list** so no markup from the API can
    execute — defence in depth, *not* the primary control (see Dependency).
  - **Record read (FR-04, FR-18):** a single record's own view showing its values in field order, with
    **secret values masked** and image values rendered as thumbnails from the binary endpoint.
  - **Record edit (FR-04):** the same form pre-filled from the stored record, saved as a full replacement.
    **Secret fields follow the API's PUT semantics:** an untouched masked secret is echoed as a **no-op
    preserve** (the client never invents cleartext it was never given), typing a new value re-encrypts, and
    an explicit **clear** affordance sends `clearSecret` — the erasure being destructive and audited
    server-side.
  - **Reveal (FR-18, BR-10):** a **Reveal** action offered on a masked secret value **only when the
    session's role is the elevated one**; it fetches the cleartext through the dedicated reveal endpoint,
    displays it, and **re-masks on navigation**; the cleartext is never persisted client-side, never
    logged, and never shown anywhere the value is listed.
  - **Record delete (FR-06, BR-05):** an explicit, irreversible confirmation step that names the record and
    states it cannot be undone; only on confirmation is the delete sent.
  - **Error surfacing:** the API's field-level `violations[]` are routed onto the corresponding controls;
    business errors (`annotation.record.value.number.out_of_bounds`, `…value.option.unknown`,
    `…value.image.not_found`, `…name.too_long`, `…value.too_long`, `…not_found`,
    `annotation.record.secret.reveal.forbidden`) are surfaced as localized messages. A record of another
    tenant is indistinguishable from a non-existent one. The client re-runs no server rule.
  - **Localization (BR-08, C-09):** every label, control, confirmation and client-side rejection resolves in
    en + pt; server messages are shown as received; no raw key or blank ever reaches the screen.
- **Out:** see **Out of scope** below.

## Acceptance criteria (Gherkin)
> The endpoints, payload shapes, PUT secret semantics, masking rule and error codes named below are the
> **consumed** contract of `feat-005` (`features/005-annotation-records-notebox-api/contracts/rest-api.md`),
> not new decisions of this feature. These scenarios become the executable UI/integration tests in
> `implement` (vitest + @testing-library/react + msw), driving a mocked API.

```gherkin
Feature: Create a record that conforms to its type (FR-04, BR-03)

  Scenario: The editor offers exactly the type's fields, in order
    Given a signed-in member of tenant A and a type "RabbitMQ" with fields [URL (Text), Port (Number, 1..65535), Environment (Single choice: dev, prod), Notes (Free text)]
    When they open the record editor for "RabbitMQ"
    Then exactly four controls are shown, in the order URL, Port, Environment, Notes
    And each control matches its field type, the Notes control being the rich-text editor
    And no control is offered for any field the type does not define

  Scenario: Create a record with values for the defined fields
    Given the member is on the record editor for "RabbitMQ"
    When they name the record "prod-broker", enter URL "amqp://h", Port 5672, choose Environment "prod", and save
    Then the client sends one create request carrying the type id and one value per filled field
    And on success the saved record is shown with those four values

  Scenario: Single choice offers exactly the field's predefined options
    Given the type's Environment field defines the options dev and prod
    When the member opens the Environment control
    Then exactly dev and prod are offered
    And no free-text entry is accepted for that field

  Scenario: Multiple choice accepts several options
    Given a type whose Tags field is Multiple choice with options a, b, c
    When the member selects a and c and saves
    Then the client sends both selected option ids for that field

  Scenario: A record name is required
    Given the member has filled values but left the name empty
    When they attempt to save
    Then the record is not created
    And a localized "name required" message is shown on the name control

  Scenario: A rejected value is shown against its own control
    Given the type's Port field has bounds 1..65535
    When the member saves Port 70000 and the API rejects it with annotation.record.value.number.out_of_bounds
    Then the localized message is shown against the Port control, not as a page-level error
    And the record is not created
```

```gherkin
Feature: Free-text values are rich text (FR-04, C-08, OQ-19)

  Scenario: The Free-text control offers the formatting actions
    Given the member is editing a record of a type with a Free text field "Notes"
    When they focus the Notes control
    Then bold, italic, underline, strikethrough, colour, ordered and unordered list, quote, link, inline code, code block and embedded image actions are offered
    And each action is labelled in the resolved locale

  Scenario: Formatting survives a save and reopen
    Given the member writes in Notes a bold word, a two-item bullet list, and a code block in language "sql"
    When they save the record and reopen it
    Then the bold word, both list items and the code block are rendered with the same formatting
    And the code block still declares language "sql"

  Scenario: Hostile markup from the API is rendered inert
    Given the API returns a record whose Notes value contains a script element and an element carrying an inline event-handler attribute
    When the record is displayed
    Then no script from that value executes
    And no event-handler attribute from that value survives into the rendered document
    And the value's allowed formatting is still rendered

  Scenario: An image embedded in rich text follows the image rules
    Given the member embeds an image inside the Notes value
    When the file is over 5 MB or is not PNG/JPEG/GIF/WebP
    Then it is rejected locally with a localized message and no upload is sent
    And an accepted file is uploaded and referenced by the rich-text value
```

```gherkin
Feature: Image field values (FR-04, C-07)

  Scenario: An image value is uploaded and referenced
    Given a type with an Image field "Diagram"
    When the member attaches a 200 KB PNG and saves
    Then the client uploads the bytes and sends the returned image reference as that field's value
    And reopening the record renders the image as a thumbnail

  Scenario: An oversize or unsupported file is rejected before upload
    Given a type with an Image field
    When the member attaches a 7 MB file, or a file whose type is not PNG/JPEG/GIF/WebP
    Then the client rejects it locally with a localized message
    And no upload request is sent
```

```gherkin
Feature: Secret values are masked, and revealed only under the elevated role (FR-18, BR-10)

  Scenario: An ordinary read shows the secret masked
    Given a record whose Secret field "API key" holds a value
    When any member opens that record
    Then the "API key" value is shown masked
    And no cleartext for it appears anywhere on the screen or in the client's stored state

  Scenario: Reveal is not offered to a member without the elevated role
    Given the session's role is not the elevated reveal role
    When the member opens a record with a masked secret value
    Then no reveal action is offered for it
    And the value remains masked

  Scenario: An elevated member reveals a secret value
    Given the session's role is the elevated reveal role
    When they trigger reveal on the masked "API key" value
    Then the client calls the dedicated reveal endpoint for that record and field
    And the returned cleartext is displayed in place of the mask

  Scenario: Revealed cleartext re-masks on navigation
    Given a revealed "API key" value is on screen
    When the member navigates away and returns to the record
    Then the value is masked again
    And no second reveal happened without the member asking for one

  Scenario: A refused reveal leaves the value masked
    Given the API answers the reveal with 403 annotation.record.secret.reveal.forbidden
    Then the localized forbidden message is shown
    And the value remains masked
    And no cleartext is shown
```

```gherkin
Feature: Edit a record, preserving secrets the member did not touch (FR-04)

  Scenario: Editing changes the values
    Given a record "prod-broker" with URL "amqp://h"
    When the member changes URL to "amqp://new" and saves
    Then the client sends one replace request for that record
    And reopening it shows URL "amqp://new"

  Scenario: An untouched masked secret is preserved, not destroyed
    Given a record whose Secret field "API key" holds a value and is shown masked in the editor
    When the member changes only the record's name and saves
    Then the value the client sends for "API key" is the masked no-op, never invented cleartext
    And after saving, a reveal by an elevated member still returns the original secret

  Scenario: A new secret value replaces the old one
    Given the member is editing a record with a Secret field
    When they type a new value for it and save
    Then the client sends that new value for the field
    And a subsequent reveal returns the new value

  Scenario: Clearing a secret is explicit
    Given a record whose Secret field holds a value
    When the member uses the explicit clear affordance for that field and saves
    Then the client sends the clear instruction for that field
    And after saving, the field holds no value

  Scenario: A record of another tenant is indistinguishable from a missing one
    Given a record id that belongs to tenant B
    When a member of tenant A opens it
    Then the same localized "not found" outcome is shown as for a non-existent record
    And no data of that record is displayed
```

```gherkin
Feature: Delete a record explicitly and irreversibly (FR-06, BR-05)

  Scenario: Delete is confirmed before it happens
    Given the member is viewing the record "prod-broker"
    When they choose delete
    Then a confirmation names "prod-broker" and states the action cannot be undone
    And no delete request has been sent yet

  Scenario: Confirming deletes the record
    Given the delete confirmation for "prod-broker" is open
    When the member confirms
    Then the client sends the delete for that record
    And on success the member is returned to the type's records surface, where "prod-broker" is gone

  Scenario: Cancelling deletes nothing
    Given the delete confirmation is open
    When the member cancels
    Then no delete request is sent
    And the record is still shown
```

```gherkin
Feature: The record screens require a live session and never supply a tenant (C-01, C-02)

  Scenario: Visiting a record screen without a session redirects to login
    Given no active session
    When the member navigates directly to a record screen
    Then the route guard redirects them to the login screen
    And no annotation-record request is sent

  Scenario: Every record request carries the bearer token and no client tenant identifier
    Given a signed-in member of tenant A
    When the client reads or writes records, or reveals a secret
    Then each request carries the member's token as its bearer credential
    And carries no client-supplied tenant identifier
```

```gherkin
Feature: The record screens are localized (BR-08, C-09)

  Scenario: The screens render in the resolved locale
    Given the resolved locale is pt
    When the record editor, the masked secret and the delete confirmation render
    Then every label, control and confirmation string is shown in pt
    And no raw message key or blank label is shown

  Scenario: A localized server error is shown as received
    Given the resolved locale is pt
    When a save fails with annotation.record.value.option.unknown
    Then the message shown is the pt message from the API error envelope
    And no raw message key is shown
```

## Compliance pre-flight
Checklist copied from `constitution/02-compliance.md`, marked for a **web client** consuming the feat-005
API. Deltas from the feat-004 exemplar are called out inline.

- **C-01 · Tenant isolation** — **applies (client obligation).** No tenant selector, no client-supplied
  tenant identifier; every read/write is scoped by the API from the bearer token, and a foreign-tenant
  record is indistinguishable from a missing one. *Evidence:* the "no client-supplied tenant identifier"
  and "another tenant's record" scenarios. *(Same as feat-004.)*
- **C-02 · Authenticated by default** — **applies.** Every record screen sits behind the existing route
  guard; an unauthenticated visit redirects and sends no request. *Evidence:* the route-guard scenario.
- **C-03 · Least-privilege authorization** — **applies. ← FLIPPED vs feat-004 (was not applicable there).**
  The **reveal** action is restricted to the elevated role: the affordance is offered only when the
  session's role is elevated, and a server 403 leaves the value masked. The API remains authoritative — the
  client gate is UX, not the security control (that is feat-005 C-03). *Evidence:* the two reveal-role
  scenarios.
- **C-04 · Personal data minimization** — **applies.** Record payloads carry only the member's own content
  plus opaque ids; no identity or token is written to logs. **Secret cleartext is never logged, never
  persisted client-side, and never included in any listing.** *Evidence:* no-PII/no-cleartext review of the
  client's requests, state and logs, plus the masking scenarios.
- **C-05 · Secrets never committed** — **applies (partial).** The client holds no key material; the API
  base URL comes from environment (feat-002's `.env.example` entry). *Evidence:* `.env.example` reuse +
  repo secret scan.
- **C-06 · Encryption in transit** — **applies.** All record traffic — including secret values sent as
  plaintext for the server to encrypt, and revealed cleartext coming back — travels only over TLS/HTTPS in
  deployed environments. *Evidence:* deployment/ingress config (HTTPS-only, inherited).
- **C-07 · Image upload safety** — **applies.** Image **field values** are uploaded here: content-type
  (PNG/JPEG/GIF/WebP) and size (≤ 5 MB) pre-validation before upload with a localized local rejection, and
  graceful surfacing of the API's rejection. The API remains authoritative. *Evidence:* the two image
  scenarios. *(Same shape as feat-004, now for values rather than icons.)*
- **C-08 · Rich-text sanitization** — **applies. ← FLIPPED by the OQ-19 decision (was *not applicable* in
  v1, and is *not applicable* in feat-004).** Free-text values now carry WYSIWYG HTML. **This client's
  share:** the editor emits only allow-listed markup, and every rich value received from the API is
  **sanitized on render** so no script or event-handler attribute from stored content can execute.
  *Evidence:* the "hostile markup is rendered inert" scenario, with hostile-markup fixtures.
  **Not this client's share:** C-08 binds the feature that *stores or returns* rich text to sanitize on
  **input/output** — that is the API, and it does not do it today (see Dependency). Client-side
  sanitization is defence in depth, never the primary control: markup can reach the store through the API
  directly, bypassing this client entirely.
- **C-09 · Localization completeness** — **applies.** All client strings (labels, controls, the delete
  confirmation, client-side image rejections, the mask/reveal affordances) resolve in en + pt; server
  messages are shown as received. *Evidence:* the localization scenarios + a per-locale coverage check.
- **C-10 · Audit trail for irreversible & admin actions** — **not applicable (this client).** The record
  delete, the secret erasure and every reveal are irreversible or privileged, but the **audit entries are
  written server-side by feat-005** (C-10 there). The client's obligations are the explicit confirmation
  (BR-05) and the explicit clear affordance, both covered by scenarios above. *(Same reasoning shape as
  feat-004.)*
- **C-11 · Data retention & deletion path** — **not applicable (this feature).** Deleting a record is
  tenant-owned content deletion handled by the API, not an account-data retention path.
- **C-12 · Encryption at rest for secret values** — **applies (client obligation only).** Encryption, key
  management and the reveal audit are entirely feat-005's (evidence there: ciphertext-at-rest test + reveal
  authorization/audit test). This client's share is to **never defeat it**: display masked by default,
  request cleartext only through the dedicated reveal action, never cache or log it, re-mask on navigation,
  and never fabricate cleartext on a PUT. *Evidence:* the masking, reveal, re-mask and
  "untouched masked secret is preserved" scenarios.

> Accessibility of the record screens is a `notebox-web` satellite standard (per `02-compliance.md`),
> handled by the satellite's own standards; it is not a numbered compliance item and is not gated here.

## Out of scope
Stated explicitly to stop scope drift during `implement`:
- **The records grid / listing of a type's records with the "visible for viewing" column projection** —
  FR-05, US-2.2 (design screen 11). The API's listing endpoint is itself out of scope in feat-005's
  contract. This feature reaches a record through the type's existing surface and delivers only the
  **single-record** screens.
- **The full "detail view exposing all fields" projection semantics** (visible vs non-visible field
  rules) — FR-05, US-2.2. This feature shows a record's values because FR-04/FR-06/FR-18 need a surface to
  edit, delete and reveal on; it does not implement the visible/detail distinction.
- **Task details rich text (FR-14, US-4.2)** — a different surface with its own feature; OQ-19 governs
  annotation *values* only.
- **Server-side sanitization of stored rich text** — an API obligation under C-08, not this client's to
  implement (see Dependency).
- **Any change to the API's behaviour** — conformance validation, bounds, option integrity, tenant
  isolation, encryption, the reveal role gate and every audit entry are owned by **feat-005** and merely
  surfaced here.
- **Type definition editing** — feat-004 owns the builder. This feature only *reads* a type to derive its
  controls; it never writes one, and it does not surface the type-edit 409 guards (OQ-17/OQ-18), which
  belong to the builder's own screens.
- **Groups / navigation placement of records** — FR-08/FR-09, US-3.1.
- **Server-side image processing / thumbnail generation** — the API serves the original binary; downscaled
  rendering is the client's concern (AD-04).

## Open Questions
No Open Question is pending for this spec.

- **OQ-19 — resolved (2026-08-11):** Free-text annotation **values are rich text**, sanitized on
  input/output to an allow-list (PRD v2 §3.1). This spec is v2 accordingly.

### Dependency — the API side of C-08 is not implemented
The OQ-19 decision extends C-08's reach from FR-14 task details to annotation values, and C-08 binds the
feature that **stores or returns** the markup. That is **feat-005**, which merged to `develop` on
2026-08-11 storing and returning Free-text values verbatim, with C-08 marked *not applicable* — correctly,
under the plain-text assumption that held at the time. It now holds a real sanitization obligation that its
shipped code does not meet: hostile markup POSTed straight at the API is stored as-is and returned to every
client.

This feature **cannot close that gap** — client-side sanitization is bypassed by any direct API call. The
scoping is the human's call, not this spec's; it does not block feat-006's own plan, since the client
obligations above are the same either way. The options, for the record:
1. A follow-up API feature carrying input/output allow-list sanitization with hostile-markup fixtures.
2. `wf reopen feat-005-annotation-records-notebox-api.spec --cascade` — reopens a merged feature to add it
   in place.
3. Accept the gap explicitly as a recorded risk until the rich editor actually ships.

_The inherited OQs are settled and non-blocking here: **OQ-14** (type delete blocked while records exist),
**OQ-15** (secret encryption — delivered by feat-005), **OQ-17/OQ-18** (type-edit guards) were all decided
by the human and are surfaced, not re-opened, by this client._
