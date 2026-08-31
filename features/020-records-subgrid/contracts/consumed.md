# Contracts consumed — feat-020

This feature adds **no** endpoint. It consumes two that already ship. Recorded so the implementer
and the auditor read the same shapes.

## 1. `GET /annotation-types/:id` — the band's columns

Needed because the **list** row does not carry fields (see `plan.md` § The finding).

```ts
interface AnnotationTypeDto {
  id: string;
  name: string;
  iconImageId: string | null;
  createdAt: string;
  fields: FieldDto[];
}
```

`FieldDto` is what the compliance argument rests on:

| Field | Why this feature reads it |
|---|---|
| `visibleForViewing` | BR-09 — decides the band's columns |
| `secret` | **C-12** — `RecordGridCell` masks on it; the band must never bypass that |
| `fieldType` | **C-08** — routes `FREE_TEXT` to `plainTextPreview` |
| `options`, `name` | rendering choice/list values and the header |

## 2. `GET /annotation-records?typeId&page&size&group` — the band's rows

Called as `annotationRecordsClient.list(typeId, 0, 10, null)`.

```ts
interface PageDto<AnnotationRecordDto> {
  items: AnnotationRecordDto[];
  page: number;   // 0
  size: number;   // 10 — OQ-32; NFR-08's default of 50 is respected by asking for less
  total: number;  // the TRUE total; the footer line names this, never items.length
}
```

`group` is `null`: the band shows the type's records, unfiltered. The navigator's group filter
belongs to the full grid, which is what the footer link opens.

### Why `size` is a literal and not a page control

OQ-32: a table row cannot hold a 50-row grid, and paginating inside a row means two paginators on
one screen. The band shows 10 and says how many exist. Anything beyond that is the full grid's job.

## Errors

Both calls go through `authFetch`, so both inherit the app-wide behaviour, and the band adds
nothing of its own to it:

| Status | Behaviour |
|---|---|
| 401 | the session-expired path fires app-wide — **not** a band-local message (spec, and R6) |
| 4xx / 5xx | `ApiError`; the band shows the localized failure and stays collapsible |
