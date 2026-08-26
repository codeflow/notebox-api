# Data model — Groups and the Navigator (notebox-web)

**ID:** features/015-groups-navigation-notebox-web · **Companion of:** `plan.md` · **Date:** 2026-08-26
**Wire truth:** `features/014-.../contracts/groups-navigation.md` + `features/016-group-counts/contracts/group-counts.md` — **mirrored, never re-derived.**
**No persistence lives in the satellite.** This is the client's data model: the wire mirrors, the
view model the UI actually renders, and the invariants that keep them honest.

## Wire mirrors — append to `lib/api/types.ts`

```ts
export type GroupDomain = 'ANNOTATION' | 'TASK';

/**
 * A group as served. The aggregate fields are domain-shaped (feat-016): an annotation group carries
 * typesUsed and never averageStatus; a task group the reverse.
 *
 * averageStatus === null means the group has NO tasks. 0 means it has tasks and they are all at 0%.
 * These are different. Rendering null as 0% reports an empty group as a stalled one.
 */
export interface GroupDto {
  id: string;
  name: string;
  domain: GroupDomain;
  itemCount: number;
  typesUsed: number | null;
  averageStatus: number | null;
  createdAt: string;
  updatedAt: string;
}

/** Create/replace body. On replace the domain must equal the stored one — the API rejects a change. */
export interface GroupInput {
  name: string;
  domain: GroupDomain;
}

/** A group node. groupId === null is the synthetic Ungrouped node; name is null with it. */
export interface NavigationGroupNodeDto {
  groupId: string | null;
  name: string | null;
  count: number;
}

export interface NavigationTypeNodeDto {
  typeId: string;
  name: string;
  groups: NavigationGroupNodeDto[];
}

/** Two roots. The tree stops at group nodes — no record or task is ever enumerated (OQ-23). */
export interface NavigationTreeDto {
  annotations: NavigationTypeNodeDto[];
  tasks: NavigationGroupNodeDto[];
}
```

### The four item types gain `groupId` — **required, nullable**

```ts
export interface TaskInput               { …; groupId: string | null; }
export interface AnnotationRecordInput   { …; groupId: string | null; }
export interface TaskDto                 { …; groupId: string | null; }
export interface TaskListItemDto         { …; groupId: string | null; }
export interface AnnotationRecordDto     { …; groupId: string | null; }
```

> **`groupId: string | null`, never `groupId?: string | null`.** The API is replace-not-patch: an
> omitted key **clears the group**. Optional would let a caller forget it and silently un-group an
> item — feat-014's recorded rollout hazard. Required-and-nullable makes forgetting a **compile
> error**, which is the only version of this guarantee a reviewer does not have to enforce by hand.

## View model — what the UI renders (`lib/navigation/viewModel.ts`)

The payload nests `type → groups`; design screen 05 nests `group → types` (OQ-23 as clarified). One
pure function inverts it.

```ts
/** A type sitting under a group, with the count of ITS records in THAT group (from the API). */
export interface NavigatorTypeNode { typeId: string; name: string; count: number; }

/**
 * A group node in the rendered tree. groupId === null is Ungrouped — `name` is null on the wire and
 * the client supplies the localized label (AD-05/AD-06), never the payload.
 * `count` is the sum of its types' counts: arithmetic on API numbers, not a second source of truth.
 */
export interface NavigatorGroupNode {
  groupId: string | null;
  name: string | null;
  count: number;
  types: NavigatorTypeNode[];   // empty on the Tasks root — task groups have no children
}

export interface NavigatorTree {
  annotations: NavigatorGroupNode[];
  tasks: NavigatorGroupNode[];
}

export function toNavigatorTree(payload: NavigationTreeDto): NavigatorTree;
```

## Invariants

| # | Invariant | Enforced by | Cites |
|---|---|---|---|
| **W-1** | Every item update sends `groupId` explicitly, including `null` | required-nullable in the input types + the echo builders | feat-014 hazard, OQ-04 |
| **W-2** | A type occupying several groups appears under **each** | the inversion accumulates into a list, never a `Map<groupId, type>` | design 05 |
| **W-3** | Sibling order is the payload's; the client never re-sorts | inversion preserves first-appearance order | **OQ-25** |
| **W-4** | `Ungrouped` is `groupId === null` and is labelled from the client's catalog | `nav.ungrouped` in `en`/`pt`; the payload's null `name` is never rendered | AD-05, AD-06, C-09 |
| **W-5** | A group node's `count` equals the sum of its types' counts | computed in the pure inversion, unit-tested | OQ-27 |
| **W-6** | `averageStatus === null` renders as "no tasks", **never** `0%` | one `formatAverageStatus`; no `?? 0` in JSX | feat-016 contract |
| **W-7** | The active filter lives in the URL, and selection derives from it | route params, no selection state | C30, spec |
| **W-8** | A group's domain comes from its panel, never a control | `GroupsPanel` prop | OQ-04 |
| **W-9** | Counts are consumed, never derived from a listing call | clients return them; no per-node request | **OQ-27** |

## What this feature does **not** model

- **No local persistence** — no cache, no localStorage, no optimistic state. The tree refetches.
- **No client-side `total`** — paging facts stay the API's (NFR-08).
- **No group hierarchy type.** There is no `parentId` anywhere in these shapes; nesting is
  unrepresentable in the client model exactly as it is on the server (OQ-04).
