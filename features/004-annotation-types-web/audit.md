# Audit (RE-AUDIT) — feat-004-annotation-types-web

**Auditor:** opus / xhigh · **Date:** 2026-07-24 · **This is a re-audit** of a prior **FAIL**.
**Prior verdict:** FAIL — reopen `implement`. One confirmed blocking correctness defect (Finding 1): server
`validation.failed` violations never reached their field control because `routeViolations` gated on the
violation `code` being a client `MessageKey` (`isMessageKey`), but the real feat-003 server emits
`annotation.*`-namespaced codes; field violations fell through to the type-level `form` slot. Tests were green
only because they fed catalog-matching (fake, un-prefixed) codes.
**Fix audited:** `5bbd692` on top of `e15735d`. Re-audit ran the scoped tests live (19 passing).

**RE-AUDIT VERDICT: PASS (with two minor non-blocking backlog notes).**
Finding 1 is **CLOSED** (concrete trace below). Finding 2 **CLOSED**. Finding 3 **addressed**. No scope creep.
Set `catalogs/epics.md` US-1.1 `feat-004-annotation-types-web` note from "pending" to **delivered**.

---

## Finding 1 (blocking) — CLOSED

**Proof it is closed (concrete re-derivation of the prior failure scenario).**
`routeViolations` no longer imports `en` and the `isMessageKey(code)` gate is **gone**
(`lib/validation/violationRouting.ts`). `applyViolation` now routes purely by the violation **PATH** and, on a
match, writes the client `MessageKey` *canonical for that slot* — the server code is never inspected.

Trace for the exact prior-failure input `{ field:'fields[0].name', code:'annotation.field.name.required' }`:
1. `field !== 'name'` → not the type-name slot.
2. `OPTION_PATH` no match.
3. `FIELD_PATH` matches → `fieldIndex=0`, `prop='name'` → `field.errors.name = FIELD_NAME_KEY` =
   `'field.name.required'` (a real key present in `messages/en.ts:57`).
4. `FieldEditor.tsx:91` renders `aria-invalid={field.errors.name ? true : undefined}` → **true** on field[0]'s
   name control, and `:95` renders `t('field.name.required')` = "Field name is required". Field[1] is untouched.

The message now lands on field[0]'s name control. The old code routed this to the never-rendered `errors.form`
slot; the new code does not. Out-of-range/unparseable paths (`fields[9].name`, `fields[2].options[9].label`,
`totally.unknown.path`) still fall to `errors.form = 'annotationType.saveFailed'` — never dropped
(verified in tests d, "out-of-range option", "unrecognized path"). Immutability preserved via `structuredClone`
(immutability test passes). The Number-bounds and option-label error paths route identically by path
(`field.number.bounds.invalid`, `option.label.required`).

**Tests are no longer tautological.** `violationRouting.test.ts` now feeds **real** feat-003 codes
(`annotation.field.name.required`, `annotation.field.option.label.required`, `annotation.type.name.required`,
`annotation.field.number.bounds.invalid`, `annotation.field.option.colour.invalid`) and asserts routing by
path to the correct slot's client key. Each of these WOULD FAIL if the `isMessageKey(code)` gate were
reintroduced, because a namespaced code is never a client `MessageKey` and would fall to `form` (the header
comment in the test file states exactly this). `TypeBuilderForm.test.tsx` (f) drives a real
`annotation.field.name.required` through MSW and asserts `aria-invalid` on `fieldNames[0]` only, `not` on
`fieldNames[1]`, the verbatim form-level alert ("The submitted data is invalid"), AND the inline "Field name is
required". Genuinely exercises the seam.

## Finding 2 (non-blocking) — CLOSED

`placeBusinessError` is now wired in `TypeBuilderForm.tsx:111-114`: on an `ApiError`, when
`placeBusinessError(problem.code).target === 'typeName'` (i.e. `annotation.type.name.taken`) it sets
`routed.errors.name`, so `TypeHeaderEditor` renders `aria-invalid` on the Name input while the form-level alert
still shows the server's verbatim "A type with this name already exists". `TypeBuilderForm.test.tsx` (d) now
asserts BOTH the verbatim alert and `aria-invalid='true'` on Name (MSW `typeNameTaken` emits the real
`annotation.type.name.taken` code, 409). Confirmed passing.

## Finding 3 (minor) — ADDRESSED

`FieldIconEditor.imageErrorKey` now maps the two known codes explicitly via a `switch`
(`annotation.image.too_large → image.validation.tooLarge`,
`annotation.image.type.unsupported → image.validation.typeUnsupported`); unknown codes fall back to the neutral
`typeUnsupported` key by documented intent, with the server's verbatim message authoritative at the caller.
The prior wrong-labeling of a *known* unsupported-type code is fixed. Non-blocking as before.

---

## Full checklist re-confirmed on the current tree

1. **Traceability (FR-01/02/03/07 → scenario → test → code) — HOLDS.** The previously-broken error-path links
   (FR-02 name/number, FR-03 option-label, FR-01 name-taken) now trace end-to-end through real server codes to
   the correct control. Happy-path chains (seven types, D2 defaults, palette, icon pre-validation) unchanged and
   intact.
2. **Scenario honesty — HOLDS.** The three previously-tautological tests are de-tautologized (feed real codes,
   assert by path/control; would fail under the old gate). No other tautological tests found: create-order (a),
   reorder PUT (c), client-invalid no-request (b), in-flight lock (g), not-found (e) all assert real behaviour.
3. **Constitution / BRs — HELD.** BR-03/04/05 (schema-only aggregate; closed seven-type set;
   `DeleteTypeDialog` explicit irreversible confirm, DELETE only on confirm). BR-08 (no untranslated text; every
   slot a `MessageKey`, server `message` shown verbatim) — and Finding 1's fix *removes* the prior
   localization-quality regression, since field controls now show a localized hint plus the verbatim server
   reason at form level. AD-04 (images by reference; `iconImageId` only, object-URL thumbnail). C-01 (tenant
   never client-supplied; `annotationTypesClient`/`imagesClient` send no tenant param) — HELD.
4. **Compliance — HELD.** C-02 (routes under `(app)` RouteGuard; integration asserts unauth redirect).
   C-07 (`validateIconFile` pre-validates type+size before request). C-09 (`pt.ts` typed
   `Record<MessageKey,string>` → compile-enforced keyset parity; `keysetCoverage.test.ts` present); all new keys
   used by the fix already exist in `en.ts` and therefore in `pt.ts`.
5. **Scope — CLEAN.** `5bbd692` touches exactly 5 files: `violationRouting.ts`, `TypeBuilderForm.tsx`,
   `FieldIconEditor.tsx`, and the two test files — precisely the fix surface, no creep.
6. **Open Questions — CLEAN.** OQ-14 (delete block-vs-cascade) and OQ-15 (Secret values) remain
   inherited/non-blocking; no OQ closed by implementer assumption.

**Known / non-findings (not gated):** deferred ADF visual-polish (tracked as a follow-up task); `<img>`
object-URL in `IconThumbnail` (intentional per AD-04 — a bearer-authenticated object URL cannot use Next
`<Image>`).

## New non-blocking backlog notes (do not block the gate)

- **B1 — inline hint text for `name.taken` is generic.** The name-taken placement reuses
  `annotationType.name.required`, so the inline hint under the Name field reads "Name is required" while the
  accurate reason ("…name already exists") shows only at form level. Placement (aria-invalid + verbatim form
  message) satisfies FR-01; the inline wording is imprecise. Backlog: add an `annotationType.name.taken` key and
  route to it. A cleaner long-term design is display-string error slots (widen slot type to carry the server's
  already-localized `violation.message`), noted in the prior audit's remedy — worth logging as design debt.
- **B2 — three test codes are not verbatim contract codes.** In `violationRouting.test.ts` the fieldType/secret/
  iconImageId case uses `annotation.field.type.invalid` / `.secret.invalid` / `.icon.invalid`, whereas feat-003
  emits `annotation.field.type.unknown`, `annotation.field.secret.not_allowed`, and (for a missing icon ref) the
  top-level `annotation.image.not_found`. This does **not** affect correctness or Finding 1's closure — routing
  is path-based and ignores the code entirely — but the test comment overclaims "REAL … server code". Backlog:
  align those three literals with the contract for fidelity.

---

## Recommendation

**PASS** — the blocking finding is genuinely closed; the fix routes by path with no dependence on the server
code namespace, tests are real and would catch a regression, and there is no scope creep. Open the `audit_pass`
gate and update `catalogs/epics.md` US-1.1 to reflect `feat-004-annotation-types-web` **delivered**. Log B1 and
B2 as backlog. No substep needs reopening.
