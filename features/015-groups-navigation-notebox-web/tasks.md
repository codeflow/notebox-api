# Tasks — Item groups and navigation-tree data (web)

**ID:** features/015-groups-navigation-notebox-web · **US:** US-3.1 · **Date:** 2026-08-26
**Project:** `notebox-web` — routed satellite. **Verify:** `npm run verify` (clean → lint →
typecheck → test → build), run **in the satellite**. The hub's `mvn -B verify` proves nothing here.
**Source:** plan.md + data-model.md (approved 2026-08-26), against
`features/014-.../contracts/groups-navigation.md` + `features/016-group-counts/contracts/group-counts.md`
**Status:** Approved (human approval 2026-08-26)

> Six tasks, strictly sequential. T-03/T-04 and T-05 both touch `lib/i18n/messages/{en,pt}.ts`, and
> T-06 edits forms whose builders T-01 changes, so no `parallel: yes` anywhere → no worktree
> isolation. Every task ships its tests and ends **green**.
>
> **T-01 is deliberately the widest task, not the smallest.** Making `groupId` required-nullable
> breaks every construction site in the repo at once, and a task must end green — so the type
> widening, the echo builders and the call-site fixes have to land together. Splitting them would
> leave a red tree between tasks, which the loop forbids. This is feat-013's T-01 shape.

- [ ] **T-01 · Wire foundation — mirrors, `groupId` through the echo builders, both new clients**
      - files: `lib/api/types.ts` (Group/Navigation mirrors + `groupId: string | null` **required** on `TaskInput`, `TaskDto`, `TaskListItemDto`, `AnnotationRecordInput`, `AnnotationRecordDto`), `lib/api/groupsClient.ts` + `navigationClient.ts` (new, + tests), `lib/api/tasksClient.ts` + `annotationRecordsClient.ts` (optional `group` param on `list`), `lib/tasks/viewModel.ts` + `lib/annotationRecords/viewModel.ts` (`groupId` in draft + input builders), every construction site the widening breaks, MSW fixtures
      - covers: **W-1** (the rollout fix), W-9 · scenarios: "Clearing the group sends null, never an omitted key", "An untouched group survives an unrelated edit" — both at the builder level, where the guarantee actually lives
      - depends: — · parallel: no
      - verify: `npm run verify` — **`typecheck` is the acceptance criterion here**, not an afterthought: with `groupId` required, every site that forgets it is a compile error, and green means none remained. Assert the builders echo `groupId` on a partial edit (the feat-012 hazard, one layer down) and that `toInput`/`toTaskInput` emit the key with `null` rather than omitting it.

- [ ] **T-02 · The inversion — `toNavigatorTree` and `formatAverageStatus`, pure and unit-tested**
      - files: `lib/navigation/viewModel.ts` (new), `lib/groups/format.ts` (new), + tests
      - covers: **W-2/W-3/W-5/W-6**, OQ-23 as clarified, OQ-25, OQ-27 · scenarios: "Groups sit above the types that occupy them", "A type occupying two groups appears under each", "Sibling order comes from the payload and is not re-sorted", "The Ungrouped node is labelled by the client, not the payload" (shape half), "Tree nodes show their counts"
      - depends: T-01 · parallel: no
      - verify: `npm run verify` — these are pure functions, so the whole OQ-23 inversion is testable with **no rendering at all**. Three things must fail if broken: a type in two groups appearing under **both** (a `Map<groupId, type>` would drop one); the payload's order **preserved**, not re-sorted (feed it deliberately non-alphabetical input and assert it comes back unchanged); and a group node's count equal to the **sum** of its types'. `formatAverageStatus(null)` must not return `0%`.

- [ ] **T-03 · The Navigator in the layout — render, one fetch, empty and error states**
      - files: `components/navigation/Navigator.tsx` + `TreeNode.tsx` (new, + tests), `app/(app)/layout.tsx` (mount the left panel), `lib/i18n/messages/{en,pt}.ts` (+ `nav.ungrouped` and the tree's strings), `src/styles/adf-fusion.overrides.css` (`nb-*` for what `af-*` lacks)
      - covers: FR-09/C28, **W-4**, C-02 · scenarios: "The Ungrouped node is labelled by the client" (en **and** pt), "A tenant with nothing sees both roots and no children", "The tree is fetched once per view, not per node", "A failed tree load degrades without breaking the workspace", "The Navigator and Groups screen are behind the session guard"
      - depends: T-02 · parallel: no
      - verify: `npm run verify` — the single-fetch scenario is asserted by **counting MSW requests**, not by inspecting the component; mounting in the layout is what makes it true and a per-page mount would fail it. The error state must leave the rest of the page rendered, so assert both: the localized error **and** the main panel still present.

- [ ] **T-04 · Click-to-detail — the group filter in the URL, selection derived from it**
      - files: `app/(app)/tasks/page.tsx`, `app/(app)/annotation-types/[id]/records/page.tsx` (read `?group=`, pass to the clients), `components/navigation/TreeNode.tsx` (hrefs), + integration tests
      - covers: FR-09 → C30, **W-7** · scenarios: "A type under a group opens that type's records filtered to the group", "A task group node opens the tasks grid filtered to it", "An Ungrouped node filters to the ungrouped, not to everything", "The filtered view is shareable"
      - depends: T-03 · parallel: no
      - verify: `npm run verify` — assert the **request carried the group filter**, not merely that the page navigated; the ungrouped case must send `none` and must **not** list grouped items. Shareability is asserted by rendering the route fresh from its URL and finding the same filtered grid with the same node selected — which only holds because selection is derived from the route, not from a click handler.

- [ ] **T-05 · Groups management — two panels, CRUD dialogs, the aggregates as received**
      - files: `app/(app)/groups/page.tsx` (new), `components/groups/GroupsPanel.tsx` + `GroupFormDialog.tsx` + `DeleteGroupDialog.tsx` (new, + tests), `lib/i18n/messages/{en,pt}.ts` (the screen's strings), `src/styles/adf-fusion.overrides.css`
      - covers: FR-08 management, **W-6/W-8**, C-09 · scenarios: "The screen shows one panel per namespace", "Creating a group takes its domain from the panel, not from a control", "The same name is accepted in the other namespace", "A duplicate name in the same namespace shows the server's message", "Renaming a group updates it in place", "Deleting confirms, and says what actually happens", "Confirming the delete removes only the group", "The group tables show their per-domain aggregates", "Every new string resolves in both locales"
      - depends: T-04 · parallel: no
      - verify: `npm run verify` — three that must be asserted rather than assumed: **no domain control exists** on the create dialog (assert its absence, since a picker would make the API's cross-domain rejection reachable); the delete dialog **says the items survive** (OQ-24 — wording is the deliverable, so assert the text); and **`averageStatus: null` renders as "no tasks", never `0%`** — the one misreading the contract warns about.

- [ ] **T-06 · Assignment on both forms + the page-level integration pass**
      - files: `components/tasks/TaskForm.tsx`, `components/annotationRecords/RecordForm.tsx` (the group selector), + their tests, + the integration suites for both domains
      - covers: FR-08 assignment at the wire, **W-1** end to end · scenarios: "The selector offers only the item's own namespace", "Assigning a group and saving", "Clearing the group sends null, never an omitted key" (wire half), "An untouched group survives an unrelated edit" (wire half)
      - depends: T-05 · parallel: no
      - verify: `npm run verify` — the selector must **not** offer the other domain's groups (assert absence). The two hazard scenarios are asserted **on the request body** here, not on component state: clearing sends `groupId: null`, and an unrelated edit still carries the original id. T-01 proves the builder; this proves the form actually uses it.

## Coverage check

All **26** spec scenarios are claimed:

| Spec `Feature:` block | Scenarios | Task |
|---|---|---|
| FR-09 Navigator renders group → type | 6 | T-02 (shape) + T-03 (render) |
| FR-09 Clicking a node | 4 | T-04 |
| FR-08 Managing groups | 7 | T-05 |
| FR-08 Assigning an item | 4 | T-01 (builder) + T-06 (wire) |
| OQ-27 Aggregates displayed as received | 2 | T-02/T-03 (tree) + T-05 (tables) |
| Standing guarantees | 3 | T-03 (guard, error) + T-05 (locales) |

**No scenario is uncovered**, and no task cites zero scenarios.

Obligations from the plan with no scenario behind them, called out so the audit does not read them
as invention: **`groupId` required-not-optional** (plan Reversibility — the compile-error guarantee),
the **layout mount point** (plan Approach), and **`nb-*` classes only, never editing
`adf-fusion.css`** (the satellite's standing rule).

**Pre-`publish` note:** the satellite is 1 commit ahead of its origin (`2ee5e3f`, the design
amendment). It is unrelated to these tasks but will ride the feature branch — plan Risk 6.
