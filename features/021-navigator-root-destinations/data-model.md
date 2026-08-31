# Data model — feat-021

**No persistent model, no migration, no DTO.** This feature adds no entity and changes no payload:
the navigation tree, its counts and the records page all keep the shapes feat-014/015/020 defined.

What it does touch is **client state**, and that is where its invariants live.

## Client state

| Structure | Where | Meaning |
|---|---|---|
| `open: Set<typeId>` | annotation types list | which rows are showing a band (feat-020) |
| `bands: Map<typeId, BandState>` | annotation types list | what has been fetched — the cache; survives collapse (feat-020) |
| **`requested: Ref<Set<typeId>>`** | annotation types list | **new.** Which types a request has already been *started* for. A ref, not state: it mutates synchronously, so it is correct even when two toggles land in one tick. |

The tree holds **no** new state: the roots' "current page" is derived from the URL, never stored.

## Invariants

| # | Invariant | Source | How it is proved |
|---|---|---|---|
| INV-N1 | A root node's `aria-current` is true exactly when the browser is on that root's destination | spec (W-7 precedent) | assertions on the list route and on a detail route beneath it |
| INV-N2 | Activating a twisty never navigates; activating a label never collapses | spec R1 | one scenario each way |
| INV-B5′ | At most one records request per type per mount — **including** toggles delivered in the same tick | feat-020 INV-B5, tightened by OQ-34 | request count across: two toggles in one tick; and open → collapse → reopen |
| INV-N3 | `requested` only ever grows within a mount, and never shrinks on collapse | this plan | the reopen-from-cache assertion (a shrinking set would refetch) |

**Why `requested` and `bands` are both needed:** `bands` is empty for a type whose request is in
flight — exactly the window a double toggle exploits. A guard reading `bands` would therefore
still fire twice, which is the defect being closed.
