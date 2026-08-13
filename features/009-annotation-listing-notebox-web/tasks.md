# Tasks — Records grid (feat-009, US-2.2 web half)

> Decomposition of the approved [plan.md](plan.md) / [contracts/interfaces.md](contracts/interfaces.md) /
> [data-model.md](data-model.md), against the approved spec (16 scenarios). Satellite feature —
> `npm run verify` in notebox-web is the signal. Each task ships its tests and its en+pt keys.
> Risk-first: the preview helper (the never-HTML pin, INV-G4) and the wire call come first.

- [x] **T-01 · `list` client call + `PageDto` + `plainTextPreview`**
      - files: `lib/api/types.ts` (+`PageDto`), `lib/api/annotationRecordsClient.ts` (+`list`), `lib/api/annotationRecordsClient.test.ts`, `lib/annotationRecords/preview.ts` (+test)
      - covers: NFR-08 wire, C-01, INV-G4 · scenarios: "Every grid request carries the bearer and no tenant identifier"; the preview risk (exact-string cases: markup stripped, entities decoded once, truncation with ellipsis, hostile markup → text only)
      - notes: `list(typeId, page, size)` via authFetch query string; preview via `DOMParser().parseFromString(...).body.textContent` — returns a string, no DOM handed back
      - depends: — · parallel: no
      - verify: `npm run test -- annotationRecordsClient preview`

- [x] **T-02 · `RecordGridCell` + `RecordsGrid` (QBE + pager)**
      - files: `components/annotationRecords/RecordGridCell.tsx` (+test), `components/annotationRecords/RecordsGrid.tsx` (+test), i18n keys (grid set, en+pt), `src/styles/adf-fusion.overrides.css` (`nb-gridScroll`, pager/QBE rows)
      - covers: FR-05, BR-09/10, FR-18, C-08 (render share), C-12, NFR-08, INV-G1..G5 · scenarios: columns Name+visible in order; served order untouched; LIST badges with colour; masked secret cell + no reveal affordance; rich cell is text (assert no element children from the value); thumbnail via mocked imagesClient + revoke on unmount; pager facts incl. boundaries (total 0 / exact multiple / last page); page-2 fetch on next; size choices 50/100/200 only; QBE filters and clears with zero requests + the page-local note; empty state with New record action
      - notes: `af-table` idiom from `AnnotationTypeList`; pager derives everything from `PageDto` (INV-G2); no `RevealableValue` import anywhere in grid files (INV-G5)
      - depends: T-01 · parallel: no
      - verify: `npm run test -- RecordGridCell RecordsGrid`

- [x] **T-03 · Grid route + navigation wiring + integration + full verify**
      - files: `app/(app)/annotation-types/[id]/records/page.tsx` (new), `app/(app)/annotation-types/[id]/page.tsx` (+View records), `app/(app)/annotation-types/[id]/records/[recordId]/page.tsx` (post-delete → grid), `annotationRecords.integration.test.tsx` (extend), MSW handlers
      - covers: FR-05 end-to-end, C-01/C-02, BR-08/C-09 · scenarios: grid loads and a row opens the detail (BR-09 pair at route level); foreign/missing type → not-found idiom; pt locale; view-records action; post-delete lands on the grid
      - notes: guard inherited from the global RouteGuard (satellite idiom); closes with the satellite's **full `npm run verify`** (clean → lint → typecheck → test → build)
      - depends: T-02 · parallel: no
      - verify: `npm run verify`

## Coverage & sequencing
- **16/16 spec scenarios covered:** wire+preview (T-01), grid behaviour ×10 (T-02), routes/navigation/locale ×5 (T-03).
- **Dependency chain:** T-01 → T-02 → T-03. Serial — each layer feeds the next.
- **Uncovered scenarios:** none.
