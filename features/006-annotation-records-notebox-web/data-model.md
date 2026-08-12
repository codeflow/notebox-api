# Data model — Annotation record editor (client view-model)

**Feature:** features/006-annotation-records-notebox-web · client-side only — **no database, no
migration**. The persistent model is feat-005's (see its `data-model.md`); this file defines the
client's working state and its invariants. Wire shapes are in `contracts/interfaces.md` §1.

## EditableRecord (lib/annotationRecords/viewModel.ts)

```ts
interface EditableRecord {
  recordId: string | null;          // null while creating
  typeId: string;
  name: string;                      // required, ≤120 (server-validated; client shows the violation)
  values: EditableValue[];           // EXACTLY one per defined field, in the type's field order
}

type EditableValue =
  | { kind: 'text';    fieldId: string; text: string }                        // TEXT
  | { kind: 'rich';    fieldId: string; html: string }                        // FREE_TEXT (OQ-19)
  | { kind: 'number';  fieldId: string; number: string }                      // NUMBER (input string; parsed on toInput)
  | { kind: 'choice';  fieldId: string; optionIds: string[] }                 // SINGLE_CHOICE / LIST (0..1) · MULTIPLE_CHOICE (0..n)
  | { kind: 'image';   fieldId: string; imageId: string | null }              // IMAGE
  | { kind: 'secret';  fieldId: string; state: SecretValueState };            // secret TEXT / FREE_TEXT

type SecretValueState =
  | { tag: 'untouched'; hasStoredValue: boolean }   // masked; the client has NO cleartext
  | { tag: 'replaced';  text: string }              // member typed a new value
  | { tag: 'cleared' };                             // member used the explicit clear affordance
```

### Derivation
- `fromType(type)` — create mode: one `EditableValue` per `type.fields[]`, empty, in field order.
- `fromDto(record, type)` — edit mode: values keyed by `fieldId` from the DTO; a secret value maps to
  `{tag:'untouched', hasStoredValue: dto.masked && …present}`; a missing value for a defined field
  gets the empty `EditableValue` of its kind.
- `toInput(editable)` — the ONLY producer of the wire body. Empty values are **omitted** (a field
  with no value sends nothing — contract: omitted non-secret = removed, which is what an emptied
  control means). Secret mapping is total:

| SecretValueState | PUT `values[]` entry | Contract meaning |
|---|---|---|
| `untouched` | `{ fieldId, text: null }` | no-op preserve (the masked echo) |
| `replaced`  | `{ fieldId, text }` | re-encrypt under the active key |
| `cleared`   | `{ fieldId, clearSecret: true }` | audited erasure |

## Invariants
- **INV-R1 (BR-03):** `values` is built from the type's field list and nothing else — a value for an
  undefined field is unrepresentable client-side; the server remains authoritative for conformance.
- **INV-R2 (order):** `values` order == the type's stored field order; the form renders it verbatim.
- **INV-R3 (BR-10):** stored-secret cleartext never enters `EditableRecord` — `untouched` carries no
  text by type; only member-typed text exists in `replaced`. Reveal cleartext lives exclusively in
  `RevealableValue` component state (unmount = gone) and is never merged into the view-model.
- **INV-R4 (PUT safety):** `toInput` is total over `SecretValueState` — no fourth shape exists, so
  the client can never invent cleartext nor silently destroy a secret.
- **INV-R5 (C-08 render):** no fetched `html` reaches the DOM or the editor without passing
  `sanitize()` (allow-list below); `RichTextValue` and editor hydration share the one function.
- **INV-R6 (images):** rich-text embedded images carry `data-image-id` only; `src` never survives
  sanitization; binary display always goes through `imagesClient.fetchObjectUrl` (authenticated).

## Sanitizer allow-list (lib/annotationRecords/sanitize.ts — the dialect contract)
| Element | Allowed attributes |
|---|---|
| `p`, `br`, `strong`, `em`, `u`, `s`, `ol`, `ul`, `li`, `blockquote`, `code` | — |
| `span` | `style` (colour only) |
| `a` | `href` (http/https only), `rel="noopener noreferrer"` |
| `pre` | `data-language` |
| `img` | `data-image-id`, `alt` (**`src` stripped**) |

Everything else is dropped: `script`, event-handler attributes, `iframe`, `javascript:`/`data:` URLs.
This table is the stored-HTML dialect — the editor must emit only this, the sanitizer accepts only
this, and the round-trip test pins both.

## Reveal state (component-local, deliberately outside the view-model)
`RevealableValue`: `{ revealed: string | null }` — set by a successful reveal call, cleared by
unmount (navigation re-masks by construction — no listener, no timer, no store to forget to clear).

## Not modelled here
Server entities, encryption, audit rows, tenant scoping — feat-005's `data-model.md`. The records
grid and visible-field projection — US-2.2.
