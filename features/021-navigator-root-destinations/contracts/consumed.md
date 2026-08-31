# Contracts consumed — feat-021

This feature defines no contract. It makes two existing **routes** reachable from a new place, and
governs the frequency of one existing **request**.

## Routes made reachable from the tree

| From | Destination | Screen |
|---|---|---|
| tree root `Annotations` | `/annotation-types` | the annotation types list (carries feat-020's records sub-grid) |
| tree root `Tasks` | `/tasks` | the tasks list |

Both are existing authenticated app routes behind the app's route guard. Neither is created here.

## Route asserted for the band's link (OQ-34a)

| Control | Destination |
|---|---|
| the band's "see all N records" | `/annotation-types/{typeId}/records` |

Already implemented in feat-020; this feature adds the assertion that the destination is what the
control actually reaches, rather than stopping at the callback.

## Request governed (OQ-34b)

```
GET /annotation-records?typeId={id}&page=0&size=10
```

Unchanged in shape, cardinality tightened: **at most one per type per mount**, including toggles
delivered within a single tick. Served by feat-008's endpoint; no change requested of the API.
