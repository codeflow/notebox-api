# Plan — Records sub-grid in the types list

**ID:** features/020-records-subgrid · **Spec:** `spec.md` v1 (approved 2026-08-30)
**Project:** notebox-web · **Stack:** Next.js 15 App Router, React 19, TypeScript strict, Vitest + Testing Library + MSW

## Origin

Spec `020-records-subgrid/spec.md`, US-2.2, FR-05 / BR-09, from OQ-32 (resolved).

---

## The finding that shapes this plan

The spec assumes expanding a row costs **one** request. It costs **two**, and the spec is wrong
about it — recorded here rather than quietly worked around.

`GET /annotation-types` returns `AnnotationTypeListItemDto`:

```ts
{ id, name, iconImageId, fieldCount, visibleFieldCount, recordCount, createdAt }
```

It carries **counts, not fields**. The band's columns *are* the type's visible fields, so opening
a row needs the fields too — `GET /annotation-types/:id`, which returns `AnnotationTypeDto` with
`fields: FieldDto[]`, and it is also `FieldDto` that carries `secret` and `fieldType`, the two
properties C-12 and C-08 hang on.

**Consequence for the spec:** scenario *"exactly one records request is issued"* must become
*"one type request and one records request, both for that type"*. Flagged for the spec amendment
at the end of this plan — not silently reinterpreted.

**Rejected:** widening the list endpoint to embed fields per row. It would make the common case
(list rendered, nothing expanded) pay for the rare one, and the spec puts *"no API change"* in
scope-out. If the two requests ever prove too slow, that is a measured argument for a future
change, not a guess now.

---

## Approach

A new component, `TypeRecordsBand`, rendered as a `<tr class="band">` beneath its type's row,
and a disclosure column added to `AnnotationTypeList`.

**What is reused, and why exactly it and not more.**

| Reused | Not reused | Reason |
|---|---|---|
| `RecordGridCell` | `RecordsGrid` | `RecordGridCell` is the single place where `field.secret` becomes a mask and `FREE_TEXT` becomes `plainTextPreview`. That is the C-08 / C-12 seam, and reusing it is what makes R1 and R2 go away. `RecordsGrid` is a *screen*: QBE filter row, page sizes 50/100/200, pagination, group column, row actions, toolbar — every one of them scope-out per the spec. Bending it into a band would mean five "hide this" props and a component that serves two masters. |
| `ExpandIcon` / `CollapseIcon` | a new twisty | Drawn this session for the navigator, in the same detailStamp idiom, at their final 9px so the 1px border lands on the pixel grid. |
| `annotationRecordsClient.list(typeId, page, size, group)` | a new client method | Already takes exactly the arguments needed: `list(id, 0, 10, null)`. |
| `annotationTypesClient.get(id)` | — | Supplies the fields the list row does not carry. |
| `.af-table tbody tr.band` | a new band style | The theme already defines the banded row. |

**State.** `AnnotationTypeList` holds one `Map<typeId, BandState>`. Several rows open at once
(OQ-32), which the navigator's `ReadonlySet` precedent already establishes for the tree. A `Map`
rather than a `Set` because a band caches what it loaded — reopening must not refetch (spec S7).

```ts
type BandState =
  | { status: 'loading' }
  | { status: 'ready'; type: AnnotationTypeDto; page: PageDto<AnnotationRecordDto> }
  | { status: 'failed'; message: string };
```

Absence from the map means collapsed. Collapsing **keeps** the entry (that is the cache); a
separate `open: Set<typeId>` carries visibility. Two structures because "loaded" and "showing"
are genuinely different questions, and conflating them is what would make S7 fail.

**Request.** On first open only: `Promise.all([typesClient.get(id), recordsClient.list(id, 0, 10, null)])`.
`size=10` is a literal from OQ-32 — NFR-08's 50 is respected by asking for less, never excepted.

**The footer line.** `total` comes from `PageDto.total`, never from `items.length`. That is the
entire point of the line: it says what is *not* shown.

---

## Alternatives rejected

1. **Render `RecordsGrid` inside the band.** Rejected above — it is a screen, not a cell renderer,
   and disabling five of its parts by prop is how a component becomes unmaintainable for both
   callers.
2. **Fetch on list load, so opening is instant.** Twelve types would mean 24 requests for a page
   where the member may open none. Spec S6 pins the opposite behaviour.
3. **A uniform preview (name + date) with columns shared across types.** This was the
   recommendation and the product owner declined it (OQ-32). Recorded so the audit does not
   rediscover it as a finding.
4. **Row actions inside the band.** Scope-out. It would also make the band a *third* place that
   can delete a record, and BR-05's confirmation path would need proving there too.

---

## Blast radius

| File | Change | Risk |
|---|---|---|
| `components/annotationTypes/AnnotationTypeList.tsx` | disclosure column; band rows; state | **R4** — the column set was measured to a settled layout this session (labels at 433, controls at 441 on the sibling screens; this grid's own widths). The new column is `width: 28px` like the navigator's `.tw` lane, asserted by test. |
| `components/annotationTypes/TypeRecordsBand.tsx` | new | — |
| `lib/i18n/messages/{en,pt}.ts` | 4 keys | C-09; the existing keyset coverage test already fails on a key present in one locale only. |
| `src/styles/adf-fusion.overrides.css` | band rules | The `classCoverage` guard added this session fails if a class is written with no rule. |
| `components/annotationTypes/AnnotationTypeList.test.tsx` | scenarios | — |

**Consumers unaffected:** `RecordsGrid`, `RecordGridCell`, both clients and every other screen —
this feature adds a caller, it changes no existing signature.

---

## Reversibility

| Decision | Kind | Note |
|---|---|---|
| Reuse `RecordGridCell` rather than `RecordsGrid` | **one-way in practice** | Once the band renders cells itself, C-08 and C-12 would need proving in two places forever. The whole compliance argument rests here. |
| Two requests per open, no endpoint change | **reversible** | If measured too slow, embed fields in the list DTO later; nothing here depends on the current shape. |
| `Map` cache + `Set` open, cache survives collapse | reversible | Internal to one component. |
| `size = 10` | reversible | One constant. |
| Band shows no row actions | **one-way-ish** | Adding them later drags BR-05's confirmation path into the band. Deliberately deferred. |

---

## Risk, and the signal that reveals it

| # | Risk | Signal |
|---|---|---|
| R1 | The band renders values itself, bypassing `RecordGridCell` | A test rendering a **Secret visible field** inside a band asserts the mask and asserts **no reveal request** is issued. Mutation-probe: render `value.text` directly, the test must fail. |
| R2 | `FREE_TEXT` reaching the band as HTML | A hostile-markup fixture in a band asserts the plain-text preview, no element from the markup. |
| R3 | An open band closing on a parent re-render | Reload the list with an open band; the band survives. |
| R4 | The disclosure column disturbing existing widths | Assert the other columns' widths are unchanged with the column present. |
| R5 | Two bands sharing one fetch or one cache slot | The two-types scenario asserts each band's own columns — the failure mode is both showing the first type's. |
| R6 | The 401 path swallowed by the band's own error state | The session-expired scenario asserts the app-wide path is taken, not a band-local message. |

---

## Contracts

No new API contract. `contracts/` records the two existing calls this feature consumes, so the
implementer and the auditor read the same thing: `contracts/consumed.md`.

---

## Spec amendment requested

`spec.md` scenario *"The records are fetched when the row opens, not when the list loads"* says
**one** records request. It must read: **one type request and one records request**. The rest of
the scenario stands. Requested here rather than edited silently — the spec is approved, and this
plan does not get to reinterpret it.
