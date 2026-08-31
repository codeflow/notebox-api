# Plan — feat-021 Navigator root destinations

**Spec:** `spec.md` v1 (approved 2026-08-31) · **Satellite:** `notebox-web` (react/next)

## Origin
- **Spec:** `features/021-navigator-root-destinations/spec.md`
- **US:** US-3.1 (tree) · US-2.2 (the OQ-34 residues)
- **FRs:** FR-09, FR-05

---

## Approach

### 1. The root labels become the control the tree already has

**Nothing new is designed here.** The exact pattern this feature needs already exists **in the
same file**, in the Administration branch: a tree row whose label is a real button carrying the
destination.

```tsx
<button
  type="button"
  className="nb-treeLabel nb-treeLink"
  aria-current={pathname === item.href ? 'page' : undefined}
  onClick={() => router.push(item.href)}
>
```

The two content roots currently render `<span className="nb-treeLabel">` instead. The change is to
give them the same button, with `/annotation-types` and `/tasks` as destinations. That single
choice discharges four of the spec's risks by construction:

| Risk | Why the precedent settles it |
|---|---|
| **R1** twisty vs. label fighting over a click | They are already two sibling elements; the twisty keeps its own `onClick`. Nothing is nested inside anything else. |
| **R2** the root looking different as a control | Same `nb-treeLabel nb-treeLink` classes the Administration destinations and the type leaves already wear. The row's metrics come from the classes, not from the tag. |
| **R4** selection drifting from the URL | `aria-current` derives from `pathname`, exactly as the Administration items do. No new state. |
| C-09 | The labels are already `t('navigator.annotations')` / `t('navigator.tasks')`. **No new key.** |

**Selection nuance.** The tree's `selectedHref` is built for group-filtered leaves
(`pathname?group=…`) and is `null` when no `group` is present. The roots must not use it: their
destinations carry no query. They compare `pathname` directly, like Administration does — and
`/annotation-types` must match the list route **exactly**, not a detail route beneath it, or every
type-detail screen would light the root as "current page".

### 2. The band's two residues (OQ-34)

**2a — the link's destination gets a test.** The route string lives in the annotation-types page,
which hands `onOpenRecords` to the list. The assertion belongs where the route is real: the
route-level integration suite that already renders the page against MSW. No production change.

**2b — two toggles in one tick.** Today `toggle` reads `open` and `bands` out of the render
closure, so a second call before React re-renders sees stale values: it re-adds instead of
removing, and it starts a second `load`. Two changes, both local to the list component:

- **Toggle with a functional update.** `setOpen(prev => …)` sees the pending value, so the second
  toggle removes what the first added — the row ends collapsed, as the spec requires.
- **A `useRef<Set<string>>` of types already requested.** A ref mutates synchronously, so it is
  immune to the stale closure that `bands` (state) cannot escape. `bands` stays exactly as it is —
  it is what the band *renders from*; the ref only answers "was a request already started".

Both structures are needed and they are not redundant: `bands` can be empty for a type whose
request is in flight, which is precisely the window the double toggle exploits.

**Why not react to the state instead?** An effect that fetches for every id in `open` would be
more idiomatic, and would issue **zero** requests when the pair of toggles ends collapsed. It is
rejected because the spec says *exactly one*: the fetch starts when the member opens the row, and
a fast close does not cancel it — the same behaviour as today, and the cache keeps the result. A
design that quietly fetches less than the approved spec says is still a design that disagrees with
the spec.

---

## Alternatives rejected

1. **Make the whole tree row clickable** (label + icon + count). Rejected: the row also holds the
   twisty, so the click target would contain a control with a different job — the R1 collision,
   built in on purpose instead of avoided.
2. **A `next/link` anchor for the roots.** Rejected: this app has no `next/link` precedent
   anywhere; every destination is `router.push` from a button (recorded in `AppNav`'s own comment).
   One anchor in one tree row would be the only one in the codebase.
3. **Give the roots their own `selectedHref`.** Rejected: `selectedHref` exists for the group
   query. Extending it to cover query-less destinations would make one value mean two things; the
   Administration items already show the simpler path.
4. **Fix the double toggle with a debounce.** Rejected: a timer makes the bug invisible rather than
   absent, and it would introduce the first timing dependency in this component's tests.
5. **Cancel the in-flight request when the row closes fast.** Rejected as scope: aborting on
   unmount/close is a real, pre-existing gap tracked in `test/setup.ts`'s note, not this feature's.

---

## Blast radius

| File | Change | Risk |
|---|---|---|
| `components/navigation/Navigator.tsx` | two root labels become buttons with destinations | R1/R2/R4 — mitigated by reusing the file's own Administration pattern |
| `components/navigation/Navigator.test.tsx` | scenarios for both roots, selection, collapse-vs-navigate, keyboard, locales | — |
| `components/annotationTypes/AnnotationTypeList.tsx` | functional `setOpen`; `requested` ref guarding `load` | R3 — a guard that swallows a legitimate reopen; the cache scenario catches it |
| `components/annotationTypes/AnnotationTypeList.test.tsx` | the two-toggles scenarios | — |
| `app/(app)/annotation-types/annotationTypes.integration.test.tsx` | the band link's destination | — |

**Consumers unaffected:** the tree's data, `toNavigatorTree`, `navigationClient`, the counts, the
group filter, `TreeNode`'s `GroupNode`/`TypeNode`, and every other screen. No endpoint, no DTO, no
signature changes.

---

## Reversibility

| Decision | Kind | Note |
|---|---|---|
| Root labels are buttons with destinations | reversible | Two elements in one file; the deviation is recorded in OQ-33 so a revert would also need to reopen it. |
| `requested` ref alongside the `bands` cache | reversible | Internal to one component. |
| Fetch starts on open and is not cancelled by a fast close | **reversible, but spec-bound** | Changing it means changing the spec's "exactly one". |

---

## Risk, and the signal that reveals it

| # | Risk | Signal |
|---|---|---|
| R1 | The twisty navigating, or the label collapsing | Two scenarios, one each way: activating the twisty must not navigate; activating the label must leave the branch expanded. |
| R2 | The row's look changing | The button carries the same classes as the existing tree links; `classCoverage` covers the names, and the live pass looks at the row. |
| R3 | The fetch guard swallowing a legitimate reopen | Open → collapse → reopen must still render the band from cache with **no** second request (feat-020's existing assertion, which must keep passing). |
| R4 | The root lit as "current page" on a detail route | An assertion that a type-detail route does **not** mark the Annotations root current. |
| R5 | The double-toggle guard leaving the row open | The spec's "ends collapsed" scenario. |

---

## Contracts

No new API contract. `contracts/consumed.md` records the two routes this feature makes reachable
and the request the guard governs, so the implementer and the auditor read the same thing.
