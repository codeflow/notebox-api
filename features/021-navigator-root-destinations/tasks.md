# Tasks — feat-021 Navigator root destinations

**Plan:** `plan.md` (approved 2026-08-31) · **Spec:** `spec.md` v1

Ordered so the riskiest assumption goes first. **T-01 leads because the whole design rests on one
claim — that the Administration branch's pattern transplants to a root row unchanged.** If a root
row cannot carry that button without disturbing the twisty or the layout, the plan moves, and
finding that out costs least before anything else is built.

---

- [x] **T-01 · The Annotations root becomes a destination**
      - files: `components/navigation/Navigator.tsx`, `components/navigation/Navigator.test.tsx`
      - covers: scenarios *"The Annotations root opens the annotation types list"*, *"A root already showing its screen reports itself as the current page"* · **INV-N1**
      - the assertion that matters: the push carries `/annotation-types` exactly, and `aria-current` is derived from `pathname`
      - depends: —
      - parallel: no (everything else builds on the shape)
      - verify: `npx vitest run components/navigation/Navigator`
      - probe: hardcode `aria-current="page"` on the root — the detail-route assertion must fail
        · **run 2026-08-31**: fails *"does NOT mark the root current on a detail route beneath the
        list"* and nothing else (the assertion lives in T-02, not T-03 — the task list said T-03)

- [x] **T-02 · The Tasks root, and the boundary of "current"**
      - files: same two
      - covers: scenarios *"The Tasks root opens the tasks list"*, and the **R4** boundary — a type-detail route must NOT light the Annotations root
      - `/annotation-types` matches the list route exactly, never a route beneath it
      - depends: T-01
      - parallel: no (same file)
      - verify: `npx vitest run components/navigation/Navigator`

- [x] **T-03 · Collapsing and navigating stay two different jobs**
      - files: same two
      - covers: scenarios *"Collapsing a branch does not navigate"* and *"Navigating does not collapse the branch"* · **INV-N2**, risk **R1**
      - both directions asserted; this is the regression the change most plausibly causes
      - **found while writing it:** collapsing is `af-treeNode closed`, which hides children via
        CSS — they stay in the DOM. Asserting their absence would assert something the app never
        does, so the test asserts the class and `aria-expanded`; that the hiding is real is T-07's
      - depends: T-01
      - parallel: no (same file)
      - verify: `npx vitest run components/navigation/Navigator`

- [x] **T-04 · A root with no children still reaches its list, from the keyboard, in both locales**
      - files: same two
      - covers: scenarios *"A root with no children still reaches its list"*, *"The root destination is operable from the keyboard"*, and the locale Scenario Outline · **C-09**
      - no new i18n key is expected — the outline exists to catch one being invented
      - depends: T-01
      - parallel: no (same file)
      - verify: `npx vitest run components/navigation/Navigator lib/i18n`

- [x] **T-05 · The band's link is asserted on its destination, not its callback (OQ-34a)**
      - files: `app/(app)/annotation-types/annotationTypes.integration.test.tsx`
      - covers: scenario *"The band's link lands on that type's records screen"* · audit finding **F-04**
      - test-only: renders the real page, expands a row, activates the line, asserts the navigation carries `/annotation-types/{id}/records`
      - depends: — (independent of the tree work)
      - parallel: yes (`isolation: worktree`)
      - verify: `npx vitest run app/\\(app\\)/annotation-types`
      - probe: break the route template in `page.tsx` (`/records` → `/record`) — this test must fail, and only this one
        · **run 2026-08-31**: 1 failed, 18 passed — it failed alone, as required

- [x] **T-06 · Two toggles in one tick fetch once, and end collapsed (OQ-34b)**
      - files: `components/annotationTypes/AnnotationTypeList.tsx`, its test
      - covers: scenarios *"Two toggles in one tick fetch once"* and *"A row toggled twice ends collapsed, and reopening still serves the cache"* · **INV-B5′**, **INV-N3**, risks **R3/R5**
      - functional `setOpen` + a `requested` ref; `bands` is untouched — it is empty while a request is in flight, which is why a cache-reading guard would still fire twice
      - depends: — (independent of the tree work)
      - parallel: yes (`isolation: worktree`)
      - verify: `npx vitest run components/annotationTypes/AnnotationTypeList`
      - probe: revert to the closure read (`!bands.has(id)`) — the two-toggles assertion must fail while feat-020's reopen assertion keeps passing
        · **run 2026-08-31**: exactly the two new assertions fail; feat-020's reopen-from-cache
        assertion keeps passing, which is what proves the guard did not simply replace the cache

- [x] **T-07 · Live browser pass**
      - files: — (evidence, recorded in the audit)
      - covers: nothing new; it is the tier the others cannot reach
      - **why it is a task and not a habit:** feat-020's own live pass found a defect (a `border`
        shorthand resetting `border-color`) that a green suite could not see, and jsdom cannot tell
        whether a row still looks like a row once its label becomes a button
      - check: both roots navigate under a real stylesheet; the row's height and the twisty's
        position are unchanged from today's measurements; the current-page highlight appears on the
        list route and not on a detail route
      - depends: T-04, T-06
      - parallel: no
      - verify: measured in a browser, numbers stated in the report — not a screenshot alone
      - **report: `_live-pass-2026-08-31.md`** · row height 18 / label 44px / 11px Tahoma /
        twisty 8+10 identical across the root that became a button, a span that did not and a
        leaf that already was one (R2 closed with numbers); collapsing measured at 36px → 0 with
        `display: none` and no navigation (R1); two toggles in one tick now 1 request, row ends
        collapsed (was 2 and open)
      - **found and fixed one defect no test could see:** the root carried `aria-current="page"`
        but not `nb-treeRow-selected`, so it was the current page for a screen reader and for
        nobody looking at the screen. Test extended to assert both halves, both directions

---

## Summary

| | |
|---|---|
| Tasks | 7 |
| Chain | T-01 → T-02, T-03, T-04 → T-07 · T-06 → T-07 · T-05 independent |
| Parallel | T-05, T-06 (`isolation: worktree`) — genuinely different files from the tree work |
| Serial | T-01…T-04 all write `Navigator.tsx` / its test; worktrees would conflict on merge |
| Scenarios uncovered | **none** — all 11 are cited by at least one task |
| Mutation probes named | T-01, T-05, T-06 |

**Scenario coverage check:** the 11 scenarios map to T-01 (2), T-02 (2), T-03 (2), T-04 (3),
T-05 (1), T-06 (2). T-07 cites none of its own and is declared above as the live half of the rest.
