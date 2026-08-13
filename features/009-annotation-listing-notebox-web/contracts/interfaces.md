# Contract — Grid interfaces (feat-009)

## Client addition (lib/api)
```ts
// types.ts
export interface PageDto<T> { items: T[]; page: number; size: number; total: number; }
// annotationRecordsClient.ts
list(typeId: string, page: number, size: number): Promise<PageDto<AnnotationRecordDto>>;
// GET /annotation-records?typeId&page&size — feat-008 contract, consumed frozen.
```

## Components
```ts
// One string out, always — the cell is a text node by construction (spec: never HTML in a cell).
plainTextPreview(html: string, max?: number): string;

RecordGridCell({ field: FieldDto, value: AnnotationValueDto }): JSX;   // dispatch per fieldType
RecordsGrid({ type: AnnotationTypeDto, onOpenRecord(id: string): void }): JSX;
// internal state { page, size, filter } — page/size refetch; filter is page-local, zero requests
```

## Routes & navigation
| Surface | Change |
|---|---|
| `annotation-types/[id]/records` (new) | the grid, behind the global guard |
| `annotation-types/[id]` | + "View records" action |
| `annotation-types/[id]/records/[recordId]` | post-delete navigates to the grid |

## i18n keys (en + pt)
`annotationRecords.grid.{viewRecords, empty, filterNote, filterPlaceholder, pagerOf, pageSize, next, previous, nameColumn}`
