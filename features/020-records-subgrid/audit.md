# Audit — feat-020 records sub-grid

**Date:** 2026-08-31 · **Agent:** opus/xhigh · **Diff audited:** `7879c9a..a9bfc28` on
`feature/records-subgrid` @ notebox-web (8 files, +1253/−61) · **Harness:** `npm run verify`
exit 0, 624 tests, 78 files.

## Verdict — Round 1 **FAIL** · Round 2 (2026-08-31) **PASS with findings**

> **Round 2 outcome:** F-01 fixed and proved to bite; `npm run verify` exit 0, 624 tests, 78
> files. F-02…F-06 stand as non-blocking, tracked below and in OQ-34. The gate opens.

### Round 1 — FAIL, reopen `implement`

One finding blocks: **C-12's declared evidence does not exist** (F-01). The *behaviour* is
correct and provable; the assertion the spec named as its evidence cannot fail, and its comment
says it checks something it does not check. That is a false signal in the compliance chain of a
secret-value control, and it costs a handful of lines to make real.

Everything else is a non-blocking finding or an observation, listed below.

> This is not a verdict about whether the feature works. It does — the live pass measured it
> under a real browser. It is a verdict about whether the artifacts still say the same thing as
> the code, and on C-12 they do not.

---

## 1. Traceability — FR → scenario → test → code

| Scenario (spec v2) | Test | Code | Chain |
|---|---|---|---|
| Expanding a row shows that type's records with that type's columns | `TypeRecordsBand.test` "shows the type's VISIBLE fields as columns" + `AnnotationTypeList.test` "expands a row into a band beneath that row and no other" | `TypeRecordsBand.tsx`, `AnnotationTypeList.tsx` | ✅ |
| Two types expanded at once keep their own columns | "keeps each open band on its own type, columns included" | `bands` Map keyed by typeId | ✅ |
| A type with more records than the band shows says so, and links out | "names the TRUE total, not the number of rows it drew" + "opens the full grid when the line is activated" | `state.page.total` | ⚠️ **F-04** — the chain stops at `onOpenRecords(id)`; the route string is asserted nowhere |
| A type with no records draws no empty table | "shows the grid's own empty message, and NO column header" | early return before `<thead>` | ✅ |
| Collapsing a row removes its band and leaves its neighbours alone | "collapses one band without touching the other" | `open` Set | ✅ |
| The rows' data is fetched when a row opens, not when the list loads | "issues no request for any type until a row is opened" + "issues exactly one type request and one records request" | `toggle` → `load` | ✅ |
| Reopening a row does not refetch | "serves a reopened row from what it already fetched" (probe-verified) | cache survives collapse | ✅ |
| A band whose records fail to load says so and stays dismissible | "says so in words when the records could not be loaded" + T-06's second test | `catch` → `failed` | ✅ |
| An expired session inside a band ends the session | "lets the 401 out to the app-wide session path" (real client + MSW 401) | `authFetch` | ✅ |
| The disclosure is operable from the keyboard and announces its state | 4 tests in "keyboard and announced state" | `aria-expanded`, native `<button>` | ✅ (live pass could not confirm activation — harness limit, documented) |
| Every string the band emits exists in both locales | `describe.each(['en','pt'])` × 4 states + catalog assertions | 4 keys | ✅ |

**11 of 11 scenarios reach a test.** One chain (the link's destination) stops one step short of
the route — F-04.

## 2. Scenario honesty — would these tests fail if the behaviour regressed?

Nine mutation probes were run during implementation and are recorded in `tasks.md`; I re-checked
the two that carry the most weight and ran one of my own.

- ✅ **Cache** — replacing `if (opening && !bands.has(id))` with `if (opening)` fails exactly the
  reopen assertion. Real.
- ✅ **`authFetch` boundary** — making the band fetch with bare `fetch()` fails the session-expired
  assertion plus nine others. Real.
- ✅ **Twisty reuse** — the test compares the button's `innerHTML` against a rendered
  `<ExpandIcon />`; a redrawn glyph fails it. Real.
- ❌ **C-12's "no reveal request"** — see F-01. Cannot fail.
- ⚠️ **`aria-expanded`** — the three state assertions were red before the attribute existed
  (recorded), so they bite.

## 3. Constitution

- **BR-09** ("visible for viewing is presentation, never access control") — the band's columns are
  `type.fields.filter(f => f.visibleForViewing)`, and a non-visible field is asserted absent both
  in jsdom and in the live pass (`Notas` never appears). Held. The flag is used for *display*
  only; no access decision rides on it.
- **Architecture boundary** — the band consumes the two existing clients and adds no endpoint, no
  DTO and no signature change. `RecordsGrid`, `RecordGridCell` and both clients are untouched by
  the diff (verified against the diff, not assumed).
- **AD / layering** — the list component now fetches, where it was documented as purely
  presentational. The doc comment was updated to say so and to state why (rows still come only
  from the prop; only the band's own detail is fetched). Deliberate and recorded, not drift.

## 4. Compliance — each item the spec marked `applies`

| Item | Declared evidence | Found |
|---|---|---|
| **C-02** Authenticated by default | "the 401 scenario" | ✅ T-06 runs the **real** records client against an MSW 401 and asserts `onSessionExpired` fires with `AUTH_TOKEN_EXPIRED` |
| **C-08** Rich-text sanitization | "hostile-markup fixture rendered inside a band" | ✅ `<img src=x onerror=…><b>bold</b>` renders with no `img`, no `b`, and a `.nb-gridPreview` |
| **C-09** Localization completeness | "the locale Scenario Outline, plus the keyset check" | ✅ both, and probes confirm each half catches what the other cannot |
| **C-12** Encryption at rest for secret values | "asserts the mask, **and asserts no reveal request is issued**" | ⚠️ **F-01 — first half only** |

## 5. Scope — diff vs the plan's blast radius

| File | In blast radius? | |
|---|---|---|
| `components/annotationTypes/AnnotationTypeList.tsx` | yes | ✅ |
| `components/annotationTypes/TypeRecordsBand.tsx` | yes (new) | ✅ |
| `lib/i18n/messages/{en,pt}.ts` | yes | ✅ |
| `src/styles/adf-fusion.overrides.css` | yes | ✅ |
| `components/annotationTypes/*.test.tsx` | yes | ✅ |
| `app/(app)/annotation-types/page.tsx` | **no** | ⚠️ **F-02** |

No unrelated change rode along. No file outside `annotationTypes` / i18n / styles was touched.

## 6. Open Questions

- **OQ-32** — resolved by the product owner on 2026-08-30, before the spec. Recorded with the
  decision text and the accepted costs. Not closed by the implementer. ✅
- **OQ-33** — opened *during* this audit stage from the owner's live use (the Navigator's root
  node reaches no list screen), decided by the owner the same day, and **kept out of this diff**.
  Routed to feat-021. ✅
- No OQ was closed by an implementer's assumption. ✅

---

## Findings

### F-01 · C-12's evidence assertion cannot fail — **blocking**

`TypeRecordsBand.test.tsx:145` "masks a Secret field and asks for no reveal (C-12)" ends with:

```js
// A listing must never trigger a reveal; the reveal path is elevated-role and audited.
expect(imagesClient.fetchObjectUrl).not.toHaveBeenCalled();
```

`fetchObjectUrl` is the **image** path (thumbnails, type icons). The reveal path is
`annotationRecordsClient.reveal(recordId, fieldId)`, called from `RevealableValue.tsx:31` — and
`annotationRecordsClient` is **not mocked in this file at all**. The fixture declares one Secret
TEXT field and no IMAGE field, so `fetchObjectUrl` could never be called under any behaviour.

**Concrete failure scenario.** Change `RecordGridCell` to render `RevealableValue` for a Secret
field — precisely the hole C-12 forbids, a reveal affordance inside a listing. An ADMIN opens a
band, the reveal button appears, a click sends `POST /annotation-records/{id}/values/{fieldId}/reveal`
and cleartext lands in a grid. **This test stays green**, because it watches the wrong client, and
the spec keeps asserting C-12 has evidence.

**Mitigation that already exists** (why this is not a security defect today): `RecordGridCell`
returns the mask and nothing else for `field.secret` (`RecordGridCell.tsx:48`), and its own suite
asserts "a secret renders only the mask (INV-G5)". The control holds. What is missing is the
signal that would catch its removal from *this* caller, which is what the spec promised.

**Fix:** mock `annotationRecordsClient` in `TypeRecordsBand.test.tsx` and assert `reveal` was not
called — the assertion the comment already claims.

### F-02 · One file outside the plan's blast radius — non-blocking

`app/(app)/annotation-types/page.tsx` gained one line: `onOpenRecords` →
`/annotation-types/{id}/records`. **This is a plan that was incomplete, not scope creep.** The
band's "see all" line has to reach the records grid, and the list component is callback-driven by
design; `onView` would have opened the *type*, not its records. It was recorded in `tasks.md` at
T-04 when it happened. The plan's blast-radius table should have included the route that owns the
list's callbacks.

### F-03 · The plan's R4 signal is not satisfiable as written — non-blocking

The plan says: *"Assert the other columns' widths are unchanged with the column present."* In a
width-constrained table (1153px measured) an added 28px column must take its space from somewhere.
Measured live: fixed-width `Icon` holds at 46px; the six flexible columns give up 3–8px each.
Nothing reorders, nothing overflows.

The unit test asserts the header set, order and labels — true and provable. No test asserts pixel
widths, and jsdom could not. **Read R4 as "the column set is not disturbed."** Recorded in
`_live-pass-2026-08-31.md` §1 and in `tasks.md`; repeated here so the next auditor does not
rediscover it as a defect.

### F-04 · The link's destination is verified live but not by a test — non-blocking

The scenario says *"activating it opens that type's full records grid."* Tests prove
`onOpenFullGrid` fires and `onOpenRecords` is called with the right type id. The route string
lives in `page.tsx` and is asserted nowhere; `annotationTypes.integration.test.tsx` does not
exercise the band. Verified by hand in the live pass (the link navigated to
`/annotation-types/{id}/records`, heading "Reunião de projeto", 13 records).

**Failure scenario:** a typo in that template literal — `/records` → `/record` — ships a 404 from
the band's only control with the whole suite green.

### F-05 · Two toggles in one tick fire two identical requests — non-blocking, low

`toggle` reads `open` and `bands` from the render closure, so a second toggle before React
re-renders sees an empty cache and calls `load` again. **Measured in the browser:** two
`t.click()` calls in the same tick produced **two** identical
`GET /annotation-records?typeId=…&page=0&size=10` requests, violating INV-B5's "once per type per
mount".

**Not reachable by human interaction, and I checked rather than assuming:** a real native
double-click on the same twisty produced exactly **one** request (React re-renders between the two
native events; the second toggle correctly closed the band). The path exists; the user cannot walk
it. A `useCallback` with functional updates, or a guard on an in-flight set, would close it.

### F-06 · `TYPES_GRID_COLUMNS = 8` duplicates the list's column count — observation

`TypeRecordsBand.tsx:9` hardcodes the outer grid's column count for its `colSpan`; the actual
count lives in `AnnotationTypeList.tsx`'s `<thead>`. Adding a ninth column would fail
"gives the grid a header row, one cell per column" (a real signal), but **nothing ties the
`colSpan` to it** — the band's own test only asserts `colspan > 1`. The two numbers agree today.

---

## What the live pass added that no test could

Recorded because it is the argument for keeping T-10 as a task: it found a defect
(`_live-pass-2026-08-31.md` §3) — the failure notice's `border` shorthand resetting
`border-color` to `currentColor`, painting the theme's soft `#d98c8c` edge in the text's red.
`classCoverage` proves only that a rule *mentions* a class; jsdom computes no styles at all.

## To reopen

```
./bin/wf reopen feat-020-records-subgrid.implement --cascade
```

Fix F-01 (the assertion C-12 was promised), then re-run `verify` and re-audit. F-02 through F-06
need no code change before publish: F-02 and F-03 are corrections to the plan's own wording, F-04
and F-05 are backlog candidates, F-06 is a note.


---

# Round 2 — 2026-08-31, after `wf reopen … --cascade`

## F-01 — closed, and the fix needed two parts

**Part 1.** `annotationRecordsClient` is now mocked in `TypeRecordsBand.test.tsx` and the test
asserts `reveal` was not called — the assertion its comment always claimed.

**Part 2, which the first part alone would not have delivered.** `RevealableValue` renders its
button only for `me?.role === 'ADMIN'`. With no auth mock, the band's tests ran roleless, so a
`RecordGridCell` that started rendering `RevealableValue` would have produced neither a button
nor a request, and the corrected assertion would have been *just as vacuous as the one it
replaced* — F-01 one level down. The band is now rendered **as ADMIN**: the adversarial case,
because an ADMIN-gated leak looks innocent to every other role.

**Probe (run 2026-08-31).** Pointing `RecordGridCell` at `RevealableValue` for Secret fields —
the hole C-12 forbids — now fails this test:

```
× masks a Secret field and asks for no reveal (C-12)
  → expect(element).not.toBeInTheDocument()
```

It fails on the **affordance** assertion, not the request one: the request needs a click. Both
assertions are kept because they cover different halves — rendering must issue no reveal, and no
control may exist to issue one. The comment in the test now says which is which.

**C-12's evidence row is now satisfied as the spec worded it.**

## Re-checks

| Check | Round 2 |
|---|---|
| Diff since Round 1 | one test file (`TypeRecordsBand.test.tsx`), +2 mocks and +1 assertion. No production code touched. |
| `verify` | exit 0 — 624 tests, 78 files, on a tree with **no dev server running** (the `.next` collision that produced two false diagnoses earlier is excluded). |
| The other five findings | unchanged; none was introduced or worsened by the fix. |
| New risk from the fix | the `useAuth` and `annotationRecordsClient` mocks are inert for every other test in the file — the band uses neither, and `RecordGridCell` uses neither. Verified by the 23 tests passing unchanged. |

## Disposition of the non-blocking findings

| Finding | Disposition |
|---|---|
| **F-02** page.tsx outside the blast radius | Recorded in `tasks.md` (T-04) and here. It is a correction to the plan's own table, not work. |
| **F-03** R4 not satisfiable as worded | Recorded in `_live-pass-2026-08-31.md` §1, `tasks.md` (T-09/T-10) and here. Correction to the plan's wording. |
| **F-04** route asserted only by hand | **OQ-34** — tracked, decision pending. |
| **F-05** two toggles in one tick → two requests | **OQ-34** — tracked, decision pending. Proved reachable programmatically and **not** reachable by a real double-click. |
| **F-06** `TYPES_GRID_COLUMNS` duplicates the column count | Observation. The header test fails if the list's column count changes, so the drift is not silent; nothing ties the `colSpan` itself. |

## Gate

`audit_pass` — **open**. Publishing and the PR follow; nothing merges here.
