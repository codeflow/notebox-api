# Plan — Annotation type builder UI (ADF Fusion theme)

**ID:** features/004-annotation-types-web
**User Story:** US-1.1 · **FRs:** FR-01, FR-02, FR-03, FR-07 · **Project:** notebox-web (satellite; react/next)
**Version:** v1 · **Status:** Draft · **Date:** 2026-07-23

## Origin
- **Spec:** [`features/004-annotation-types-web/spec.md`](spec.md) (approved 2026-07-23) — 30 Gherkin scenarios.
- **US / FRs:** US-1.1 (define an annotation type with an icon and typed fields). This feature is its **web
  half**: FR-01 (type CRUD through the UI), FR-02 (declare the seven typed fields, visible-for-viewing,
  Number bounds, Secret flag), FR-03 (options on choice fields, List badge colours), FR-07 (upload & render
  type/field icons; client pre-validation + thumbnail rendering).
- **BRs bound:** BR-03 (the type *is* the schema of its annotations — the builder edits that schema),
  BR-04 (the closed set of seven field types — the builder offers exactly those), BR-05 (type deletion is
  explicit and irreversible — the confirmation is the UI's job), BR-08 (no user-facing text untranslated).
- **Consumed contract (FIXED, no change):** `features/003-annotation-types/contracts/rest-api.md` —
  `/api/annotation-types` CRUD (POST/GET/GET{id}/**PUT full-replace**/DELETE), `/api/images`
  (POST raw bytes → `ImageRefDto`, GET{id} → binary), the `AnnotationTypeInput`/`AnnotationTypeDto`/
  `ImageRefDto` shapes, the `fieldType`/`badgeColour` enums, the D2 `visibleForViewing` defaults, and the
  `Problem` / `validation.failed` envelope with per-field `violations[]`. This plan re-specifies **none** of
  the server's rules; it consumes and surfaces them.
- **Governing architecture:** AD-04 (images are a reference + a dedicated binary endpoint; downscaled
  thumbnails are `notebox-web`'s concern), AD-06 (the UI is a separate deployable over synchronous REST/JSON),
  BR-08/C-09 (en + pt, never a raw key). Presentation is constrained to the **ADF Fusion** theme
  (`DESIGN-CONSTRAINT.md`, human decision 2026-07-22).
- **Builds on feat-002 web infrastructure (consumed, not re-specified):** the existing `RouteGuard`,
  `authFetch`/`ApiError`, `AuthProvider`/session, `I18nProvider`/`useTranslation`, the hand-rolled validation
  idiom, the ADF `.af-*`/`.nb-*` vocabulary, and the Vitest + MSW harness.

## Approach (three sentences)
Add an annotation-type-builder area under the existing protected `app/(app)/` route group — a **list**, a
read-only **detail**, and a full-page **create/edit builder** — that models the whole type as one editable
client-side aggregate (`EditableType` → `EditableField[]` → `EditableOption[]`) held in lifted `useState`, so
add/remove/**reorder** are in-memory array mutations and **Save serializes the entire aggregate and POSTs
(create) or PUTs (edit) it** in one request, exactly matching the API's full-replace `PUT`. New `lib/api`
clients (`annotationTypesClient`, `imagesClient`) extend the existing `authFetch`/`ApiError` seam for type CRUD
and raw-byte icon upload + authenticated binary fetch (rendered as a downscaled object-URL thumbnail, AD-04),
while a pure `annotationTypeValidation` module (mirroring `loginValidation`) runs client pre-checks and a
`violationRouting` helper maps the server's `violations[]` back onto the offending control. Every string is a
new key in the existing flat en/pt catalog, server `Problem.message` is shown verbatim, and each of the 30
Gherkin scenarios becomes a Vitest + Testing-Library test driving MSW handlers — **no new dependency**.

### 1. Routes & screen structure (App Router, under the existing `(app)` group)
The `(app)/layout.tsx` already mounts `RouteGuard` + the ADF chrome, so every screen below inherits auth
gating (C-02) and the theme automatically. New routes:
```
app/(app)/annotation-types/
  page.tsx                 # LIST  — the tenant's types (from GET /api/annotation-types)
  new/page.tsx             # CREATE builder (empty EditableType)
  [id]/page.tsx            # DETAIL — read-only full schema + Edit / Delete actions
  [id]/edit/page.tsx       # EDIT builder (EditableType hydrated from GET /api/annotation-types/{id})
```
- **List** → **Detail** → **Edit** is the navigation spine. Deletion is a modal (`.af-dialog`) confirmation
  raised from Detail (and List row action), never its own route.
- All four are Client Components (`'use client'`); they call the API only from the client, never from a Server
  Component (keeps AD-06's boundary clean; no SSR of tenant data → no hydration mismatch), matching feat-002.

### 2. Component decomposition (names + responsibilities; signatures in `contracts/interfaces.md`)
State is **lifted**: `TypeBuilderForm` owns the single `EditableType` in `useState`; every child editor is
controlled via `value` + `onChange` callbacks (the feat-002 `useState` idiom, scaled to a nested form — no
state library). Reorder/add/remove are pure array transforms on the held aggregate.

| Component | Responsibility |
|---|---|
| `AnnotationTypeList` | Renders the tenant's types in an ADF `.af-panelCollection`/`.af-table` with an `.af-toolbar` "New type"; each row sourced from the list response, with View / Edit / Delete row actions. |
| `TypeBuilderForm` | Owns `EditableType`; drives create (POST) vs edit (PUT); serialize-on-save; routes server `violations[]` + business errors back onto controls; the create/edit screens are thin wrappers over it. |
| `TypeHeaderEditor` | Type name input + `FieldIconEditor` for the type icon. |
| `FieldList` | The ordered field list: "Add field", per-field **move up / move down** (keyboard-accessible reorder), remove. |
| `FieldEditor` | One field; renders the sub-editors that vary by `fieldType` (see §3). |
| `FieldTypeSelect` | The `fieldType` chooser offering **exactly** the seven types (BR-04). |
| `VisibleForViewingToggle` | The visible-for-viewing control, seeded from the D2 default for the field type. |
| `NumberBoundsEditor` | min / max inputs — **Number only**. |
| `SecretToggle` | Secret checkbox — **Text / Free text only** (declaration only). |
| `OptionsEditor` | Ordered options with add / remove / move — **List / Single choice / Multiple choice only**. |
| `OptionRow` | One option: label input + (List only) `BadgeColourPicker`. |
| `BadgeColourPicker` | The fixed palette {RED, GREEN, BLUE, BLACK, GRAY, YELLOW} — no other colour choosable. |
| `FieldIconEditor` | Pick → client pre-validate → upload → hold `iconImageId`; hosts an `IconThumbnail`. |
| `IconThumbnail` | Renders an icon as a CSS-downscaled thumbnail from an authenticated object URL (AD-04). |
| `DeleteTypeDialog` | ADF `.af-dialog` naming the type + stating irreversibility; sends DELETE only on confirm (BR-05). |

### 3. Per-`fieldType` conditional editors (BR-04 closed set)
`FieldEditor` shows sub-editors strictly by type — this is what the FR-02/FR-03 scenarios assert:

| fieldType | Number bounds | Secret | Options editor | Badge colour on option |
|---|---|---|---|---|
| TEXT | – | ✓ | – | – |
| FREE_TEXT | – | ✓ | – | – |
| NUMBER | ✓ | – | – | – |
| LIST | – | – | ✓ | ✓ |
| SINGLE_CHOICE | – | – | ✓ | – |
| MULTIPLE_CHOICE | – | – | ✓ | – |
| IMAGE | – | – | – | – |

`visibleForViewing` seed (D2, **consumed** from feat-003 — not re-decided here): TEXT/LIST/NUMBER → `true`;
FREE_TEXT/SINGLE_CHOICE/MULTIPLE_CHOICE/IMAGE → `false`. The client keeps this D2 table only to *display* the
control's initial state; the server remains authoritative (§data-model).

### 4. Client state / view-model (core of `data-model.md`)
The in-progress type is an **editable view-model** distinct from the wire DTOs: `EditableType` /
`EditableField` / `EditableOption` carry transient client ids (stable React keys), UI-only order (array
position), raw string number inputs, per-control error slots, and a `visibleForViewingTouched` flag. On save a
pure `toInput(EditableType): AnnotationTypeInput` serializes the held aggregate — dropping per-type fields the
wire shape forbids (Secret off non-Text/Free-text, options off non-choice, bounds off non-Number, badge colour
off non-List). `fromDto(AnnotationTypeDto): EditableType` hydrates the editor when opening a type. Full DTO⇄
view-model mapping and invariants are in `data-model.md`.

### 5. API-client surface (`lib/api`, extends `authFetch`/`ApiError`)
Two new client modules, matching the existing `apiClient` idiom (thin functions over `authFetch`, throw
`ApiError` on non-2xx):
- `annotationTypesClient`: `list()`, `get(id)`, `create(input)`, `replace(id, input)`, `remove(id)`.
- `imagesClient`: `upload(contentType, bytes)` (POST raw bytes with `Content-Type: image/…` — `authFetch`
  passes the body/headers through), `fetchObjectUrl(id)` (authenticated GET → `blob()` → `URL.createObjectURL`,
  because the tenant-scoped binary endpoint needs the bearer, so a bare `<img src>` cannot be used).
- **Error routing:** `validation.failed` `violations[]` (e.g. `fields[1].name`, `fields[2].options[0].label`)
  are parsed by `violationRouting` and set on the exact control; a violation that fails to parse falls back to
  a form-level message. Business codes are shown from `Problem.message` **verbatim** (already localized):
  `annotation.type.name.taken` on the name field, `annotation.type.not_found` as a form-level message that
  makes clear the change was **not** saved, `annotation.image.too_large` / `annotation.image.type.unsupported`
  on the icon control. (Client keys exist only for the *pre-upload* rejections, which have no server message.)

### 6. Icon handling (FR-07, AD-04)
Pick → **client pre-validate** content-type ∈ {image/png, image/jpeg, image/gif, image/webp} and size ≤ 5 MB
(`5 * 1024 * 1024`); on failure show a localized message and send **no** request. On pass → `imagesClient.upload`
→ hold the returned `imageId` on the type/field (referenced in the aggregate; only persisted when the type is
saved). Thumbnails render from the served **original** via an object URL, CSS-downscaled (no server thumbnail);
the object URL is revoked on unmount/replace. Object-URL vs a token-less direct GET is settled toward object
URL because the endpoint is authenticated; the revoke detail is reversible.

### 7. Reorder = hold-the-whole-aggregate + PUT
The client holds the entire type; reorder/add/remove mutate the in-memory `EditableField[]`/`EditableOption[]`
order, and Save PUTs the whole definition — array order **is** field/option order on the wire. This is dictated
by the API being a full-replace `PUT` (no field-level endpoint exists). Reorder UI is **move up / move down**
buttons (keyboard-accessible, zero dependency); HTML5 drag is a possible later enhancement, not required.

### 8. i18n (`lib/i18n`, en + pt)
Add the builder's client-owned keys to the flat `en` catalog (extends the `MessageKey` union) and mirror them
in `pt`; `t(key)` renders them, `t()` never returns a raw key. Server error `Problem.message` is shown
verbatim (feat-002 `messageFor` idiom), never re-keyed. New keys are listed in `contracts/interfaces.md`; the
en/pt keysets must stay identical (coverage test, C-09).

### 9. Testing (Vitest + Testing-Library + MSW)
Each Gherkin scenario → one test driving MSW handlers. Add annotation-type + image handler **factories** and
fixtures to `test/msw/handlers.ts` (the existing `server.use()` per-case pattern). The seven-field-type chooser
scenario and the D2-default `Scenario Outline` become **table tests**; error-path scenarios assert the request
was/was not sent and that the message lands on the right control.

## Affected components / Blast radius
The human reviews this most closely. **Existing satellite files changed:**

| File | Change |
|---|---|
| `lib/api/types.ts` | ADD `FieldType`, `BadgeColour`, `OptionInput/Dto`, `FieldInput/Dto`, `AnnotationTypeInput/Dto`, `ImageRefDto`; **extend `Problem` with optional `violations?: Violation[]`** + a `Violation` interface. |
| `lib/api/ApiError.ts` | **Extend `parseProblem` to preserve `violations[]`** — it currently returns only `{code, message}` and would drop them, breaking field-level error routing. Additive, backward-compatible. |
| `lib/i18n/messages/en.ts` | Add the builder's client-owned keys (grows the `MessageKey` union). |
| `lib/i18n/messages/pt.ts` | Mirror the same keys (keysets must match). |
| `test/msw/handlers.ts` | Add annotation-type CRUD + image handler factories and fixtures. |
| `src/styles/adf-fusion.overrides.css` | Possibly add builder-specific `.nb-*` tweaks (kept in the overrides file; base CSS never forked). |

**New files added:**
```
lib/api/annotationTypesClient.ts          lib/api/imagesClient.ts
lib/annotationTypes/viewModel.ts          # EditableType/Field/Option + toInput/fromDto + D2 defaults
lib/validation/annotationTypeValidation.ts  lib/validation/violationRouting.ts
app/(app)/annotation-types/page.tsx
app/(app)/annotation-types/new/page.tsx
app/(app)/annotation-types/[id]/page.tsx
app/(app)/annotation-types/[id]/edit/page.tsx
components/annotationTypes/AnnotationTypeList.tsx   TypeBuilderForm.tsx   TypeHeaderEditor.tsx
components/annotationTypes/FieldList.tsx   FieldEditor.tsx   FieldTypeSelect.tsx
components/annotationTypes/VisibleForViewingToggle.tsx   NumberBoundsEditor.tsx   SecretToggle.tsx
components/annotationTypes/OptionsEditor.tsx   OptionRow.tsx   BadgeColourPicker.tsx
components/annotationTypes/FieldIconEditor.tsx   IconThumbnail.tsx   DeleteTypeDialog.tsx
test/…  (per-Gherkin-group *.test.tsx + the validation/viewModel/violationRouting unit tests)
```
**Consumes feat-003's contract only — no API change.** No route outside `annotation-types/` is modified.

## Trade-offs & alternatives rejected
- **Hold-the-whole-aggregate + PUT (chosen) vs PATCH-style partial field edits (rejected).** feat-003 exposes
  only a full-replace `PUT` on the type aggregate and **no** field-/option-level endpoint. A partial-edit UI
  would need a client-invented diff/patch protocol the server does not implement — crossing into re-specifying
  the API. Holding the aggregate and PUTting it is exactly the contract's shape and makes reorder trivial.
- **Full-page builder (chosen) vs modal builder (rejected).** The builder is a deep nested form (type + N
  fields × their per-type editors + options + icons); a modal is cramped, traps scroll, and cannot be
  deep-linked/bookmarked or survive reload. A dedicated route (`/new`, `/[id]/edit`) is roomy and linkable.
  The **delete confirmation** *is* a modal (`.af-dialog`) — small, focused, correct for a yes/no gate.
- **Native move up/down buttons (chosen) vs a drag-and-drop library — dnd-kit / react-beautiful-dnd
  (rejected).** Reorder is expressible with two buttons per row using plain array splices; this is
  keyboard-accessible by default and adds **no dependency**. A DnD library buys nicer ergonomics the spec does
  not require and would need extra work to stay accessible. Drag can be layered on later behind the same
  reorder callbacks.
- **Hand-rolled `useState` + pure validators (chosen) vs a form library + schema (react-hook-form + zod)
  (rejected).** The satellite already establishes the pure-`validateX`/`isSubmittable` + controlled-`useState`
  pattern (`loginValidation`, `LoginForm`); reusing it keeps one idiom. RHF/zod would duplicate that idiom and
  add two dependencies to solve a problem the existing pattern already solves, even nested.
- **Lifted `useState` (chosen) vs a state-management library — Redux / Zustand (rejected).** One screen's
  local aggregate; Context + lifted state suffices, matching feat-002. A store is premature.
- **Object-URL thumbnail (chosen) vs direct `<img src={GET /api/images/{id}}>` (rejected).** The binary
  endpoint is tenant-scoped and needs the bearer; a bare `<img>` sends no `Authorization`. Fetch via `authFetch`
  → blob → object URL is the only correct path, downscaled in CSS per AD-04.

## Risks
- **Violation-path parsing brittleness.** `violationRouting` parses indexed paths (`fields[1].options[0].label`);
  a mis-parse could misroute or drop a message. *Mitigation:* unit tests over representative envelopes incl.
  nested option paths; unmatched paths fall back to a visible form-level message (never silently swallowed).
  *Signal:* the FR-02/FR-03 error-path tests + `violationRouting` unit tests.
- **`visibleForViewing` D2 drift.** The client D2 table (for display) could drift from feat-003. *Mitigation:*
  a table test asserting all seven defaults; the client sends the effective value explicitly so display and
  wire agree. *Signal:* the D2 `Scenario Outline` table test.
- **Icon object-URL leaks.** Un-revoked object URLs accumulate. *Mitigation:* revoke on unmount/replace.
  *Signal:* a test asserting revoke on thumbnail unmount.
- **Lost update on full-replace PUT.** feat-003 has no ETag/optimistic-concurrency; a concurrent edit
  overwrites, and a type deleted elsewhere surfaces only as `annotation.type.not_found` on save. *Mitigation:*
  accept the contract's semantics; surface `not_found` clearly so the member is not left believing the change
  saved. *Signal:* the "edited a type deleted elsewhere" scenario. (No new OQ — this is the consumed contract.)
- **Conditional-editor leakage.** Showing Secret on a non-Text/Free-text field or options on a non-choice field
  would violate FR-02/FR-03 and could send a wire shape the server rejects. *Mitigation:* strict per-type
  rendering + `toInput` drops forbidden fields. *Signal:* the seven-type + "Secret only on Text/Free text" +
  "options not offered for non-choice" tests.
- **Locale gap / raw key.** A missing `pt` builder key violates BR-08. *Signal:* the en/pt keyset-coverage test.

## Reversibility
Client-only feature: no persistent schema, no public API is introduced, so **most decisions are reversible**.
The few that harden into something others depend on:
- **Route URL shape (`/annotation-types`, `/new`, `/[id]`, `/[id]/edit`) — one-way-ish.** Becomes bookmarkable/
  deep-linkable; fix the shape now.
- **Message-key namespace (`annotation.type.*`, `annotationType.builder.*`, `fieldType.*`, `badgeColour.*`) —
  one-way-ish.** Enters the shared catalog other screens (US-1.2/US-2.x) will read; name it stably now.
- **API-client contract (`annotationTypesClient`, `imagesClient` signatures) — one-way-ish.** `imagesClient`
  especially will be **reused** by annotation-record image fields (US-2.1) and the detail grids (US-1.2); pin
  the signatures deliberately.
- **`Problem.violations` + `parseProblem` extension — one-way-ish but low-risk.** Additive and backward
  compatible; other error handling keeps working unchanged.
- **Reversible (no deliberation owed):** the component decomposition, the `EditableType`/`EditableField`/
  `EditableOption` shape, reorder-via-buttons, the object-URL revoke strategy, sending `visibleForViewing`
  explicitly, hand-rolled validation, and the MSW handler factories — all swappable without cross-cutting churn.

## Assumptions
- **[OQ-14 — inherited from feat-003, non-blocking]:** deleting a type that owns annotation records (block vs
  cascade). No annotation records exist until US-2.1, so this builder deletes only empty types; the policy does
  not gate the UI.
- **[OQ-15 — inherited from feat-003, non-blocking]:** Secret-value encryption / masked display / role-gated
  reveal. This builder only *declares* the Secret flag on Text/Free-text fields; masking values is US-2.2/US-2.1
  and out of scope, so OQ-15 does not gate it.

No new Open Question is raised. **No new dependency is introduced** — the feature is built on the installed
`next`/`react`/`react-dom` plus the existing Vitest/MSW harness, native HTML5 controls, `fetch`, and the
hand-rolled validation/i18n modules.
