# Contract — Record listing (feat-008)

## Endpoint
| Method | Path | Purpose | Success | Auth |
|---|---|---|---|---|
| GET | `/annotation-records?typeId={uuid}&page={0+}&size={1..200}` | Page of one type's records, visible fields only | `200` + `PageDto` | member |

Defaults: `page=0`, `size=50` (NFR-08). Order: **`createdAt DESC, id DESC`** (OQ-20, documented in OpenAPI).

## PageDto
```jsonc
{
  "items": [ /* AnnotationRecordDto — the feat-005 shape, values FILTERED to visibleForViewing fields,
                masking (FR-18) and sanitize-on-read (C-08) inherited unchanged */ ],
  "page": 0, "size": 50, "total": 51
}
```
The projection is presentation, never access control (BR-09): the detail read keeps returning all fields.

## Errors
| Condition | Mechanism | HTTP | key / code |
|---|---|---|---|
| `typeId` missing | Bean Validation | 400 | `annotation.record.list.type.required` |
| `size` < 1 or > 200 | Bean Validation | 400 | `annotation.record.list.size.out_of_bounds` |
| Type unknown or foreign-tenant | existing `AnnotationTypeNotFoundException` | 404 | `annotation.type.not_found` |

Both new keys ship en + pt (C-09). No other endpoint or key changes.
