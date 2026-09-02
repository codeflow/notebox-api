# feat-024 — Tasks

| | |
|---|---|
| Tasks | 7 |
| Chain | T-01 → T-02, T-03, T-04 → T-05 → T-06 · T-07 last |
| Probes named | T-02, T-03, T-05 |

---

- [x] **T-01 · The read-only cell, once**
      - files: `components/grids/ReadOnlyCell.tsx`, its test
      - covers: *"A read-only column says where it IS changed"* · **[B-1]**
      - shows the value, carries a localized tooltip, and offers no control
      - verify: `npx vitest run components/grids`

- [x] **T-02 · The members grid edits its row — and the compliance task**
      - files: `components/members/MembersTable.tsx`, `MembersScreen.tsx`, tests
      - covers: the outline's members row · both **C-03** scenarios · **[B-3] [B-6]**
      - **sequenced before the other two grids on purpose:** if a role cannot be kept off one's own
        row, the scope changes, and finding that out after two other grids depend on the pattern
        costs more.
      - probe: offer the role control on the viewer's own row → the C-03 assertion must fail
      - verify: `npx vitest run components/members`

- [x] **T-03 · The groups grid renames in the row, and the dialog goes**
      - files: `components/groups/GroupsPanel.tsx`, `GroupFormDialog.tsx`, tests
      - covers: the outline's groups row · **[B-5]**
      - feat-008's rename tests move to the row — re-pointed, never deleted
      - probe: leave the counts editable → the read-only assertion must fail
      - depends: T-01

- [x] **T-04 · The type's field grid edits in the row**
      - files: `app/(app)/annotation-types/[id]/page.tsx`, tests
      - covers: the outline's field row · the field's type stays read-only
      - depends: T-01

- [x] **T-05 · The whole-type PUT, built from the type and not from the row**
      - files: `lib/annotationTypes/rowUpdate.ts`, its test
      - covers: *"Confirming writes the row once"* for the field grid
      - **the trap this task exists for:** the endpoint replaces the whole type, so a body built
        from the row deletes every other field. feat-023's T-09 found this on records, live, with
        the unit test green.
      - probe: send only the edited field → the assertion that the others survive must fail
      - depends: T-04

- [x] **T-06 · C-12 — the secret flag is the server's call**
      - files: the field grid's test
      - covers: both **C-12** scenarios · **[B-4]**
      - a refused flip keeps the row in edit with the flag as set, and shows the reason
      - depends: T-05

- [x] **T-07 · Live browser pass**
      - **why it is a task:** jsdom computes no layout. feat-023's live pass found a column reflow
        and a data loss that the whole suite was green over.
      - check: each grid's read-only cells line up with the editable ones; a refused save keeps its
        geometry; the members grid renders no role control on one's own row
      - depends: T-06
