# Tasks — Annotation record editor + secret masking & audited reveal (feat-006, US-2.1 web half)

> Decomposition of the approved [plan.md](plan.md) / [contracts/interfaces.md](contracts/interfaces.md) /
> [data-model.md](data-model.md), against **spec v2** (29 scenarios). All work lands in the
> **notebox-web satellite** (react/next); its `npm run verify` is the green/red signal. Each task is one
> outcome, ships its test and its i18n keys (en+pt), and cites the Gherkin scenario(s) it makes pass.
> Ordered by dependency, then risk-first: the sanitizer dialect and the TipTap editor are the two
> decisions most likely to invalidate the plan, so they go first. Leaf components before the `ValueField`
> dispatch that assembles them.

- [x] **T-01 · Editor stack dependencies + `sanitize.ts` dialect + hostile-markup fixtures**
      - files: `package.json` (deps per contracts §7), `lib/annotationRecords/sanitize.ts`, `lib/annotationRecords/sanitize.test.ts`
      - covers: C-08 (client share), OQ-19 · scenario: "Hostile markup from the API is rendered inert" (function level)
      - notes: DOMPurify configured to exactly the data-model allow-list table (the stored-HTML **dialect contract**); fixtures: `<script>`, event-handler attributes, `<iframe>`, `javascript:`/`data:` hrefs, `img[src]` (stripped, `data-image-id` kept); allowed formatting must survive untouched
      - depends: — · parallel: no  *(package.json — every later task installs on top of it)*
      - verify: `npm run test -- sanitize`
- [x] **T-02 · `RichTextEditor` — TipTap wrapper, screen-13 toolbar, dialect serialization**
      - files: `components/annotationRecords/RichTextEditor.tsx`, `components/annotationRecords/RichTextEditor.test.tsx`, `src/styles/adf-fusion.overrides.css` (`nb-*` toolbar/editor classes), `lib/i18n/messages/en.ts` + `pt.ts` (toolbar keys), `vitest.setup` shims if needed
      - covers: FR-04, OQ-19 · scenarios: "The Free-text control offers the formatting actions", "Formatting survives a save and reopen" (serialize half)
      - notes: StarterKit + Underline + TextStyle/Color + Link + customized Image (`data-image-id`, no `src`) + CodeBlockLowlight with language `<select>`; tests drive `editor.commands` and assert serialized HTML round-trips through `sanitizeRichText` unchanged (the dialect pin) — **no simulated keystrokes** (jsdom risk, plan §Risks); every toolbar action label localized
      - depends: T-01 · parallel: no  *(riskiest — must fail fast while the plan is cheap to change)*
      - verify: `npm run test -- RichTextEditor`
- [x] **T-03 · Wire types + `annotationRecordsClient` + MSW handlers**
      - files: `lib/api/types.ts` (contracts §1 additions), `lib/api/annotationRecordsClient.ts`, `lib/api/annotationRecordsClient.test.ts`, MSW fixtures (masked DTO shape, reveal, error envelopes)
      - covers: FR-04/FR-06/FR-18 (wire plumbing) · scenarios: "Every record request carries the bearer token and no client tenant identifier"
      - notes: mirrors `annotationTypesClient` exactly (`authFetch`, `ApiError`); five methods incl. `reveal(recordId, fieldId)`; tests assert bearer header present and no tenant parameter anywhere (C-01)
      - depends: — · parallel: yes  *(file-disjoint from T-01/T-02)*
      - verify: `npm run test -- annotationRecordsClient`
- [x] **T-04 · `EditableRecord` view-model — `fromType`/`fromDto`/`toInput` + secret state machine**
      - files: `lib/annotationRecords/viewModel.ts`, `lib/annotationRecords/viewModel.test.ts`
      - covers: BR-03, BR-10 (INV-R1..R4) · scenarios: "The editor offers exactly the type's fields, in order" (derivation half), "An untouched masked secret is preserved, not destroyed", "A new secret value replaces the old one", "Clearing a secret is explicit" (all at the `toInput` level: exact wire bodies)
      - notes: values keyed by the type's fields in stored order (undefined field unrepresentable); `toInput` total over `untouched → {text:null}` / `replaced → {text}` / `cleared → {clearSecret:true}`; empty non-secret values omitted; number carried as input string, parsed on `toInput`
      - depends: T-03 · parallel: no
      - verify: `npm run test -- viewModel`
- [x] **T-05 · `SecretValueField` + `RevealableValue` — mask, replace, clear, ADMIN-gated reveal, re-mask**
      - files: `components/annotationRecords/SecretValueField.tsx` (+test), `components/annotationRecords/RevealableValue.tsx` (+test), i18n keys (masked placeholder, reveal/clear affordances)
      - covers: FR-18, BR-10, C-03 (client share), C-12 (client share) · scenarios: "An ordinary read shows the secret masked", "Reveal is not offered to a member without the elevated role", "An elevated member reveals a secret value", "Revealed cleartext re-masks on navigation" (unmount = re-mask), "A refused reveal leaves the value masked"
      - notes: reveal state component-local only (no store/context/log); affordance rendered only for `role === 'ADMIN'` from the session `Me`; 403 → localized message, value stays masked; `SecretValueField` renders the three-state control and never any stored cleartext
      - depends: T-03, T-04 · parallel: yes  *(own files; worktree)*
      - verify: `npm run test -- SecretValueField RevealableValue`
- [x] **T-06 · `ImageValueField` + `RichTextValue` renderer — references, thumbnails, highlighted code**
      - files: `components/annotationRecords/ImageValueField.tsx` (+test), `components/annotationRecords/RichTextValue.tsx` (+test), i18n keys (local image rejections)
      - covers: FR-04, C-07, C-08 (render), OQ-19 · scenarios: "An image value is uploaded and referenced", "An oversize or unsupported file is rejected before upload", "An image embedded in rich text follows the image rules", "Hostile markup from the API is rendered inert" (component level), "Formatting survives a save and reopen" (render half)
      - notes: reuses feat-004 pre-validation (PNG/JPEG/GIF/WebP, ≤5 MB) + `imagesClient` upload/`fetchObjectUrl`; `RichTextValue` = `sanitizeRichText` → `data-image-id` → object URL resolution → lowlight-highlighted `pre[data-language]`; object URLs revoked on unmount
      - depends: T-01, T-03 · parallel: yes  *(own files; worktree)*
      - verify: `npm run test -- ImageValueField RichTextValue`
- [x] **T-07 · `ValueField` dispatch + plain controls (text, number, choice)**
      - files: `components/annotationRecords/ValueField.tsx`, `components/annotationRecords/ValueField.test.tsx`, i18n keys (control labels, bounds hints)
      - covers: FR-04, BR-03 · scenarios: "The editor offers exactly the type's fields, in order" (control-per-type half), "Single choice offers exactly the field's predefined options", "Multiple choice accepts several options"
      - notes: dispatch on the closed `fieldType` set (mirrors `FieldEditor`), `field.secret` → `SecretValueField`; `FREE_TEXT` → `RichTextEditor`; `IMAGE` → `ImageValueField`; SINGLE_CHOICE radios / LIST select with badge colours / MULTIPLE_CHOICE checkboxes offering exactly the type's options; NUMBER shows min/max hints but re-runs no server rule
      - depends: T-02, T-04, T-05, T-06 · parallel: no  *(assembles the leaves)*
      - verify: `npm run test -- ValueField`
- [x] **T-08 · `RecordForm` — create/edit orchestration + `violations[]` routed onto controls**
      - files: `components/annotationRecords/RecordForm.tsx`, `components/annotationRecords/RecordForm.test.tsx`, i18n keys (form labels, save/cancel)
      - covers: FR-04 · scenarios: "Create a record with values for the defined fields", "A record name is required", "A rejected value is shown against its own control", "Editing changes the values", "A record of another tenant is indistinguishable from a missing one" (form-level 404 surfacing)
      - notes: name + ordered `ValueField` list from the view-model; save → `toInput` → client create/replace; `ApiError` envelope → violations mapped to controls, business errors as localized form messages; the client re-runs no server rule (name-required surfaces the server violation on the control)
      - depends: T-07 · parallel: no
      - verify: `npm run test -- RecordForm`
- [x] **T-09 · `DeleteRecordDialog` + delete flow — explicit and irreversible**
      - files: `components/annotationRecords/DeleteRecordDialog.tsx`, `components/annotationRecords/DeleteRecordDialog.test.tsx`, i18n keys (confirmation strings)
      - covers: FR-06, BR-05 · scenarios: "Delete is confirmed before it happens", "Confirming deletes the record", "Cancelling deletes nothing"
      - notes: clone of `DeleteTypeDialog` semantics — names the record, states it cannot be undone; no request until confirm; cancel sends nothing
      - depends: T-03 · parallel: yes  *(own files; worktree)*
      - verify: `npm run test -- DeleteRecordDialog`
- [x] **T-10 · Routes + "New record" entry + localization completeness + full verify**
      - files: `app/(app)/annotation-types/[id]/records/new/page.tsx`, `…/records/[recordId]/page.tsx`, `…/records/[recordId]/edit/page.tsx` (+route tests), `app/(app)/annotation-types/[id]/page.tsx` ("New record" action), i18n final sweep
      - covers: FR-04/06/18 end-to-end, C-01, C-02, BR-08/C-09 · scenarios: "Visiting a record screen without a session redirects to login", "The screens render in the resolved locale", "A localized server error is shown as received", plus the navigation clauses (post-save → detail, post-delete → type detail)
      - notes: all three routes behind the existing `RouteGuard`; detail composes `RevealableValue` + `DeleteRecordDialog`; `keysetCoverage.test.ts` green over the full `annotationRecords.*` keyset in en+pt; closes with the satellite's **full `npm run verify`** (clean → lint → typecheck → test → build)
      - depends: T-08, T-09 · parallel: no
      - verify: `npm run verify`

## R1 remediation tasks (audit Round 1, 2026-08-11)

> Round 1 fenced T-01…T-10 as substantially honest — do not redo them. Three findings survive:
> the violation-path contract mismatch MSW hid (F-01), and two test gaps (F-02/F-03).

- [x] **T-11 · Suffix-tolerant violation-path routing (F-01)**
      - files: `components/annotationRecords/RecordForm.tsx` (`routeRecordProblem`), `components/annotationRecords/RecordForm.test.tsx`
      - covers: FR-04 · scenarios: "A record name is required", "A rejected value is shown against its own control" — made true against the REAL API's paths, not only MSW's
      - notes: the hub mapper emits raw Jakarta property paths (`create.input.name`, `create.input.values[0].text`); match the path TAIL — `/(?:^|\.)name$/` and unanchored `/values\[(\d+)\]/` — so both the prefixed real shape and the bare shape route; MSW fixtures switch to the real prefixed paths (keeping one bare-path case)
      - depends: — · parallel: no  *(behavioural — the only code change)*
      - verify: `npm run test -- RecordForm`
- [x] **T-12 · Secret empty-revert test through a stateful wrapper (F-02)**
      - files: `components/annotationRecords/SecretValueField.test.tsx`
      - covers: BR-10, INV-R4 · scenario: "An untouched masked secret is preserved, not destroyed" (the type-then-clear edge)
      - notes: wrapper loops `onChange` back into `state`; type then clear the input and assert `{tag:'untouched', hasStoredValue:true}` is emitted — and that `toInput` on that state sends the `{text:null}` echo, never `{text:''}` (which would re-encrypt empty over the stored secret)
      - depends: — · parallel: yes  *(test-only, own file)*
      - verify: `npm run test -- SecretValueField`
- [x] **T-13 · Embedded rich-text image rules tests (F-03)**
      - files: `components/annotationRecords/RichTextEditor.test.tsx`, `components/annotationRecords/ValueField.test.tsx`
      - covers: C-07, OQ-19 · scenario: "An image embedded in rich text follows the image rules"
      - notes: oversize / unsupported file through the editor's file input → localized message in the editor's error slot and NO upload call; accepted file → `uploadImage` called and a `data-image-id` node inserted (assert via serialized HTML)
      - depends: — · parallel: yes  *(test-only)*
      - verify: `npm run test -- RichTextEditor ValueField`

## Coverage & sequencing
- **29/29 spec-v2 scenarios covered:** record create/conformance (T-04/T-07/T-08), rich text (T-01/T-02/T-06), image values (T-06), secret masking + reveal (T-05), edit + secret preservation (T-04/T-08), delete (T-09), session/tenant (T-03/T-10), localization (every task's keys + T-10 coverage).
- **Dependency chain:** T-01 → T-02; T-03 → T-04 → {T-05, T-07}; {T-02, T-04, T-05, T-06} → T-07 → T-08; {T-08, T-09} → T-10.
- **Parallelisable (worktree):** T-03 (alongside T-01/T-02), T-05, T-06, T-09 (file-disjoint). T-01, T-02, T-04, T-07, T-08, T-10 serial.
- **R1 chain:** T-11 serial (sole code change); T-12, T-13 parallel (test-only, file-disjoint).
- **Uncovered scenarios:** none by omission after R1 — the two on-control error scenarios are red against the real API until T-11; the embedded-image scenario is uncovered until T-13. The API-side C-08 gap (spec v2 §Dependency) is **deliberately not a task here** — it is a server obligation pending a human scoping decision; a client task cannot close it.
