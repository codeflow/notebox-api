# Data model — feat-020

**No persistent model.** This feature stores nothing, migrates nothing, and adds no entity. It is
a second reader of data feat-005/008 already own. This file exists because the lifecycle asks for
it, and saying *"none, and here is why"* is worth more than an empty heading.

## Client-side state

Held by `AnnotationTypeList`, lost on unmount by design — an expanded band is a glance, not a
preference worth persisting.

```ts
type BandState =
  | { status: 'loading' }
  | { status: 'ready'; type: AnnotationTypeDto; page: PageDto<AnnotationRecordDto> }
  | { status: 'failed'; message: string };

const [bands, setBands] = useState<ReadonlyMap<string, BandState>>(new Map());
const [open, setOpen] = useState<ReadonlySet<string>>(new Set());
```

### Why two structures and not one

`bands` answers *"what has been loaded"*; `open` answers *"what is showing"*. Collapsing drops
from `open` and **keeps** `bands` — that is the cache, and it is what makes spec S7 (reopening
issues no request) true. Folding them into one would make collapse either lose the cache or
leave the band visible.

## Invariants

| # | Invariant | Bound to | How it is checked |
|---|---|---|---|
| INV-B1 | A band's columns are exactly its own type's `visibleForViewing` fields | BR-09 | The two-types scenario: each band shows its own columns |
| INV-B2 | Every value renders through `RecordGridCell` | C-08, C-12 | Secret field masked, no reveal request; hostile markup renders as plain text |
| INV-B3 | The footer names `PageDto.total`, never `items.length` | OQ-32 | 47-records scenario, with 10 rendered |
| INV-B4 | `open ⊆ bands ∪ {loading}` — nothing is shown that was never fetched | — | Type invariant; a band renders only from its `BandState` |
| INV-B5 | Opening fetches once per type per mount | spec S6/S7 | Request count asserted across open → collapse → reopen |

## What this feature does not touch

`AnnotationRecord`, `AnnotationType`, `TypeField`, their tables, and every rule about them
(BR-03 value conformance, BR-05 deletion, BR-10 secret encryption) stay exactly where they are.
The band reads; nothing here writes.
