# Contracts consumed — feat-023

**This feature defines no contract and changes none.** It moves six screens into a panel and makes
grids editable in place; every call it makes is one the screen it replaces already made.

| Surface moving into the panel | Endpoints it already calls |
|---|---|
| Record detail | `GET /annotation-records/{id}`, `GET /annotation-types/{id}` |
| Record edit | the above, plus `PUT /annotation-records/{id}`, `POST /images` |
| New task | `POST /tasks`, `GET /groups?domain=TASK` |
| Groups | `GET/POST/PUT/DELETE /groups` |
| Members | `GET/POST /members`, `PATCH /members/{id}` |
| Message catalog | `GET/PUT/DELETE /message-overrides` |

## Inline editing

```
PUT /annotation-records/{id}
```

Unchanged in shape. **Cardinality tightened by [A-2]:** confirming a row issues **exactly one**
update for that record — never one per field.

## Deliberately NOT called

```
POST /annotation-records/{id}/values/{fieldId}/reveal
```

**[A-4] / C-12.** A grid is a listing, and a listing never reveals. The inline editor renders the
mask for a Secret field with no editable branch — this endpoint must remain untouched from any
grid, which is what R1's test asserts.
