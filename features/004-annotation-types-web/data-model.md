# Data Model — Annotation type builder UI (client view-model)

**Feature:** features/004-annotation-types-web
**Project:** notebox-web (satellite; react/next) · **Version:** v1 · **Status:** Draft · **Date:** 2026-07-23

> This is a **client-only** feature. It introduces **no database, no migration, no persistent schema** — the
> only persistent shapes are feat-003's, consumed unchanged
> (`features/003-annotation-types/contracts/rest-api.md`). What lives here is the **UI view-model**: the
> editable, in-memory representation of a type while a member builds it, plus the pure mapping to/from the wire
> DTOs. Wire shapes are mirrored (not owned) in `lib/api/types.ts`; view-model shapes live in
> `lib/annotationTypes/viewModel.ts`. Full TypeScript signatures are in
> [`contracts/interfaces.md`](contracts/interfaces.md).

## Two layers, kept distinct

| Layer | Where | Owner | Purpose |
|---|---|---|---|
| **Wire DTOs** — `AnnotationTypeInput/Dto`, `FieldInput/Dto`, `OptionInput/Dto`, `ImageRefDto` | `lib/api/types.ts` | feat-003 (server authoritative) | exactly what crosses the network; the client only mirrors the shapes |
| **View-model** — `EditableType`, `EditableField`, `EditableOption` | `lib/annotationTypes/viewModel.ts` | this feature | what the builder edits; carries UI-only concerns the wire never sees |
| **Mapping** — `toInput()`, `fromDto()`, `emptyType()`, `emptyField()` | `lib/annotationTypes/viewModel.ts` | this feature | pure functions bridging the two layers |

The wire ⇄ view-model boundary is the point of this model: the view-model is deliberately **looser** than the
wire (raw strings, transient ids, per-control error slots) and `toInput()` is the **narrowing** step that
drops everything the wire forbids for a given `fieldType`.

## Entities (view-model)

### EditableType
The whole in-progress type — one instance held in `TypeBuilderForm`'s lifted `useState`.

| Field | Type | Constraints / invariants | Notes |
|-------|------|--------------------------|-------|
| `clientId` | `string` | unique within the session | transient React key only; **never** sent; a real `id` exists only on an already-persisted type |
| `serverId` | `string \| null` | UUID when editing; `null` when creating | drives POST (create) vs PUT `/{serverId}` (edit) |
| `name` | `string` | required, trimmed non-empty, ≤120 (client pre-check; server authoritative) | maps to `AnnotationTypeInput.name` |
| `iconImageId` | `string \| null` | UUID of an uploaded image in this tenant, or null | set by `FieldIconEditor` after upload; referenced, not embedded (AD-04) |
| `iconObjectUrl` | `string \| null` | object URL or null | UI-only; revoked on unmount/replace; **never** sent |
| `fields` | `EditableField[]` | **array order == wire field order**; may be empty in-progress | ordered aggregate child |
| `errors` | `TypeErrorSlots` | per-control `MessageKey` map (`name?`, `form?`) | UI-only; populated by client validation + server violation routing |

### EditableField
One field definition; array position is its order.

| Field | Type | Constraints / invariants | Notes |
|-------|------|--------------------------|-------|
| `clientId` | `string` | unique within the type | transient React key + violation-path anchor; never sent |
| `serverId` | `string \| null` | UUID when hydrated from a saved type; else null | informational; the wire carries no field id on input |
| `name` | `string` | required, trimmed non-empty, ≤120 (client pre-check) | `FieldInput.name` |
| `fieldType` | `FieldType` | required; ∈ `TEXT\|LIST\|NUMBER\|FREE_TEXT\|SINGLE_CHOICE\|MULTIPLE_CHOICE\|IMAGE` (BR-04 closed set) | drives which sub-editors render (plan §3) |
| `iconImageId` | `string \| null` | UUID or null | optional field icon |
| `iconObjectUrl` | `string \| null` | object URL or null | UI-only; revoked on unmount/replace; never sent |
| `visibleForViewing` | `boolean` | seeded from the **D2 default** for `fieldType` | see D2 table + `visibleForViewingTouched` |
| `visibleForViewingTouched` | `boolean` | UI-only | if `false`, the value shown is the D2 default and re-seeds when `fieldType` changes; once the member toggles, it sticks |
| `secret` | `boolean` | default `false`; **only meaningful for TEXT / FREE_TEXT** | declaration only; value encryption is US-2.1/OQ-15 (out of scope) |
| `numberMin` | `string` | raw input; **NUMBER only**; parsed on serialize; optional | kept as string so partial/invalid input is editable; `toInput` parses to number\|null |
| `numberMax` | `string` | raw input; **NUMBER only**; parsed on serialize; optional | client pre-check `min ≤ max` when both present |
| `options` | `EditableOption[]` | **ordered**; only for LIST / SINGLE_CHOICE / MULTIPLE_CHOICE | array position == wire option order |
| `errors` | `FieldErrorSlots` | per-control `MessageKey` map (`name?`, `fieldType?`, `numberBounds?`, `secret?`, `icon?`, `options?`) | UI-only |

### EditableOption
One option on a choice field; array position is its order.

| Field | Type | Constraints / invariants | Notes |
|-------|------|--------------------------|-------|
| `clientId` | `string` | unique within the field | transient React key + violation-path anchor; never sent |
| `label` | `string` | required, trimmed non-empty (client pre-check) | `OptionInput.label` |
| `badgeColour` | `BadgeColour \| null` | ∈ `RED\|GREEN\|BLUE\|BLACK\|GRAY\|YELLOW`; **LIST only**; null on Single/Multiple choice | fixed palette (FR-03); `toInput` drops it for non-List fields |
| `errors` | `OptionErrorSlots` | per-control `MessageKey` map (`label?`, `badgeColour?`) | UI-only |

## Relations
- `EditableType` **1 — 0..N** `EditableField` (ordered by array index; the aggregate the member edits and Save
  serializes whole — plan §7).
- `EditableField` **1 — 0..N** `EditableOption` (ordered; present only for LIST / SINGLE_CHOICE /
  MULTIPLE_CHOICE — enforced by conditional rendering + `toInput` narrowing).
- `EditableType.iconImageId` and `EditableField.iconImageId` **reference** an `ImageRefDto.id` produced by
  `POST /api/images` (a reference, never embedded bytes — AD-04). The referenced image is only *persisted onto
  the type* when the type itself is saved.

## Wire ⇄ view-model mapping (the only "migration" here)

### `fromDto(dto: AnnotationTypeDto): EditableType` — hydrate the editor (DETAIL / EDIT open)
- assigns fresh `clientId`s to the type, every field, every option (React keys);
- copies `id → serverId` (type + fields), `name`, `iconImageId`;
- `numberMin/Max` number|null → string (`""` when null);
- `options` copied in order; `badgeColour` preserved (null when absent);
- `visibleForViewing` copied as returned by the server, with `visibleForViewingTouched = true` (the value is
  authoritative, not a default to re-seed);
- `secret` copied as returned;
- `errors` initialized empty; `iconObjectUrl` null until the thumbnail loads.

### `toInput(t: EditableType): AnnotationTypeInput` — serialize on Save (the **narrowing** step)
Per field, by `fieldType` — **drop everything the wire shape forbids** so the client never sends an invalid
combination (defence-in-depth behind the conditional UI; matches feat-003's `@…AllowedForFieldType`
constraints):

| fieldType | emits `numberMin/Max` | emits `secret` | emits `options` | option `badgeColour` |
|---|---|---|---|---|
| TEXT | ✗ (null) | ✓ | ✗ (omit) | — |
| FREE_TEXT | ✗ | ✓ | ✗ | — |
| NUMBER | ✓ (parsed; null if blank) | ✗ (false) | ✗ | — |
| LIST | ✗ | ✗ (false) | ✓ | ✓ (kept) |
| SINGLE_CHOICE | ✗ | ✗ | ✓ | ✗ (dropped → null) |
| MULTIPLE_CHOICE | ✗ | ✗ | ✓ | ✗ (dropped → null) |
| IMAGE | ✗ | ✗ | ✗ | — |

- `name` trimmed; `iconImageId` passed through (null when unset);
- array order preserved verbatim → becomes field/option order on the wire;
- `numberMin/Max` parsed from string to `number | null` (blank → null);
- transient `clientId`, `serverId`, `iconObjectUrl`, all `errors`, and `visibleForViewingTouched` are **never**
  serialized.

### Seeds
- `emptyType(): EditableType` — fresh `clientId`, `serverId=null`, empty name/fields, no icon.
- `emptyField(fieldType?): EditableField` — fresh `clientId`, `visibleForViewing` seeded from the **D2 default**
  (below), `secret=false`, empty options.

## D2 — `visibleForViewing` defaults (consumed from feat-003, not re-decided)
Used **only to seed the UI control**; the client sends the effective value explicitly and the server stays
authoritative.

| fieldType | default |
|---|---|
| TEXT | `true` |
| LIST | `true` |
| NUMBER | `true` |
| FREE_TEXT | `false` |
| SINGLE_CHOICE | `false` |
| MULTIPLE_CHOICE | `false` |
| IMAGE | `false` |

## Invariants ↔ Business Rules
- **BR-03** (the type *is* the schema of its annotations): the editable aggregate is exactly the schema; no
  annotation-record shape appears here (records are US-2.1, out of scope).
- **BR-04** (closed set of seven field types): `FieldType` is a fixed union; `FieldTypeSelect` offers exactly
  these; no free-text field type is representable.
- **BR-05** (deletion explicit & irreversible): no cascade/soft-delete state in the model; `DeleteTypeDialog`
  gates a `remove(id)` that the client cannot undo — the audit is server-side (C-10, feat-003).
- **BR-08 / C-09** (nothing untranslated): every error slot holds a `MessageKey`, never a literal string;
  server `Problem.message` is shown verbatim (already localized).

## Migrations
**None.** No table, column, or index is created or altered — this feature persists nothing of its own. All
persistence is feat-003's, consumed via REST. The only transformation defined here is the pure in-memory
`fromDto` / `toInput` mapping above.

## Notes (retention / security / compliance — cross-ref constitution/02-compliance.md)
- **C-01 (tenant isolation, client obligation):** the model carries **no tenant identifier**; tenant is derived
  server-side from the bearer (feat-002 `authFetch`). No field lets a member choose or override a tenant.
- **C-04 (data minimization):** the view-model holds only type-definition data + transient UI state; **no PII**,
  no password, no token. `iconObjectUrl` is a session-only blob URL, revoked on unmount.
- **C-07 (image upload safety, client obligation):** icon uploads are content-type + size pre-validated
  (`{image/png,jpeg,gif,webp}`, ≤5 MB) before `imagesClient.upload`; the server remains authoritative and its
  `annotation.image.too_large` / `annotation.image.type.unsupported` are surfaced on the icon control.
- **OQ-15 (Secret values):** `secret` is a **boolean flag only**. No cleartext secret value is ever held,
  masked, or revealed in this model — value encryption / role-gated reveal is deferred to US-2.1 (out of scope,
  non-blocking here).
- **Retention:** nothing to retain — the view-model is ephemeral UI state discarded on navigation; persisted
  type data is governed by feat-003's retention path, not this feature.
