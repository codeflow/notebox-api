# Tasks — feat-020 records sub-grid

**Plan:** `plan.md` (approved 2026-08-30) · **Spec:** `spec.md` v2

Ordered by dependency, then by risk. **T-01 goes first because it is the task most likely to
invalidate the plan** — if `RecordGridCell` cannot be reused outside `RecordsGrid`, the whole
compliance argument moves, and changing the plan is cheapest before anything else is built.

---

- [x] **T-01 · Render one type's records through the shared cell renderer**
      - files: `components/annotationTypes/TypeRecordsBand.tsx`, `components/annotationTypes/TypeRecordsBand.test.tsx`
      - covers: FR-05, BR-09 · scenario: *"Expanding a row shows that type's records with that type's columns"*
      - also proves: **INV-B1**, **INV-B2**, and risks **R1/R2** — a Secret visible field renders masked with no reveal request; a hostile-markup FREE_TEXT renders as plain text
      - depends: —
      - parallel: no (everything else builds on its shape)
      - verify: `npx vitest run components/annotationTypes/TypeRecordsBand`
      - probe: render `value.text` directly instead of `RecordGridCell` — the Secret and markup assertions must both fail
      - **reopened 2026-08-31 by the audit (F-01)**: the C-12 assertion watched
        `imagesClient.fetchObjectUrl` (the IMAGE path) while the reveal path is
        `annotationRecordsClient.reveal`, which this file did not even mock — it could not fail.
        Fixed in two parts, because the first was not enough: (1) mock the reveal client and
        assert `reveal` was not called; (2) render the band **as ADMIN**, since the affordance is
        ADMIN-gated and under a roleless mock the new assertion would have been just as vacuous.
        Probe: rendering `RevealableValue` for a Secret field inside `RecordGridCell` now fails
        this test on the affordance assertion (the request needs a click, so the two assertions
        cover different halves and both are kept).

- [x] **T-02 · The empty and failed states of a band**
      - files: `components/annotationTypes/TypeRecordsBand.tsx`, its test
      - covers: scenarios *"A type with no records draws no empty table"* and *"A band whose records fail to load says so and stays dismissible"*
      - note: the empty case asserts **no column header row** — the accepted cost of option A is that a header over nothing looks wrong, so the band shows none
      - depends: T-01
      - parallel: no (same file as T-01)
      - verify: `npx vitest run components/annotationTypes/TypeRecordsBand`

- [x] **T-03 · The footer line names the true total and links out**
      - files: `components/annotationTypes/TypeRecordsBand.tsx`, its test
      - covers: scenario *"A type with more records than the band shows says so, and links out"* · **INV-B3**
      - the assertion that matters: 10 rows rendered, the line reads 47, and 47 comes from `PageDto.total`
      - depends: T-01
      - parallel: no (same file)
      - verify: `npx vitest run components/annotationTypes/TypeRecordsBand`
      - probe: source the number from `items.length` — the test must fail

- [x] **T-04 · The disclosure column, and expansion state that survives a re-render**
      - files: `components/annotationTypes/AnnotationTypeList.tsx`, `components/annotationTypes/AnnotationTypeList.test.tsx`
      - also touched, because the band cannot be wired without them: `app/(app)/annotation-types/page.tsx`
        (the new `onOpenRecords` prop → `/annotation-types/{id}/records`, the band's link target) and
        `lib/i18n/messages/{en,pt}.ts` (`annotationType.band.disclosure`, the accessible name T-08 will assert)
      - covers: scenarios *"Two types expanded at once keep their own columns"*, *"Collapsing a row removes its band and leaves its neighbours alone"* · risks **R3**, **R5**
      - reuses `ExpandIcon`/`CollapseIcon`; the lane is 28px like the navigator's `.tw` (**R4**: the other columns' widths are asserted unchanged)
      - depends: T-01
      - parallel: no
      - verify: `npx vitest run components/annotationTypes/AnnotationTypeList`

- [x] **T-05 · Fetch on open, once per type, and not before**
      - files: `components/annotationTypes/AnnotationTypeList.tsx`, its test
      - covers: scenarios *"The rows' data is fetched when a row opens"* (spec v2: **one type request and one records request**) and *"Reopening a row does not refetch"* · **INV-B4**, **INV-B5**
      - this is where the `Map` cache and the `Set` of open rows earn their separation
      - depends: T-04
      - parallel: no
      - verify: `npx vitest run components/annotationTypes/AnnotationTypeList`
      - probe: key the cache on the open set alone — the reopen assertion must fail
        · **run 2026-08-31**: `if (opening)` in place of `if (opening && !bands.has(id))` fails
        *"serves a reopened row from what it already fetched"*, and nothing else

- [x] **T-06 · A 401 inside a band ends the session, not the band**
      - files: `components/annotationTypes/AnnotationTypeList.test.tsx`
      - covers: scenario *"An expired session inside a band ends the session"* · risk **R6**
      - test-only: the behaviour comes from `authFetch`; this asserts the band did not swallow it into a local error state
      - depends: T-05
      - parallel: yes — **run in the main thread anyway**: T-06 and T-07 both write
        `AnnotationTypeList.test.tsx`, so two worktrees would have conflicted on merge
      - verify: `npx vitest run components/annotationTypes/AnnotationTypeList`
      - probe: have the band fetch with bare `fetch` instead of the client — the session-expired
        assertion fails (**run 2026-08-31**, along with 9 other band tests)

- [x] **T-07 · Keyboard operation and the announced state**
      - files: `components/annotationTypes/AnnotationTypeList.tsx`, its test
      - covers: scenario *"The disclosure is operable from the keyboard and announces its state"* (OQ-32 §4)
      - `aria-expanded` on the disclosure; the footer link reachable by tabbing onward
      - the tab-order assertion pins the path, not merely reachability: name link → View → … →
        the band's link, and never the *next* row's twisty first
      - depends: T-04
      - parallel: yes — **main thread**, same test file as T-06
      - probe: the three state assertions were red before `aria-expanded` was added (2026-08-31);
        keyboard activation needed no code, which is what a native `<button>` buys
      - verify: `npx vitest run components/annotationTypes/AnnotationTypeList`

- [x] **T-08 · The band's four strings, in both locales**
      - files: `lib/i18n/messages/en.ts`, `lib/i18n/messages/pt.ts`, `components/annotationTypes/TypeRecordsBand.test.tsx`
      - covers: Scenario Outline *"Every string the band emits exists in both locales"* · **C-09**
      - the four: disclosure accessible name, "see all N records", empty message, failure message
      - the existing keyset coverage test already fails on a key present in one locale only
      - **no catalog change was needed**: the four keys were added as their tasks landed
        (`band.failed`/`band.seeAll` in T-01..T-03, `band.disclosure` in T-04, and the empty
        message is the records grid's own). This task is the assertion that they are all
        *rendered*, in both locales, in all four band states
      - **finding for the audit**: a key missing from **pt only** falls back to English rather
        than rendering the key, so the raw-key assertion cannot see a one-sided miss —
        `keysetCoverage` is what catches that, and the probe below confirms both halves
      - depends: T-03
      - parallel: yes — **main thread** (the run order was already serial by then)
      - probes (**run 2026-08-31**): drop `{total}` from the pt line → 2 fail; delete the pt
        `band.failed` key → 5 fail, including both keyset guards; point the band at a key in
        neither catalog → 4 fail, the raw-key assertion among them in both locales
      - verify: `npx vitest run lib/i18n components/annotationTypes/TypeRecordsBand`

- [x] **T-09 · Band styling in the theme's banded row**
      - files: `src/styles/adf-fusion.overrides.css`
      - covers: no scenario of its own — it is the visual half of T-01..T-04
      - uses `.af-table tbody tr.band`, which the theme already defines
      - the `classCoverage` guard added on 2026-08-30 fails if a class is written with no rule, so this task cannot be skipped silently
      - **but that guard only proves a rule MENTIONS the class** — it scans the sheets for `.name`.
        It cannot see a rule that does nothing, which is precisely why T-10 exists
      - two traps worth recording: `.nb-msg-error` carries only colours (the `border: 1px solid`
        lives on `.nb-msg`, which a band notice does not wear, and `border-color` with no
        `border-style` draws nothing); and the muted notice colour needed `:not(.nb-msg-error)`
        because this rule sits later in the file and at equal specificity would repaint the
        failure grey
      - depends: T-04
      - parallel: yes — **main thread**
      - verify: `npx vitest run src/styles/classCoverage` → green; full `npm run verify` → **exit 0**
        (**2026-08-31**; a first run failed in `next build` with `Cannot find module for page:
        /_not-found` because a dev server from an earlier session was writing into `.next` while
        `npm run clean` deleted it — environment, not code, confirmed by the clean re-run)

- [x] **T-10 · Live browser pass**
      - files: — (evidence, recorded in the audit)
      - covers: nothing new; it is the tier the other nine cannot reach
      - **why it is a task and not a habit:** jsdom loads no stylesheet and no layout engine. On 2026-08-30 seven defects shipped with a green suite for exactly that reason, including a dialog that was invisible in a browser while every assertion passed. A band is a `<tr>` inside a `<table>` with a `colspan` — precisely the kind of thing that asserts fine and renders wrong.
      - check: band opens under a real stylesheet; columns land where the outer grid's do; two open bands do not overlap; the 28px lane leaves the other columns where they were
      - depends: T-09
      - parallel: no
      - verify: measured in a browser, numbers stated in the report — not a screenshot alone
      - **report: `_live-pass-2026-08-31.md`** · lane exactly 28px; two bands contiguous with
        zero overlap (198→234→268→304→558); band is its row's next sibling, `colspan=8`, inner
        grid 10 rows over a 13-record total; hidden field absent as a column; reopen issued
        **zero** requests (INV-B5 live)
      - **found and fixed one defect no test could see**: the failure notice drew a red border
        instead of the theme's `#d98c8c`, because T-09 used the `border` shorthand, which resets
        `border-color` to `currentColor`, after `.nb-msg-error`
      - **R4 correction**: "the other columns' widths are unchanged" is not achievable in a
        width-constrained table and is not what happens — Icon (fixed 46px) holds, the six
        flexible columns give up 3–8px each. Nothing reorders or overflows. Read R4 as "the
        column set is not disturbed"
      - **not verified here**: keyboard *activation* — synthetic Enter triggers no native button
        activation in this harness (proved with a plain control button), so it rests on T-07's
        jsdom tests. `aria-expanded` WAS read live per row

---

## Summary

| | |
|---|---|
| Tasks | 10 |
| Chain | T-01 → T-02, T-03, T-04 → T-05 → T-06 · T-04 → T-07 · T-03 → T-08 · T-04 → T-09 → T-10 |
| Parallel | T-06, T-07, T-08, T-09 (`isolation: worktree`) |
| Serial | T-01, T-02, T-03, T-05 share `TypeRecordsBand.tsx` / `AnnotationTypeList.tsx` — worktrees would conflict on merge |
| Scenarios uncovered | **none** — all 11 are cited by at least one task |
| Mutation probes named | T-01, T-03, T-05 |

**Scenario coverage check:** the 11 Gherkin scenarios map to T-01 (1), T-02 (2), T-03 (1),
T-04 (2), T-05 (2), T-06 (1), T-07 (1), T-08 (1). No task exists without a scenario, except
T-09 and T-10, which are declared above as the visual and live halves of tasks that do cite one.
