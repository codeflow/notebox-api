# Plan — Records grid and detail projection (notebox-web)

## Origin
Spec: [spec.md](spec.md) (approved 2026-08-12) · US-2.2 · FR-05 · NFR-08 · BR-09. Consumes feat-008's
frozen contract (`PageDto`, filtered `AnnotationRecordDto` rows, newest-first). Satellite stack:
react/next, vitest + Testing-Library + MSW; visual truth: design screen 11.

## Approach (three sentences)
Extend `annotationRecordsClient` with `list(typeId, page, size) → PageDto` (the only new wire call),
and render it in a new `RecordsGrid` component following the `AnnotationTypeList` `af-table` idiom —
headers from Name + the type's visible fields, cells dispatched per field type by a `RecordGridCell`
that renders LIST badges, masked dots, compact thumbnails and **text-node** rich previews (a
`plainTextPreview` helper strips markup via DOM parsing and truncates — never `innerHTML` in a cell).
Grid state is three values (`page`, `size`, `filter`): page/size drive refetches, the QBE filter is a
pure client-side predicate over the loaded page with the honesty note, and the `af-statusBar` pager
displays only `PageDto`'s facts. The route `annotation-types/[id]/records/page.tsx` composes it with
the loading/empty/not-found states; the type detail gains "View records"; the record detail's
post-delete navigation retargets to the grid.

## Design

### 1. Client — `lib/api/annotationRecordsClient.ts` (+`lib/api/types.ts`)
`PageDto<T> { items, page, size, total }` mirrored in `types.ts`;
`list(typeId: string, page: number, size: number): Promise<PageDto<AnnotationRecordDto>>` via
`authFetch('/annotation-records?typeId=…&page=…&size=…')`. MSW tests: URL shape, bearer, no tenant
param, envelope parsing.

### 2. Cell rendering — `components/annotationRecords/RecordGridCell.tsx` + `lib/annotationRecords/preview.ts`
`plainTextPreview(html, max = 120)`: `DOMParser` → `textContent`, collapse whitespace, truncate with
ellipsis — returns a **string**, so the cell is a text node by construction (the spec's
never-HTML-in-a-cell pin). `RecordGridCell({ field, value })` dispatch: TEXT/NUMBER text ·
LIST/choices → labels, LIST with `nb-badge` + `data-colour` · secret → `••••••••` (reuse the
`annotationRecords.secret.masked` key; **no** `RevealableValue` import) · IMAGE →
`IconThumbnail`-style object URL (reuse `imagesClient.fetchObjectUrl`, revoke on unmount) ·
FREE_TEXT → `plainTextPreview(value.text)`.

### 3. Grid — `components/annotationRecords/RecordsGrid.tsx`
Props `{ type, onOpenRecord }`. Internal state `{ page, size, filter }`; `useEffect` refetch on
page/size; rows rendered as served (no client sort). Structure: QBE row (one input per text-like
column + name, page-local predicate, `annotationRecords.grid.filterNote` visible while active) →
`af-table` in a horizontal-scroll wrapper (`nb-gridScroll`) → `af-statusBar` pager (position `page+1`
of `ceil(total/size)`, total, prev/next disabled at bounds, size select **50/100/200** — the
contract's bound). Empty page + no filter → `annotationRecords.grid.empty` + New record action.

### 4. Route & navigation
- `app/(app)/annotation-types/[id]/records/page.tsx`: loads the type (404 → the established
  not-found idiom), renders `RecordsGrid`, `onOpenRecord` → the existing detail route.
- Type detail (`[id]/page.tsx`): "View records" button beside "New record".
- Record detail (`[recordId]/page.tsx`): post-delete `router.push` retargets to `…/records` (the
  declared feat-006 navigation change — one line).

### 5. i18n + CSS
Keys (en+pt): `annotationRecords.grid.viewRecords`, `.grid.empty`, `.grid.filterNote`,
`.grid.filterPlaceholder`, `.grid.pagerOf`, `.grid.pageSize`, `.grid.next`, `.grid.previous`,
`.grid.nameColumn`. CSS: `nb-gridScroll` (overflow-x), pager/QBE rows in `adf-fusion.overrides.css`
per screen 11 — theme sheet untouched.

### 6. Tests
Client (envelope/URL/auth) · `preview` unit (markup stripped, truncation, no entities leak) ·
`RecordGridCell` per-type (badge colour attr, mask, thumbnail via mocked imagesClient, preview is
text — assert no element children from the value) · `RecordsGrid` (column order, served order kept,
pager facts + page-2 fetch asserted via MSW, size bound, QBE filters/clears with **zero** requests,
empty state) · route integration (load, not-found, pt locale, bearer/no-tenant, view-records button,
post-delete target) — the `annotationRecords.integration` idiom.

## Alternatives rejected
- **Server round-trip per QBE keystroke** — feat-008 ships no filter params; inventing them
  client-side would 400. Page-local filtering with the on-screen note is the honest fit until a
  server-filter feature exists.
- **Rendering rich cells with `RichTextValue`** — sanitized HTML in cells is safe but wrong-shaped
  (block content in a grid row) and costs highlight/imagery work per cell; the text preview is what
  screen 11's density implies.
- **A generic DataGrid abstraction** — one consumer today; `AnnotationTypeList` and this grid can
  converge later if a third grid appears (rule-of-three).
- **Client-side sorting toggles** — the spec pins served order; user sorting is future work.

## Blast radius
- **New:** `RecordsGrid.tsx`, `RecordGridCell.tsx`, `lib/annotationRecords/preview.ts`,
  `records/page.tsx` (grid route), ~9 i18n keys ×2, `nb-grid*` CSS, tests for each.
- **Modified:** `lib/api/types.ts` (+`PageDto`), `lib/api/annotationRecordsClient.ts` (+`list`),
  `annotation-types/[id]/page.tsx` (+View records), `records/[recordId]/page.tsx` (post-delete
  target), `annotationRecords.integration.test.tsx`, MSW handlers.
- **Test callers of changed signatures:** none — all additions are additive (the L-02 rule: no
  service-layer equivalent here; the client's `list` is new surface).
- **Consumers:** none downstream; this is the leaf of US-2.2.
- **Untouched:** the detail/editor components, `RevealableValue`, the theme sheet, the API.

## Risk
- **Preview helper leaking entities or markup** (`&amp;` shown raw, or worse). *Signal:* unit cases
  with entities + hostile markup asserting exact output strings.
- **Pager arithmetic at boundaries** (total 0, exact multiple, last page). *Signal:* dedicated
  pager-facts tests at those totals.
- **Thumbnail object-URL leaks across page changes.** *Signal:* revoke-on-unmount test (the
  feat-006 idiom reused).

## Reversibility
| Decision | Kind |
|---|---|
| QBE page-local (with on-screen note) | reversible (a server-filter feature can supersede) |
| Preview-as-text for rich cells | reversible |
| Post-delete navigation → grid | reversible |
| All component/file shapes | reversible |
