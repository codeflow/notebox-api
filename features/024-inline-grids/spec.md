# feat-024 — Inline editing in every remaining data grid

> Completes **OQ-35**. feat-023 built the mechanism (`useRowEditor`, `RowEditorCell`, the panel)
> and applied it to one grid. This applies the rule the product owner actually stated to the rest,
> and answers the question feat-023 deferred: *what does a grid do when its listed columns are not
> all editable?*

## Origin

- **FR-05** (a listing exposing visible fields) · **FR-08** (groups) · **FR-16** (runtime
  translations) · **OQ-11** (member administration)
- **US-2.2**, with reach into US-3.1 and US-5.2
- **Brief item 2.3**, verbatim: *"regra pra todo o sistema, a edição no datagrid (em qualquer um em
  qualquer tela) deve ser uma edição na própria linha"*
- **Brief item 4.4**, on the type's field grid, with the product owner's own caveat: *"então não sei
  como você vai fazer"*

**Why this is a feature and not a fix.** It adds an editing mode and a save path to three grids,
two of which sit on administration surfaces. The members grid in particular is where a role is
changed — the sharpest privilege edge in the product.

## In

- **The members grid** edits **name and status** in the row.
- **The groups grid** renames in the row, replacing the rename dialog.
- **The type's field grid** edits name, visibility, secret and constraints in the row.
- One shared rule for **columns a row lists but cannot edit**, applied consistently.

## Out

- **The translations grid** — it already edits inline (brief 24.7 observed it), and it is the
  pattern the others were copied from. Untouched.
- **The tasks and subtasks grids** — the product owner's two named exceptions, settled in feat-023.
- **The records grid** — done in feat-023.
- Changing a field's **type** in the row. It is listed and it is not editable here: altering
  `fieldType` on a field with stored values is a data migration, not an edit.
- No API change. Every rule below is already enforced server-side.

## Assumptions

Recorded as decisions, not invented requirements. Each names what was rejected.

| # | Question | Decision | Rejected |
|---|---|---|---|
| **B-1** | What about a column that is listed but not editable — email, a derived count, a field's type? | **It renders read-only in the row, in place, with a tooltip saying where it IS changed.** The row keeps its shape; the member is never left clicking something inert with no explanation. | Hiding the column in edit mode — the row would reflow and the member would lose their place. |
| **B-2** | Does A-7's rule ("a grid edits inline when its listed fields ARE its editable fields") disqualify these grids? | **No, and B-1 is why.** The rule is about whether a row can *hold* the edit. A grid with one read-only column still edits everything a row can hold; a task's edit needs priority, dates and a card that no row has columns for. The distinction is capacity, not purity. | Reading the rule strictly and sending all three to the panel — it would make the product owner's system-wide rule apply to exactly one grid. |
| **B-3** | Can a **role** be changed in the row? | **No — the API has no endpoint for it.** `UserPatch` carries `displayName` and `locale`; status has `PUT /users/{id}/active`; **role has nothing.** So role is a `[B-1]` read-only column, with a hint saying it is set when the member is provisioned. Found while implementing T-02, by reading `UserResource` rather than assuming — the first draft of this spec asserted role editing and a whole C-03 scenario on top of it. | Adding a role endpoint — that is an API feature with its own audit and privilege-escalation surface, and this spec says "no API change" for good reason. |
| **B-3b** | Can a member deactivate **themselves** in the row? | **No — the control is absent on the signed-in member's own row**, as it already is for the action icon this replaces. The server refuses it; the UI must not offer an action that will be rejected. | Offering it and showing the server's error — it teaches the member that the product's rules are suggestions. |
| **B-4** | Can the `secret` flag be flipped in the row? | **Yes, and the server is the guard.** It already refuses when the field holds values (`AnnotationTypeFieldSecretFlipException`), and a rejected save keeps the row in edit with the member's values and the reason (INV-E3, feat-023). | Disabling the toggle client-side on a guess about whether values exist — the client does not know, and a control disabled for the wrong reason is worse than one that explains a refusal. |
| **B-7** | The type's field grid: can a field be **renamed** in the row? | **Yes, and the server is the guard — the same answer as [B-4].** `applyFields` matches fields **by name**, so a rename against a type that owns records is refused (`AnnotationTypeFieldHasRecordsException`); against an empty type it succeeds. The client does not know whether records exist, and the refusal carries the reason. Found by reading `AnnotationTypeService`, not by assuming. | Disabling the name on a guess about record counts — the detail screen does not load them, so the guess would be wrong exactly when it mattered. |
| **B-5** | Groups: keep the rename dialog as well? | **No, remove it.** Two ways to rename is how two behaviours drift; the row is now the way. Create still opens its dialog — creating is not editing a row that exists. | Keeping both — the brief's whole complaint about grids was that editing had too many shapes. |
| **B-6** | The members grid's `status` column — a checkbox, or a select? | **A select with the two states**, localized, matching how `role` is edited beside it. | A checkbox — it reads as "active" only, and the column shows a word, so toggling a box to change a word is a mismatch. |

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-05 — the system-wide rule reaches every grid that can hold it

  Scenario Outline: A grid edits everything a row can hold
    Given the <grid>
    When the member activates the edit icon on a row
    Then <editable> become editable in place
    And <readonly> stay read-only in the row
    And the row offers confirm and cancel controls
    Examples:
      | grid              | editable                                  | readonly              |
      | members grid      | name and status                           | email and role        |
      | groups grid       | the group's name                           | its item counts       |
      | type fields grid  | name, visibility, secret and constraints  | the field's type      |

  Scenario: A read-only column says where it IS changed      # [B-1]
    Given a row in edit with a column it cannot edit
    When the member looks at that cell
    Then it shows its value unchanged
    And it carries a localized tooltip naming where that value is changed

  Scenario: Confirming writes the row once
    Given a row in edit with a changed value
    When the member confirms
    Then exactly one update request is issued for that row
    And the row returns to its read state showing the new value

  Scenario: A failed save keeps the member's work
    Given a row in edit whose update will be rejected
    When the member confirms
    Then the row stays in edit with the values the member typed
    And the server's message is shown against that row

Feature: C-03 — administration grids do not offer what they may not do

  Scenario: No row offers a role control at all      # [B-3]
    Given the members grid
    When a row enters edit
    Then the role cell shows its value read-only
    And it carries a localized hint saying where a role is set
    And no row anywhere offers a control that changes a role

  Scenario: An administrator cannot deactivate themselves from the row      # [B-3b]
    Given the members grid, signed in as an administrator
    When that administrator edits their OWN row
    Then the status control is absent from that row
    And every other row offers it

  Scenario: A non-administrator sees no editing at all
    Given the members grid, signed in as a MEMBER
    Then no row offers an edit control

Feature: C-12 — the secret flag is the server's call

  Scenario: Flipping secret on a field that holds values is refused, and nothing is lost
    Given the type's field grid, and a field that holds stored values
    When the member turns its secret flag on and confirms
    Then the server refuses
    And the row stays in edit with the flag as the member set it
    And the server's reason is shown

  Scenario: A Secret field's stored values are never shown by this grid
    Given the type's field grid
    Then no cell renders a stored value for any field

  Scenario: Renaming a field of a type that owns records is refused      # [B-7]
    Given the type's field grid for a type that owns records
    When the member renames a field and confirms
    Then the server refuses
    And the row stays in edit with the name the member typed
    And the server's reason is shown
```

## Risks

| # | Risk | Mitigation |
|---|---|---|
| **R1** | The members grid becomes a privilege-escalation surface | B-3: there is no role control to escalate through, and a test asserts its absence by name. B-3b removes the status control on one's own row. A test signs in as each role and asserts what is offered |
| **R2** | Removing the rename dialog breaks feat-008's tests | They move to the row, as feat-010/012's subtask tests moved to the panel — re-pointed, never deleted |
| **R3** | Three grids, three copies of the editor | `useRowEditor` is the shared piece and already exists; what differs per grid is only which cells are editable |
| **R4** | A grid that lists a derived value lets someone type into it | B-1's read-only rendering is asserted per grid, by name |

## Compliance pre-flight

| Item | Verdict | Why / evidence |
|---|---|---|
| **C-01 Tenant isolation** | not applicable | No new data path. |
| **C-02 Authenticated by default** | **applies** | Every save goes through `authFetch`, unchanged. |
| **C-03 Least-privilege** | **applies — the strictest item here** | The members grid sits on an administration surface. It does **not** edit roles: no endpoint exists and none is added. Evidence: the three C-03 scenarios, asserted per role, including the own-row case and the absence of any role control. |
| **C-04 Personal data minimization** | not applicable | Renders nothing new. |
| **C-07 / C-08** | not applicable | No upload, no rich text in these grids. |
| **C-09 Localization** | **applies** | Read-only tooltips, the status select's two states, and the new row-action names. Both catalogs, keyset guard. |
| **C-10 Audit trail** | not applicable | Server-side and unchanged — a role change is already audited. |
| **C-12 Encryption at rest** | **applies** | The field grid edits the `secret` flag. Evidence: the two C-12 scenarios; the grid renders no stored values at all. |

## Traceability

| Scenario | Test |
|---|---|
| the outline, per grid | `MembersTable.test.tsx`, `GroupsPanel.test.tsx`, `annotationTypes.integration.test.tsx` |
| read-only column tooltip | one per grid, asserted by its localized name |
| no role control anywhere | `MembersTable.test.tsx`, asserted by the control's absence in every row |
| own status absent | `MembersTable.test.tsx`, ADMIN signed in as themselves |
| secret flip refused | `annotationTypes.integration.test.tsx`, server stub returning the refusal |
