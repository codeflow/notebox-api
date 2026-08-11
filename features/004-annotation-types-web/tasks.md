# Tasks — Annotation type builder UI (ADF Fusion theme)

**Feature:** features/004-annotation-types-web
**Status:** Draft

> Atomic tasks decomposed from the approved `plan.md` (§1–§9 + Blast radius) against
> `contracts/interfaces.md`. Each task has one outcome, ships its own tests, and is independently
> verifiable. Ordered by **dependency then risk**: the contract seam most likely to invalidate the
> plan (T-01) goes first while changing it is still cheap. Every task cites the Gherkin scenario(s)
> from `spec.md` it makes pass. `parallel: yes` marks tasks whose files are disjoint from every
> other in-flight task (safe in separate worktrees); tasks touching the same file are `no`.
> Verify convention: a scoped `npm test -- <path>` per task; the final wiring task runs full
> `npm run verify`.

---

## Foundations (highest risk to the plan)

- [x] **T-01 · Wire DTO mirrors + `Problem.violations` + `parseProblem` extension**
      - files: `lib/api/types.ts`, `lib/api/ApiError.ts`, `lib/api/ApiError.test.ts`
      - adds `FieldType`, `BadgeColour`, `OptionInput/Dto`, `FieldInput/Dto`, `AnnotationTypeInput/Dto`,
        `ImageRefDto`; extends `Problem` with optional `violations?: Violation[]` + a `Violation` interface;
        extends `parseProblem` to **preserve `violations[]`** (today drops them) — additive, backward-compatible.
      - covers: FR-02 · scenario: "A field-level validation violation is surfaced against the right field"
        (this is the seam every field-level error path depends on)
      - depends: —
      - parallel: no
      - verify: `npm test -- lib/api/ApiError`

- [x] **T-02 · i18n builder keys (en + pt) + keyset-coverage test**
      - files: `lib/i18n/messages/en.ts`, `lib/i18n/messages/pt.ts`, `lib/i18n/keysetCoverage.test.ts`
      - adds the `annotationType.*`, `field.*`, `option.*`, `fieldType.<TYPE>`, `badgeColour.<COLOUR>`,
        `annotationType.validation.*` / `image.validation.*` keys to `en` (grows the `MessageKey` union) and
        **mirrors every key in `pt`**; coverage test asserts en/pt keysets are identical (C-09).
      - covers: BR-08 · scenario: "The builder renders in the resolved locale"
      - depends: —
      - parallel: yes
      - verify: `npm test -- lib/i18n/keysetCoverage`

- [x] **T-03 · Editable view-model — `viewModel.ts` (EditableType/Field/Option, fromDto, toInput, D2 seeds)**
      - files: `lib/annotationTypes/viewModel.ts`, `lib/annotationTypes/viewModel.test.ts`
      - `emptyType/emptyField/emptyOption`, `fromDto` (DTO→editable round-trip), `toInput` (**per-fieldType
        narrowing**: drops Secret off non-Text/Free-text, options off non-choice, bounds off non-Number, badge
        colour off non-List; preserves field/option array order), `VISIBLE_DEFAULTS` (D2 seed table).
      - tests: `toInput` drops forbidden fields per type; `toInput` preserves order; `fromDto` round-trips;
        `VISIBLE_DEFAULTS` has all seven correct defaults (table test).
      - covers: FR-01, FR-02 · scenarios: "Create a type with an ordered list of fields" (toInput order),
        "Visible-for-viewing shows the per-field-type default" (VISIBLE_DEFAULTS, 7 examples)
      - depends: T-01
      - parallel: yes

- [x] **T-04 · Client pre-validation — `annotationTypeValidation.ts` (+ `validateIconFile`)**
      - files: `lib/validation/annotationTypeValidation.ts`, `lib/validation/annotationTypeValidation.test.ts`
      - `validateType` (writes `MessageKey`s into error slots, immutable), `isSubmittable`; icon pre-check
        `ALLOWED_IMAGE_TYPES`, `MAX_IMAGE_BYTES` (5·1024·1024), `validateIconFile(file)` → key | null.
      - tests: type/field/option name required; `numberMin ≤ numberMax` gate; Secret only on TEXT/FREE_TEXT;
        `validateIconFile` rejects oversize and unsupported content-type, accepts allowed ones.
      - covers: FR-07 · scenarios: "Oversize icon is rejected client-side before any upload",
        "Unsupported icon format is rejected client-side before any upload"
      - depends: T-02, T-03
      - parallel: yes

- [x] **T-05 · Server-error routing — `violationRouting.ts` (`routeViolations` + `placeBusinessError`)**
      - files: `lib/validation/violationRouting.ts`, `lib/validation/violationRouting.test.ts`
      - `routeViolations(type, problem)` parses indexed paths (`fields[1].name`, `fields[2].options[0].label`)
        onto the matching editable error slots, immutable; unparseable/unmatched paths land on the type-level
        `form` slot (never dropped). `placeBusinessError(code)` maps business codes to placement
        (`annotation.type.name.taken`→typeName, `annotation.type.not_found`→form, image codes→fieldIcon).
      - tests: nested option-path routing; unmatched path → form slot; each business code → correct placement.
      - covers: FR-02, FR-03, FR-01 · scenarios: "A field-level validation violation is surfaced against the
        right field", "A rejected option is surfaced against that option", "A duplicate type name is surfaced
        on the form"
      - depends: T-01, T-03
      - parallel: yes

- [x] **T-06 · API client — `annotationTypesClient.ts` (list/get/create/replace/remove over MSW)**
      - files: `lib/api/annotationTypesClient.ts`, `lib/api/annotationTypesClient.test.ts`
      - thin over `authFetch`/`ApiError`: `list` GET, `get` GET{id}, `create` POST→201, `replace` PUT→200,
        `remove` DELETE→204; throws `ApiError` on non-2xx.
      - tests (MSW): each verb hits the right method/URL, carries the bearer, sends **no** client tenant id,
        and parses/throws correctly.
      - covers: FR-01, C-01 · scenarios: "The type list shows the tenant's types", "Every builder request
        carries the bearer token and no client tenant identifier"
      - depends: T-01
      - parallel: yes

- [x] **T-07 · API client — `imagesClient.ts` (raw-byte upload + object-URL fetch/revoke over MSW)**
      - files: `lib/api/imagesClient.ts`, `lib/api/imagesClient.test.ts`
      - `upload(contentType, bytes)` POST raw bytes → `ImageRefDto`; `fetchObjectUrl(id)` authenticated
        GET → `blob()` → `URL.createObjectURL`; `revokeObjectUrl(url)` wrapper.
      - tests (MSW): upload sends raw bytes with `Content-Type: image/…` and bearer; `fetchObjectUrl` returns
        an object URL from the blob; `revokeObjectUrl` calls `URL.revokeObjectURL`.
      - covers: FR-07 · scenario: "Upload a type icon and render it as a thumbnail" (upload + object-URL seam)
      - depends: T-01
      - parallel: yes

- [x] **T-08 · MSW handler factories + fixtures — `test/msw/handlers.ts`**
      - files: `test/msw/handlers.ts`
      - adds `typesListSuccess`, `typeGetSuccess`, `typeCreateSuccess`, `typeReplaceSuccess`,
        `typeDeleteSuccess`, `typeNameTaken`, `typeNotFound`, `typeValidationFailed(violations)`,
        `imageUploadSuccess`, `imageTooLarge`, `imageTypeUnsupported`, and the `TYPE_RABBITMQ` fixture
        (leading-wildcard URLs, existing `server.use()` per-case idiom).
      - covers: FR-01 · scenario: "The type list shows the tenant's types" (fixtures/handlers exercised by the
        component & integration tests below — this task lands the shared test infrastructure they consume)
      - depends: T-01, T-03
      - parallel: yes

---

## Components (each ships its test)

- [x] **T-09 · `AnnotationTypeList` (list panel + row actions)**
      - files: `components/annotationTypes/AnnotationTypeList.tsx`, `components/annotationTypes/AnnotationTypeList.test.tsx`
      - ADF `.af-panelCollection`/`.af-table` with a "New type" toolbar; rows sourced from the list response
        with View / Edit / Delete row actions; empty-state key.
      - tests: renders both tenant types from the response (not from input); toolbar + row callbacks fire.
      - covers: FR-01 · scenario: "The type list shows the tenant's types"
      - depends: T-01, T-02
      - parallel: yes

- [x] **T-10 · `FieldTypeSelect` + `VisibleForViewingToggle`**
      - files: `components/annotationTypes/FieldTypeSelect.tsx`, `components/annotationTypes/VisibleForViewingToggle.tsx`,
        `components/annotationTypes/FieldTypeSelect.test.tsx`
      - `FieldTypeSelect` offers **exactly** the seven types (BR-04), localized labels; `VisibleForViewingToggle`
        seeds from `VISIBLE_DEFAULTS` for the chosen type until touched.
      - tests: chooser lists exactly the seven and no other (table over the seven); toggle shows the D2 default
        per field type until changed (table over the 7 outline rows).
      - covers: FR-02 · scenarios: "The builder offers exactly the seven field types",
        "Visible-for-viewing shows the per-field-type default until the member changes it"
      - depends: T-02, T-03
      - parallel: yes

- [x] **T-11 · `NumberBoundsEditor` + `SecretToggle` (per-type conditional editors)**
      - files: `components/annotationTypes/NumberBoundsEditor.tsx`, `components/annotationTypes/SecretToggle.tsx`,
        `components/annotationTypes/NumberBoundsEditor.test.tsx`
      - `NumberBoundsEditor` min/max inputs (Number only); `SecretToggle` checkbox (Text/Free-text only).
      - tests: min/max inputs render and feed the field on change; Secret checkbox present for Text/Free-text
        and absent for Number/List/Single/Multiple/Image (rendered under `FieldEditor` gating in T-14, asserted
        here at the component level).
      - covers: FR-02 · scenarios: "Number field exposes min and max inputs",
        "The Secret checkbox is offered only on Text and Free text fields"
      - depends: T-02, T-03
      - parallel: yes

- [x] **T-12 · `OptionsEditor` + `OptionRow` + `BadgeColourPicker`**
      - files: `components/annotationTypes/OptionsEditor.tsx`, `components/annotationTypes/OptionRow.tsx`,
        `components/annotationTypes/BadgeColourPicker.tsx`, `components/annotationTypes/OptionsEditor.test.tsx`
      - ordered options add/remove/move; badge-colour picker (List only) offering exactly
        {RED,GREEN,BLUE,BLACK,GRAY,YELLOW}; no colour control on Single/Multiple choice; editor not rendered
        for non-choice types.
      - tests: List options keep declared order + colour; Single/Multiple show no colour control; palette is
        exactly the six; Text field shows no options editor.
      - covers: FR-03 · scenarios: "Define ordered List options with badge colours", "Single and Multiple
        choice options carry no badge colour control", "The badge-colour chooser offers only the fixed palette",
        "Options editor is not offered for non-choice fields"
      - depends: T-02, T-03
      - parallel: yes

- [x] **T-13 · `FieldIconEditor` + `IconThumbnail` (pre-validate → upload → thumbnail/revoke)**
      - files: `components/annotationTypes/FieldIconEditor.tsx`, `components/annotationTypes/IconThumbnail.tsx`,
        `components/annotationTypes/FieldIconEditor.test.tsx`
      - pick → `validateIconFile` pre-check → on pass `imagesClient.upload` → hold `iconImageId` +
        object URL; `IconThumbnail` renders the CSS-downscaled object URL and **revokes on unmount/replace**;
        oversize/unsupported picks send **no** request and raise a localized message via `onError`.
      - tests: oversize/SVG picks send no upload + surface the local message; a valid PNG uploads and renders a
        thumbnail; the object URL is revoked on unmount.
      - covers: FR-07 · scenarios: "Upload a type icon and render it as a thumbnail", "Upload a per-field icon",
        "Oversize icon is rejected client-side before any upload", "Unsupported icon format is rejected
        client-side before any upload"
      - depends: T-02, T-04, T-07
      - parallel: yes

- [x] **T-14 · `FieldEditor` + `FieldList` (per-type composition + reorder/add/remove)**
      - files: `components/annotationTypes/FieldEditor.tsx`, `components/annotationTypes/FieldList.tsx`,
        `components/annotationTypes/FieldEditor.test.tsx`
      - `FieldEditor` renders name + `FieldTypeSelect` + `VisibleForViewingToggle` and the sub-editors **strictly
        by `fieldType`** (bounds/Secret/options/badge per §3 table); `FieldList` adds "Add field", keyboard-
        accessible move up/down, remove (pure array transforms).
      - tests: switching field type swaps in exactly the right sub-editors and drops the others; move up/down
        reorders; add/remove mutate the list.
      - covers: FR-02, FR-03 · scenarios: "The Secret checkbox is offered only on Text and Free text fields"
        (conditional composition), "Options editor is not offered for non-choice fields"
      - depends: T-10, T-11, T-12, T-13
      - parallel: no

- [x] **T-15 · `TypeHeaderEditor` (type name + type icon)**
      - files: `components/annotationTypes/TypeHeaderEditor.tsx`, `components/annotationTypes/TypeHeaderEditor.test.tsx`
      - type name input + `FieldIconEditor` reused for the type icon; controlled value/onChange.
      - tests: name change propagates; type icon upload sets `iconImageId` on the type.
      - covers: FR-01, FR-07 · scenario: "Upload a type icon and render it as a thumbnail"
      - depends: T-13
      - parallel: no

- [x] **T-16 · `DeleteTypeDialog` (irreversible confirmation)**
      - files: `components/annotationTypes/DeleteTypeDialog.tsx`, `components/annotationTypes/DeleteTypeDialog.test.tsx`
      - ADF `.af-dialog` naming the type + stating irreversibility (BR-05); `onConfirm` only on confirm,
        `onCancel` closes with no effect.
      - tests: dialog names the type and states irreversibility; confirm fires `onConfirm`; cancel fires only
        `onCancel`.
      - covers: FR-01 · scenarios: "Delete a type requires an explicit irreversible confirmation" (the
        confirmation UI), "Cancelling the delete confirmation sends no request"
      - depends: T-02
      - parallel: yes

- [x] **T-17 · `TypeBuilderForm` (owns EditableType; create POST / edit PUT / serialize-on-save / error routing)**
      - files: `components/annotationTypes/TypeBuilderForm.tsx`, `components/annotationTypes/TypeBuilderForm.test.tsx`
      - lifts the single `EditableType` in `useState`; composes `TypeHeaderEditor` + `FieldList`; on save runs
        `validateType`/`isSubmittable`, `toInput`, then `create` (mode create) or `replace` (mode edit); on
        `ApiError` runs `routeViolations` + `placeBusinessError` to land messages on the exact control and keeps
        input intact; `onSaved`/`onCancel` callbacks.
      - tests (MSW): create sends one request with fields in declared order; edit sends one full-replace PUT in
        the reordered order; add/remove reflected in the PUT; `annotation.type.name.taken` lands on the name
        field with no list add; `annotation.type.not_found` shows a form-level "not saved" message; a
        `fields[1].name` violation lands on that field's name (not the type name); a Number bounds violation
        lands on that Number field; a nested option violation lands on that option row; a server image rejection
        message is shown and the type is not saved with a broken icon reference.
      - covers: FR-01, FR-02, FR-03, FR-07 · scenarios: "Create a type with an ordered list of fields",
        "Edit reorders fields and saves a full replacement", "Add and remove fields while editing",
        "A duplicate type name is surfaced on the form", "Editing a type that was deleted elsewhere is
        surfaced", "A server-rejected Number bound is surfaced on the field", "A field-level validation
        violation is surfaced against the right field", "A rejected option is surfaced against that option",
        "A server image rejection is surfaced gracefully"
      - depends: T-05, T-06, T-07, T-08, T-14, T-15
      - parallel: no

---

## Routes & wiring

- [x] **T-18 · Routes under `(app)/annotation-types/` + integration tests (spine, guard, locale)**
      - files: `app/(app)/annotation-types/page.tsx`, `app/(app)/annotation-types/new/page.tsx`,
        `app/(app)/annotation-types/[id]/page.tsx`, `app/(app)/annotation-types/[id]/edit/page.tsx`,
        `app/(app)/annotation-types/annotationTypes.integration.test.tsx`,
        `src/styles/adf-fusion.overrides.css` (builder-specific `.nb-*` tweaks only, if needed)
      - list page mounts `AnnotationTypeList`; `new`/`[id]/edit` are thin wrappers over `TypeBuilderForm`
        (empty vs `fromDto`-hydrated); `[id]` renders the **read-only** full schema in stored order with
        Edit / Delete (raising `DeleteTypeDialog`); all four are `'use client'` behind the existing `RouteGuard`.
      - tests: list→detail→edit→delete spine (delete confirm → DELETE → row gone); detail shows every field
        with name/type/per-field settings in stored order; unauthenticated visit redirects to login and sends
        no request; requests carry the bearer and no client tenant id, and only tenant A's types show; renders
        fully in `pt` (no raw key/blank); a `pt` `annotation.type.name.taken` save error shows the API's `pt`
        message verbatim.
      - covers: FR-01, C-01, C-02, BR-08 · scenarios: "View a type's full schema", "Delete a type requires an
        explicit irreversible confirmation", "Visiting the builder without a session redirects to login",
        "Every builder request carries the bearer token and no client tenant identifier", "The builder renders
        in the resolved locale", "A localized server error is shown as received"
      - depends: T-09, T-16, T-17
      - parallel: no
      - verify: `npm run verify`

## Notes
- **Order reflects dependencies then risk.** T-01 (the `Problem.violations`/`parseProblem` contract seam) is
  first: it underpins every field-level error path and is the change most likely to reveal a contract mismatch.
- **Dependency chain.** T-01 → {T-03, T-06, T-07}; T-02 & T-01 → T-04, T-05; T-01/T-03 → T-08. Leaf components
  T-09/T-10/T-11/T-12/T-13/T-16 depend only on the libs (T-02/T-03/T-04/T-07). T-13 → T-15; T-10..T-13 → T-14;
  T-14 + T-15 + the clients/routing/MSW (T-05/T-06/T-07/T-08) → **T-17**; T-09 + T-16 + T-17 → **T-18** (final
  wiring + full `npm run verify`).
- **Parallelisable (disjoint files, safe in worktrees):** T-02, T-03, T-04, T-05, T-06, T-07, T-08 in the
  foundations wave; then T-09, T-10, T-11, T-12, T-13, T-16 in the component wave. `no` = T-01 (first, alone),
  T-14, T-15, T-17, T-18 (each composes/serialises files touched by its dependencies).
- **Verify convention:** scoped `npm test -- <path>` per task (shown per task); the final wiring task T-18 runs
  the full `npm run verify` (clean + lint + typecheck + test + build).
- **Scenario coverage — COMPLETE.** All scenarios in `spec.md` are cited by at least one task: FR-01 (9),
  FR-02 (6, incl. the 7-row visible-for-viewing `Scenario Outline` covered as a table test in T-03 + T-10),
  FR-03 (5), FR-07 (5), C-01/C-02 (2), BR-08/C-09 (2). No scenario is left uncovered and no task cites a
  non-existent scenario (no spec hole found).
