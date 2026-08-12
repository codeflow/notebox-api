# Plan — Annotation record editor + secret masking & audited reveal (ADF Fusion theme)

**Feature:** features/006-annotation-records-notebox-web · **US-2.1 (web half)** · designed against
**spec v2** (rich text, OQ-19) · Date: 2026-08-11
**Satellite stack:** react/next (Next 15 App Router, React 19, TypeScript, vitest + Testing-Library +
MSW, ADF Fusion theme). Verify: `npm run verify` (clean → lint → typecheck → test → build).

## Origin
- Spec: `features/006-annotation-records-notebox-web/spec.md` **v2** (29 scenarios).
- Consumed contract: `features/005-annotation-records-notebox-api/contracts/rest-api.md` (five
  `/api/annotation-records` endpoints, PUT secret semantics of 2026-07-25, error table).
- Design truth: `notebox-web/design/handoff/` screens **12** (detail, masked secret + Reveal) and
  **13** (create/edit, one control per field type, rich Free-text editor, `af-noteWindow` violations).
- Existing patterns surveyed (reused, not reinvented): `lib/api/annotationTypesClient.ts` (client
  object + `authFetch` + `ApiError`), `lib/annotationTypes/viewModel.ts` (`EditableType`,
  `fromDto`/`toInput`), `components/annotationTypes/*` (form/field decomposition, dialog, thumbnails),
  `lib/validation/annotationTypeValidation.ts` (client-side pre-checks), `lib/i18n` catalogs +
  `keysetCoverage.test.ts`, `imagesClient.fetchObjectUrl` (authenticated binary → object URL).

## Approach (three sentences)
A new `annotationRecordsClient` mirrors the feat-004 client pattern over the five feat-005 endpoints,
and an `EditableRecord` view-model (`fromDto`/`toInput`) derives **one value control per field of the
type, in field order**, with a per-secret-field state machine that can only emit the three legal PUT
shapes (no-op echo, replacement, explicit clear). The screens are three routes nested under the
existing type surface — create, detail, edit — plus an explicit irreversible delete dialog cloned from
`DeleteTypeDialog`, with the detail view masking secrets and offering role-gated, navigation-scoped
Reveal. Rich Free-text values are edited with **TipTap** (headless ProseMirror, styled with the ADF
theme), stored as **HTML** whose embedded images carry `data-image-id` references (never bytes, never
src), and every rich value is **sanitized with DOMPurify on render** as defence in depth for C-08.

## Design

### 1. Routes (App Router, under the existing `(app)` group)
| Route | Screen | Notes |
|---|---|---|
| `annotation-types/[id]/records/new` | record create (screen 13) | type fetched, editor derived from it |
| `annotation-types/[id]/records/[recordId]` | record detail (screen 12) | masked secrets, Reveal, Delete |
| `annotation-types/[id]/records/[recordId]/edit` | record edit (screen 13) | pre-filled; PUT on save |

Navigation entry: the type detail page (`annotation-types/[id]`) gains a **"New record"** toolbar
action. After create/edit → the record's detail; after delete → back to the type detail. **No records
grid** — that surface is US-2.2; until it ships, existing records are reached from where they were
created (post-save navigation) or by URL. Stated plainly rather than smuggling a list in.

### 2. Component decomposition (signatures in `contracts/interfaces.md`)
- `RecordForm` — create/edit orchestrator (mirrors `TypeBuilderForm`): name input, ordered
  `ValueField` list, save/cancel, `violations[]` routing onto controls by `values[i]`/field path.
- `ValueField` — dispatch on `field.fieldType` (closed set, mirrors `FieldEditor`'s dispatch):
  `TEXT` → text input · `NUMBER` → number input with min/max hints · `SINGLE_CHOICE`/`LIST` → radios /
  select with badge rendering · `MULTIPLE_CHOICE` → checkboxes · `IMAGE` → `ImageValueField` ·
  `FREE_TEXT` → `RichTextEditor` · secret `TEXT`/`FREE_TEXT` → `SecretValueField`.
- `SecretValueField` — the state machine (§4): masked display, "type new value", explicit
  **Clear secret** affordance; never renders stored cleartext (it never has any).
- `RichTextEditor` — TipTap wrapper: screen-13 toolbar (bold, italic, underline, strike, colour,
  ordered/unordered list, quote, link, inline code, code block + language select, image), ADF-styled
  (`nb-*` classes in `adf-fusion.overrides.css`; the theme sheet itself is never edited).
- `RichTextValue` — read-side renderer: DOMPurify-sanitized HTML, `data-image-id` → object URL
  resolution, highlighted code blocks.
- `ImageValueField` — upload + thumbnail, reusing the feat-004 pre-validation (type/size) and
  `imagesClient`; same rules applied to images embedded in rich text.
- `RevealableValue` — masked secret on the detail view; Reveal button rendered only when
  `session.me.role === 'ADMIN'`; calls reveal, shows cleartext in place, state is component-local so
  navigation unmounts it (re-mask by construction); 403 → localized message, stays masked.
- `DeleteRecordDialog` — clone of `DeleteTypeDialog` semantics: names the record, states
  irreversibility, confirm/cancel.

### 3. API client (`lib/api/annotationRecordsClient.ts` + `types.ts` additions)
Same object pattern as `annotationTypesClient`: `create`, `get`, `replace`, `remove`,
`reveal(recordId, fieldId)`. New wire types (client-side mirrors, server authoritative):
`AnnotationValueInput`, `AnnotationRecordInput`, `AnnotationValueDto`, `AnnotationRecordDto`,
`RevealResponse`. Shapes in `contracts/interfaces.md` §1.

### 4. View-model (`lib/annotationRecords/viewModel.ts`) — core of `data-model.md`
`EditableRecord` holds `name` + one `EditableValue` per **defined field** (keyed by `fieldId`, ordered
by the type — BR-03 by construction: no control, no value, no unknown field can exist client-side).
Secret fields carry a three-state machine — `untouched` (masked; PUT emits the no-op echo
`{fieldId, text: null}`) · `replaced` (PUT emits the new text) · `cleared` (PUT emits
`{fieldId, clearSecret: true}`) — the only three legal shapes; invented cleartext is unrepresentable.
Full model in `data-model.md`.

### 5. Rich text (OQ-19) — the load-bearing decisions
- **Editor: TipTap** (`@tiptap/react` + StarterKit + Underline, TextStyle+Color, Link, Image
  customized, CodeBlockLowlight) with **lowlight** (highlight.js core) for syntax highlighting and a
  language `<select>` on the code block. *Why a dependency at all:* the satellite has no editor;
  screen 13 demands lists, colour, links and a highlighted code block with language selection —
  hand-rolling that on `contenteditable` (with `document.execCommand` deprecated) is a correctness
  and a11y swamp far beyond this feature. *Why TipTap specifically:* headless (the ADF toolbar is
  ours to style, no UI framework rides in), ProseMirror schema = enforced document shape, and
  CodeBlockLowlight ships the exact code-block behaviour. *Alternative rejected:* Lexical — equally
  capable, but its code-block/highlight path needs more custom work for the same result.
- **Stored dialect: HTML** in the value's `text` (the API stores a string ≤ 65 535 bytes and returns
  it verbatim). *Alternative rejected:* ProseMirror JSON — smaller ecosystem lock-in on read
  (every future consumer needs the schema to render), while HTML is renderable by any client and is
  what C-08's allow-list vocabulary already describes.
- **Embedded images: `<img data-image-id="<uuid>" alt="…">` — no `src`, no bytes.** The images
  endpoint is authenticated, so a persisted `src` URL cannot work from an `<img>` tag (no bearer
  header) and data-URIs would blow the 65 535 limit. Editor and renderer resolve `data-image-id`
  through `imagesClient.fetchObjectUrl` (existing authenticated-binary pattern). The sanitizer
  **strips `src` entirely** on rich values — which also kills remote-image tracking pixels.
- **Sanitize on render: DOMPurify**, allow-list exactly the editor's schema (tags: `p, br, strong,
  em, u, s, span[style: color only], ol, ul, li, blockquote, a[href: http/https only, rel
  noopener], code, pre[data-language], img[data-image-id, alt]`; everything else — scripts, event
  handlers, iframes, `javascript:` URLs — dropped). Applied in `RichTextValue` AND before hydrating
  the editor with fetched content. *Stated limit:* this is defence in depth. **C-08's primary
  (input/output) sanitization is the API's obligation and is NOT implemented there** — spec v2
  §Dependency; scoping that server-side follow-up is a pending human decision outside this plan.
- **Oversize:** a rich value can exceed 65 535 bytes; the API answers
  `annotation.record.value.too_long`; the client surfaces it on the control (no client duplicate of
  the server rule — C-09/constitution posture unchanged).

### 6. Reveal (FR-18, BR-10, C-03 client share)
Reveal state lives only in `RevealableValue` component state — unmount = re-mask, nothing persisted,
nothing logged; no store, no context. Affordance rendered only for `role === 'ADMIN'` (from the
session's `Me`); the server stays authoritative (403 → message + still masked). The reveal response is
never written anywhere except the in-place display.

### 7. i18n
New `annotationRecords.*` keys in `lib/i18n/messages/en.ts` + `pt.ts` (editor labels, toolbar action
labels, masked/reveal/clear strings, delete confirmation, local image rejections);
`keysetCoverage.test.ts` extends automatically (it walks the keyset). Server messages shown verbatim.

### 8. Testing (vitest + Testing-Library + MSW)
Scenario-per-test as in feat-004: MSW fixtures for the five endpoints incl. the masked DTO shape and
the reveal; secret state-machine tests assert the exact PUT body for untouched/replaced/cleared;
hostile-markup fixtures (script tag, event-handler attribute, `javascript:` href, `src`-bearing img)
assert inert rendering; TipTap tests drive the **editor's command API** (`editor.commands`) and assert
serialized HTML rather than simulating keystrokes — jsdom cannot host a real contenteditable
faithfully, and command-level tests pin exactly what we own (toolbar wiring + serialization).

## Blast radius
- **New:** 3 route pages; `components/annotationRecords/*` (§2, ~8 components + tests);
  `lib/api/annotationRecordsClient.ts` (+test); `lib/annotationRecords/viewModel.ts` (+test);
  `lib/annotationRecords/sanitize.ts` (DOMPurify config, +hostile-fixture test); i18n keys en+pt.
- **Modified:** `lib/api/types.ts` (new wire types); `app/(app)/annotation-types/[id]/page.tsx`
  ("New record" action); `src/styles/adf-fusion.overrides.css` (`nb-*` editor/toolbar classes);
  `package.json` (**new deps:** `@tiptap/react` + extensions, `lowlight`, `dompurify`).
- **Not touched:** the hub API (all server behaviour is feat-005's, merged); `adf-fusion.css`
  (never edited); existing feat-002/004 components; MSW/vitest infrastructure.
- **Consumers:** none yet — US-2.2 (grid/detail projection) will consume the record routes and the
  `RichTextValue` renderer.

## Risks
- **TipTap under jsdom** — editors are DOM-heavy; some APIs (ClipboardEvent, elementFromPoint) need
  shims. *Signal:* editor tests red in CI while green in a browser. *Mitigation:* command-API testing
  (§8), shims kept in `vitest.setup`, and if a given interaction cannot be tested honestly in jsdom it
  is listed as not-covered rather than faked.
- **Stored-HTML dialect drift** — a future editor swap could serialize differently and re-render old
  values subtly differently. *Mitigation:* the allow-list in `sanitize.ts` is the contract; a
  round-trip test pins serialize→sanitize→render for every toolbar feature.
- **The API-side C-08 gap** (spec v2 §Dependency) — hostile markup can enter storage past this
  client. *Signal:* the audit will flag it; it is already surfaced for a human scoping decision. This
  plan neither fixes nor hides it.
- **65 535-byte ceiling on rich values** — embedded-image references keep values small, but a long
  document can still hit it. *Signal:* `value.too_long` surfaced on the control; acceptable (server
  bound is authoritative).

## Reversibility
| Decision | Kind |
|---|---|
| Stored rich-text dialect = sanitized HTML in `text` | **one-way** (future readers parse it) |
| Embedded images = `data-image-id` references, `src` stripped | **one-way** (stored markup shape) |
| Editor = TipTap (+lowlight) | costly-reversible (swap = re-style + re-test; stored HTML survives via the dialect contract) |
| DOMPurify on render + allow-list | reversible (config file) |
| Routes nested under `annotation-types/[id]/records` | reversible until US-2.2 links them |
| Component decomposition, view-model shape | reversible |

## Assumptions
- The feat-005 contract as merged is stable (no listing endpoint; masked DTO shape; reveal is
  ADMIN-only — the client's `Role` union already carries `ADMIN`).
- The satellite's `npm run verify` is the green/red signal for this feature (hub verify is
  irrelevant here).
- Design screens 12/13 govern visuals; where the mockup used inline styles, they land as `nb-*`
  classes in `adf-fusion.overrides.css` (handoff README rule).
