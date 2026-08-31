# Audit — feat-021 Navigator root destinations

**Date:** 2026-08-31 · **Agent:** opus/xhigh · **Diff:** `06a1b55..c0f8baf` on
`feature/navigator-root-destinations` @ notebox-web (5 files, +283/−12) · **Harness:**
`npm run verify` exit 0, **637 tests, 78 files**, on a tree with no dev server running.

## Verdict — **PASS with findings**

No finding blocks. Three are recorded: one is an imprecision in the approved spec that the
implementation exposed (F-01), one is a decision worth stating so it is not rediscovered as a bug
(F-02), and one is an inherited note (F-03).

---

## 1. Traceability — scenario → test → code

| Scenario | Test | Code | Chain |
|---|---|---|---|
| The Annotations root opens the annotation types list | "takes the member to the annotation types list from the Annotations root" | `router.push(ANNOTATIONS_LIST)` | ✅ |
| The Tasks root opens the tasks list | "takes the member to the tasks list from the Tasks root" | `router.push(TASKS_LIST)` | ✅ |
| A root already showing its screen reports itself as the current page | "marks the root as the current page…" — `aria-current` **and** `nb-treeRow-selected` | `pathname === …` | ⚠️ **F-01** on its second line |
| Collapsing a branch does not navigate | "collapses without navigating when the twisty is used" | separate twisty handler | ✅ + live |
| Navigating does not collapse the branch | "navigates without collapsing when the label is used" | separate elements | ✅ + live |
| A root with no children still reaches its list | "reaches the list from a root with no children" | destination is a constant, not derived from children | ✅ |
| The root destination is operable from the keyboard | "is operable from the keyboard" | native `<button>` | ✅ (jsdom; the live tier cannot — §5) |
| Both root labels named in both locales | `it.each(['en','pt'])` | existing keys | ✅ |
| The band's link lands on that type's records screen | "takes the member to that type's records screen from the band's line" | `page.tsx` route template | ✅ — **this is the F-04 the feat-020 audit raised, now closed** |
| Two toggles in one tick fetch once | "issues one records request, not two" | `requested` ref | ✅ + live (2 → 1) |
| A row toggled twice ends collapsed, reopening serves the cache | "leaves the row collapsed, and still serves the cache…" | functional `setOpen` | ✅ |

**11 of 11 scenarios reach a test.** Every chain ends in code, and two end in a browser
measurement as well.

## 2. Scenario honesty — do these tests bite?

Three probes were run and are recorded in `tasks.md`:

- ✅ **`aria-current` hardcoded** → fails the detail-route assertion, and only that one.
- ✅ **the fetch guard reverted to the closure read (`!bands.has(id)`)** → fails exactly the two new
  assertions, while feat-020's reopen-from-cache assertion keeps passing. That second half matters:
  it proves the ref did not simply *replace* the cache.
- ✅ **the route template broken (`/records` → `/record`)** → 1 failed, 18 passed. It failed
  **alone**, as `tasks.md` demanded of it.

One test was corrected during implementation rather than left dishonest: the collapse assertion
originally claimed the children leave the DOM. They do not — collapsing is `af-treeNode closed`,
CSS-driven. Asserting their absence would have asserted something the app never does. It now
asserts the class and `aria-expanded`, with the real hiding measured in the live pass (36px → 0,
`display: none`).

## 3. Constitution

- **Architecture boundary** — no client, no DTO, no endpoint, no signature touched. The tree's
  data, `toNavigatorTree`, the counts and the group filter are untouched (verified against the
  diff, not assumed).
- **Precedent over novelty (source hierarchy §4)** — the design is the Administration branch's own
  pattern, from the same file. No new idiom entered the codebase.
- **BRs** — none bound by the spec; none affected. The tree remains presentation.

## 4. Compliance — the item marked `applies`

| Item | Declared evidence | Found |
|---|---|---|
| **C-09 Localization completeness** | the locale outline, plus the keyset check; **no new key expected** | ✅ `git diff` over `lib/i18n/` is **empty** — no key was added, and the outline renders both roots by their catalog values in en and pt |

Every other item was marked `not applicable` in the spec's pre-flight with a reason; none of them
became applicable — the diff adds no data path, no value rendering, no upload, no admin action.

**NFR-09** — the deviation from design screen 05 (which draws root labels inert) is recorded in
OQ-33 *and* restated in the spec, so a future conformance pass will find the decision rather than
"correct" it.

## 5. Scope — diff vs the plan's blast radius

Five files, **all five named in the plan**:

```
app/(app)/annotation-types/annotationTypes.integration.test.tsx
components/annotationTypes/AnnotationTypeList.test.tsx
components/annotationTypes/AnnotationTypeList.tsx
components/navigation/Navigator.test.tsx
components/navigation/Navigator.tsx
```

Nothing rode along. No unrelated fix, no drive-by refactor.

## 6. Open Questions

- **OQ-33** — resolved by the product owner before the spec; this feature implements it. ✅
- **OQ-34** — resolved by the product owner (fold the residues here); both are in the diff and
  both are tested. ✅
- No OQ was closed by an implementer's assumption.

---

## Findings

### F-01 · The spec's "And the Tasks root is not [current]" is not testable as written — non-blocking

The scenario reads:

```gherkin
Then the "Annotations" root is marked as the current page
And the "Tasks" root is not
```

The second line cannot be asserted the way it implies. On `/annotation-types` the Navigator shows
**only the Annotations branch** — `showsTasks` is false outside the tasks and overview sections
(feat-018's "the tree follows the tab"). The Tasks root is not merely unmarked; it **is not
rendered at all**.

**Consequence:** an assertion written literally against that line would pass for the wrong reason
forever — the strongest form of a vacuous test, and exactly what feat-020's F-01 was.

**Disposition:** not asserted here on purpose. The behaviour it gestures at is already covered by
an existing test — *"shows only Annotations on the annotations section"* — which asserts the Tasks
branch is absent. Recorded so the next reader does not "fix" the gap by adding a test that cannot
fail. **The spec's wording, not the code, is what was imprecise.**

### F-02 · A fast double toggle still fetches for a row that ends closed — by decision, not by accident

Two toggles in one tick now issue **one** request (measured, down from two) and the row ends
collapsed. That request still completes and fills the cache for a band nobody is looking at.

This is the approved design: the plan considered an effect-driven fetch that would have issued
**zero** requests in this case and rejected it, because the spec says *exactly one* and because a
fast close does not cancel an in-flight request anywhere else in this app either. Recorded here so
an auditor does not read the orphaned response as a leak. Cancelling in-flight work on close is a
real, **pre-existing** gap noted in `test/setup.ts`; it belongs to whoever takes that on.

### F-03 · `TYPES_GRID_COLUMNS` still duplicates the list's column count — inherited, unchanged

feat-020's F-06, untouched here and out of this feature's scope. Repeated only so it does not
disappear between audits.

---

## What the live pass added that no test could

`_live-pass-2026-08-31.md` §2: the root carried `aria-current="page"` but **not**
`nb-treeRow-selected`, so it was the current page for a screen reader and for nobody looking at
the screen. The jsdom test passed — it asserted `aria-current`, correct as far as it went. The
missing half needs a stylesheet to exist at all. Fixed, and the test now asserts both halves in
both directions.

That is two features running, two live passes, two defects that a green suite could not see.

## Gate

`audit_pass` — **open**.
