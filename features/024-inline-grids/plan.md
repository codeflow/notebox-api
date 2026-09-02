# feat-024 — Plan

> The spec carries six stated assumptions `[B-1..B-6]`. They were resolved rather than asked
> because the product owner's instruction was to run to the end; each names what it rejected.

## 1. What already exists, and is not rebuilt

`useRowEditor` (one row in edit, one request on confirm, a rejection keeps the work) and the
frozen-column-width behaviour both shipped in feat-023 and are grid-agnostic. This feature supplies
**only** the per-grid cell rendering and the per-grid save call.

`RowEditorCell` is NOT reused: it dispatches on `FieldDto.fieldType`, which is annotation-record
vocabulary. These grids edit domain columns, not typed fields.

## 2. The read-only cell — one component, three grids [B-1]

```tsx
<ReadOnlyCell hint="members.email.changedElsewhere">{member.email}</ReadOnlyCell>
```

A cell that shows its value and carries a localized tooltip saying where it *is* changed. Defined
once because three grids need it and because the alternative — each grid inventing its own — is
exactly how the severity icon ended up in one dialog out of nine.

## 3. Per grid

| Grid | Editable in the row | Read-only, with a hint | Save call |
|---|---|---|---|
| `MembersTable` | `displayName`, `active` (select) [B-6] | `email` — identity · `role` — **no endpoint exists** [B-3] | `membersClient.update` (PATCH) + `setActive` |
| `GroupsPanel` | `name` | the two count columns — derived | `groupsClient.replace` |
| type field grid | `name`, `visibleForViewing`, `secret`, constraints | `fieldType` — a migration, not an edit | `annotationTypesClient.replace` |

**The type field grid is a whole-type PUT.** The endpoint replaces the type, so the row's save
sends every field with one changed — the same replace-not-patch trap feat-023's T-09 found on
records, and the same answer: build the body from the type as read, overlay the row.

## 4. C-03, and where it is enforced [B-3]

The members grid asks two questions before rendering a row's controls:

1. **Is the viewer an ADMIN?** No → no edit control on any row.
2. **Is this the viewer's own row?** Yes → the row edits, but the **status control is absent**.

Role is not on this list because **there is nothing to gate**: `UserResource` exposes no role
change. That is a stronger guarantee than a hidden control, and the test asserts the absence rather
than trusting it.

**A row's confirm sends up to two requests**, because the API splits them: `PATCH /users/{id}` for
the name, `PUT /users/{id}/active` for the status. That is a deliberate exception to feat-023's
one-request-per-row invariant (INV-E2), and it is the API's shape, not a choice — the alternative
is inventing an endpoint. Only the halves that actually changed are sent.

## 5. Blast radius

| File | Change | Risk |
|---|---|---|
| `components/grids/ReadOnlyCell.tsx` | **new** — the shared read-only cell [B-1] | R4 |
| `components/members/MembersTable.tsx` | inline editing, role gating | R1, C-03 |
| `components/groups/GroupsPanel.tsx` | inline rename; the rename dialog goes [B-5] | R2 |
| `components/groups/GroupFormDialog.tsx` | create only — its rename mode is removed | R2 |
| `app/(app)/annotation-types/[id]/page.tsx` | inline editing of the field grid | C-12 |
| `lib/i18n/messages/{en,pt}.ts` | three hints, the status states, row-action names | C-09 |

**Not touched:** `TranslationsTable` (already inline), the tasks and subtasks grids (the two named
exceptions), the records grid (feat-023).

## Reversibility

Every change is additive to a grid's row rendering plus one save call. Reverting a grid is deleting
its editor branch; nothing else depends on it. The one irreversible-feeling change is B-5 (the
rename dialog), and its component survives for the create path.
