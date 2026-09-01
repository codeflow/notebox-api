# Data model — feat-023

**No persistent model, no migration, no DTO.** Every entity this feature touches already exists;
it changes where they are edited, not what they are. What is new is **client state**, and that is
where the invariants live.

## Client state

| Structure | Owner | Meaning |
|---|---|---|
| `panel: PanelContent \| null` | `PanelHost` | what the panel is showing; `null` = closed. A **discriminated union** (`record`, `record-edit`, `task-new`, `groups`, `members`, `catalog`), never a rendered node — so a caller cannot supply its own variant of a screen |
| `width: 'normal' \| 'expanded'` | `PanelHost` | [A-5]; kept in `sessionStorage` so it survives navigation but never becomes a stored preference nobody asked for |
| `dirty: boolean` | each form inside the panel | whether anything was typed. **Owned by the form**, not computed by diffing drafts — a diff invites false positives that would nag on every close |
| `editingRowId: string \| null` | `useRowEditor` | which row is in edit; at most one per grid |
| `draft: Partial<Record>` | `useRowEditor` | the values being typed, discarded on cancel, sent once on confirm |

## Invariants

| # | Invariant | Source | How it is proved |
|---|---|---|---|
| INV-P1 | At most one panel content is shown at a time | plan §1 | the union has no "stack" |
| INV-P2 | The same `PanelContent` value renders identically regardless of which caller opened it | spec R3 | one test opens from menu, grid and empty state and compares |
| INV-P3 | Closing with `dirty === true` always asks first; with `false`, never | [A-6] | both directions asserted |
| INV-P4 | A confirmation REPLACES panel content — the panel never stacks an overlay | [A-6] | asserted on the Groups delete |
| INV-E1 | At most one row per grid is in edit | plan §4 | entering edit on a second row leaves the first |
| INV-E2 | Confirming issues **exactly one** update for the row | [A-2] | request count |
| INV-E3 | A rejected save leaves the row in edit with the member's values, and writes nothing | [A-2] | asserted on both halves |
| **INV-E4** | **A Secret field has no editable control in any grid, and no grid ever calls `reveal`** | **[A-4] / C-12** | a Secret visible field rendered in edit mode asserts **no input exists** and `annotationRecordsClient.reveal` was not called |
| INV-R1 | Every route that exists today still renders | plan §Blast radius | each kept route is hit directly |

> **INV-E4 is the one that is not a preference.** The others describe behaviour the product owner
> may want changed; this one is C-12, and loosening it needs a compliance argument.
