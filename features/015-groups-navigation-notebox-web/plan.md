# Plan — Item groups and navigation-tree data (web)

**ID:** features/015-groups-navigation-notebox-web
**User Story:** US-3.1 · **Version:** v1
**Status:** Approved (human approval 2026-08-26)
**Date:** 2026-08-26
**Project:** `notebox-web` (Next.js App Router + React + TypeScript) — routed satellite
**Spec:** [spec.md](spec.md) — approved 2026-08-24, 26 scenarios

## Origin
- **Spec:** `features/015-groups-navigation-notebox-web/spec.md` (approved 2026-08-24).
- **FRs (client half):** FR-08 (manage groups, assign an item to one), FR-09 (the Navigator, C28, with click-to-detail routing C30).
- **Wire truth, mirrored and never re-derived:**
  `features/014-groups-navigation-notebox-api/contracts/groups-navigation.md` and
  `features/016-group-counts/contracts/group-counts.md` — both merged to `develop`.
- **Decisions this plan implements:** OQ-04 (flat, single-membership, per-domain), **OQ-23 as
  clarified 2026-08-24** (payload is `type → groups`; the **UI nests `group → type`**, inverted
  client-side), OQ-24 (delete un-groups), OQ-25 (order comes from the payload), **OQ-27** (counts
  consumed, never computed).
- **Visual source of truth:** `notebox-web/design/handoff` — **screen 05** (workspace home: left
  accordion Navigator) and **screen 14** (Groups management), as amended 2026-08-24 (`2ee5e3f`).
- **Verify signal:** the satellite's own — `npm run verify` (clean → lint → typecheck → test →
  build). The hub's `mvn -B verify` proves nothing here.

## Approach

**The Navigator is chrome, so it belongs in the layout.** `app/(app)/layout.tsx` currently renders
`BrandingBar` + `AppNav` + a main panel. The tree becomes a **left panel inside that layout**, so it
persists across route changes and is fetched once per mount rather than per page. Design 05 shows a
splitter between the panels; the splitter's *drag-to-resize* is out of scope (spec), so it renders
as the static divider the mockup's CSS already provides.

**The inversion lives in one pure function, not in a component.** `GET /navigation` returns
`type → [groups]`; design 05 draws `group → [types]`. A single `toNavigatorTree(payload)` in
`lib/navigation/viewModel.ts` performs the inversion and nothing else — it is pure, so the
group→type mapping, the `Ungrouped` placement and the count arithmetic are unit-testable without
rendering anything. Components consume the inverted shape and stay dumb. Putting the inversion in a
`useEffect` or inside the tree component is what makes a rendering concern untestable.

**Ordering is consumed, never recomputed.** The payload arrives name-ordered with `Ungrouped` last
(OQ-25). After inversion the *group* order is derived from first appearance across types, so the
view model preserves the payload's relative order rather than re-sorting — re-sorting client-side
would silently fork the ordering contract the API owns, and OQ-25 would then be enforced in two
places that can disagree.

**A group node's count is the sum of its types' counts; a type node's count is the API's.** Under
`group → type`, the API gives a count per (type, group) pair. The type node under a group shows that
number verbatim; the group node shows the sum of its children. That sum is arithmetic on numbers the
API computed — not a second source of truth — and it is exactly what design 05 draws
(`Ungrouped (3)` above `Scratch (3)`).

**Routing carries the filter in the URL, not in state.** Clicking a type node under a group goes to
`/annotation-types/{typeId}/records?group={groupId|none}`; a task group node to
`/tasks?group={groupId|none}`. The existing pages read the param and pass it to the shipped clients,
so the view is shareable and reload-safe, and node selection derives from the URL rather than from a
click handler's memory — no selection state to desynchronise.

**`groupId` joins the existing full-echo builders — that is the whole rollout fix.** Both domains
already have the discipline feat-012/feat-013 established: `lib/tasks/viewModel.ts`'s
`taskToDraft`/`toTaskInput`/`toSubtaskInput` and `lib/annotationRecords/viewModel.ts`'s
`fromDto`/`toInput`. Adding `groupId` **to those builders** means every existing call site echoes it
automatically; no caller has to remember. Making it `groupId: string | null` (**not** optional) in
the mirrored input types turns a forgotten key into a **compile error**, which is the only mechanism
that actually closes feat-014's PUT-replace hazard rather than documenting it.

**Groups management is one route with two panels.** `/groups` renders two independent
`GroupsPanel` instances — one per domain — each with New/Rename/Delete. The domain comes from the
panel's prop, never from a control, so the cross-domain rejection the API enforces is unreachable
from the UI. The delete dialog states what actually happens (OQ-24: the group goes, its items become
ungrouped, nothing is deleted) — the BR-05 confirmation the API deliberately does not perform.

**`averageStatus: null` renders as "no tasks", never as 0%.** The contract's explicit warning. A
dedicated `formatAverageStatus` helper returns the em-dash for `null` and a progress bar for a
number, so the distinction is one tested function rather than a `?? 0` waiting to happen in JSX.

## Design (seams)

| Seam | File | Responsibility |
|---|---|---|
| wire mirrors | `lib/api/types.ts` (extend) | `GroupDto`, `GroupInput`, `NavigationTreeDto`, node types, `groupId` on the four item types |
| clients | `lib/api/groupsClient.ts` (new), `navigationClient.ts` (new) | `/groups` CRUD + `/navigation`; `list(domain, page, size)` |
| clients | `lib/api/tasksClient.ts`, `annotationRecordsClient.ts` (extend) | optional `group` query param on `list` |
| **inversion** | `lib/navigation/viewModel.ts` (new) | `toNavigatorTree` — pure; group→type, counts, Ungrouped last |
| formatting | `lib/groups/format.ts` (new) | `formatAverageStatus` (null → "no tasks") |
| echo | `lib/tasks/viewModel.ts`, `lib/annotationRecords/viewModel.ts` (extend) | `groupId` in draft + input builders |
| chrome | `app/(app)/layout.tsx` (extend) | mounts `<Navigator/>` as the left panel |
| tree | `components/navigation/Navigator.tsx`, `TreeNode.tsx` (new) | render + expand/collapse; selection from the URL |
| groups | `app/(app)/groups/page.tsx` (new), `components/groups/GroupsPanel.tsx`, `GroupFormDialog.tsx`, `DeleteGroupDialog.tsx` (new) | screen 14 |
| assignment | `components/tasks/TaskForm.tsx`, `components/annotationRecords/RecordForm.tsx` (extend) | the group selector |
| styling | `src/styles/adf-fusion.overrides.css` (extend) | `nb-*` classes for anything `af-*` lacks |

## Alternatives rejected

- **Asking the API to nest `group → type`.** Rejected at OQ-23: the payload is a bipartite relation
  and nesting is presentation (AD-06). It would also have meant reopening a merged contract.
- **Re-sorting the tree client-side.** Rejected: OQ-25 is the API's contract, and enforcing an
  ordering in two places is how they diverge. The view model preserves payload order.
- **Computing counts in the client** from the filtered listings. Rejected at OQ-27 — one round trip
  per node on first paint. feat-016 exists precisely so this is unnecessary.
- **A `<GroupProvider>` context holding the tree.** Rejected: the layout already gives one mount
  point; a context adds a second place for the tree's freshness to be wrong, for no reuse today.
- **Making `groupId` optional (`groupId?: string | null`).** Rejected — this is the load-bearing
  decision. An optional key lets a caller omit it and silently clear the group, which is exactly
  feat-014's hazard. Required-and-nullable makes omission a compile error.
- **Rendering the Navigator per page** rather than in the layout. Rejected: it refetches on every
  navigation and loses expand/collapse state on each click.
- **Implementing the `af-selectManyShuttle` bulk assignment** from design 14 — deferred at spec, and
  the design's own README now records why (no bulk API surface).

## Reversibility

**One-way:**
- **`groupId: string | null` (required) in the mirrored input types.** The compile-error guarantee is
  the rollout fix; loosening it later silently reopens the hazard.
- **The URL shape** `?group={uuid|none}` — shareable links and the API's own filter vocabulary.
- **The Navigator living in `app/(app)/layout.tsx`** — every protected route inherits it.

**Reversible:** the split between `Navigator`/`TreeNode`; whether `formatAverageStatus` returns a
node or a string; `nb-*` class names; expand/collapse defaults.

## Blast radius (satellite `notebox-web`)

**New (11):** `lib/api/groupsClient.ts`, `lib/api/navigationClient.ts`,
`lib/navigation/viewModel.ts`, `lib/groups/format.ts`, `components/navigation/Navigator.tsx`,
`components/navigation/TreeNode.tsx`, `components/groups/GroupsPanel.tsx`,
`GroupFormDialog.tsx`, `DeleteGroupDialog.tsx`, `app/(app)/groups/page.tsx`, plus their tests.

**Changed (9):** `lib/api/types.ts`, `lib/api/tasksClient.ts`, `lib/api/annotationRecordsClient.ts`,
`lib/tasks/viewModel.ts`, `lib/annotationRecords/viewModel.ts`, `app/(app)/layout.tsx`,
`components/tasks/TaskForm.tsx`, `components/annotationRecords/RecordForm.tsx`,
`lib/i18n/messages/{en,pt}.ts`, `src/styles/adf-fusion.overrides.css`.

**Consumers:** none — `notebox-web` is a leaf. **Not touched:** the rich-text stack, the image
upload path, secret reveal, auth/`RouteGuard`, `adf-fusion.css` (never edited).

## Risk

1. **Widening the input types breaks every construction site at once.** Making `groupId` required
   turns ~40 existing object literals in tests and components into compile errors. *Signal:*
   `npm run typecheck` floods on the first task. *Mitigation:* that is the point — it is a
   guided checklist, and it is why the echo builders are extended **first**, so most sites inherit
   the field instead of being edited.
2. **Rendering `averageStatus: null` as `0%`.** The exact misreading the contract warns about.
   *Signal:* the "no tasks" test. *Mitigation:* one `formatAverageStatus` helper; no `?? 0` in JSX.
3. **The Navigator refetching on every navigation.** If mounted per page or keyed wrongly it
   refetches and drops expand state. *Signal:* the "fetched once per view" scenario counting
   requests via MSW. *Mitigation:* layout mount.
4. **Selection state desynchronising from the URL.** Two sources of truth for "which node is
   current". *Signal:* the shareable-URL scenario. *Mitigation:* selection is *derived* from the
   route params; the tree holds no selection state.
5. **Inverting to `group → type` losing a type that occupies several groups.** A naive
   `Map<groupId, type>` overwrites. *Signal:* the "a type appearing under several groups" scenario.
   *Mitigation:* the inversion accumulates into a list, and it is unit-tested as a pure function.
6. **The satellite is 1 commit ahead of its origin** (`2ee5e3f`, the design amendment) — unrelated
   to this plan, but it will ride the feature branch unless pushed first. Named so it is not a
   surprise at `publish`.

## Test plan (26 scenarios → ~10 files)

| Area | Tests | Scenarios |
|---|---|---|
| `lib/navigation/viewModel.test.ts` | inversion, multi-group types, Ungrouped last, order preserved, count sums | 6 |
| `components/navigation/Navigator.test.tsx` | render, empty roots, single fetch, error state | 4 |
| routing (integration) | node → filtered grid, ungrouped filter, shareable URL, selection | 4 |
| `components/groups/*.test.tsx` | two panels, create per domain, duplicate name, rename, delete dialog wording | 6 |
| form assignment | selector scoped to domain, assign, clear sends null, untouched survives edit | 4 |
| aggregates | counts shown as received, null-vs-0% | 2 |

Locale coverage rides the existing catalog-completeness check; the `pt` end-to-end scenario is
asserted in the groups screen test, matching feat-013's precedent.
