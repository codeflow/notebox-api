# Plan — Form widgets and states

**Feature:** features/019-form-widgets · **Status:** Approved (standing authorisation 2026-08-27)

## Approach
**A1 · Spin buttons on `ValueField`'s number branch.** A `step(current, direction)` helper clamps to
`field.numberMin` / `numberMax`, so a spinner can never produce a value the type would reject — the
server's validation is not duplicated, it is simply never provoked. An empty field starts from the
minimum, so the first click always lands somewhere valid.

**A2 · `af-choiceGroup`** on the single- and multiple-choice wrappers. Class only; the radio and
checkbox behaviour already shipped.

**A3 · `NotesDrawer`** on the task detail screen: dock tab, `aria-expanded`/`aria-controls`, closed by
default. Its body says notes are not stored — the API has no notes surface, and an empty panel would
read as "no notes written yet" rather than "this does not save".

## Reversibility
No one-way decisions. The closest is A1's clamping rule, which becomes the field's client-side
contract; it is pinned by three tests.

## Alternatives rejected
**Wrapping the remaining text inputs in `af-comboField`** to close the report's widget diff. Rejected:
the handoff uses the combo shell where there is a picker or an LOV behind the trailing button. This
app has neither on those screens, so it would add a button-shaped affordance that does nothing —
which is the failure the login feature deliberately avoided by making its adornment inert and saying
so. Recorded as a deviation instead.

## Blast radius
| File | Change |
|---|---|
| `components/annotationRecords/ValueField.tsx` + test | spin buttons, `step`, choice-group class |
| `components/tasks/NotesDrawer.tsx` + test | **new** |
| `app/(app)/tasks/[taskId]/page.tsx` | mount the drawer |
| `lib/i18n/messages/en.ts`, `pt.ts` | spinner + drawer strings |
| `src/styles/adf-fusion.overrides.css` | spin box, dock |

## Risks
| # | Risk | Signal |
|---|---|---|
| R1 | A spinner produces an out-of-bounds value | the two clamping tests |
| R2 | The drawer reads as "no notes yet" rather than "not stored" | the explicit copy test |
| R3 | The existing record-form suites regress | they must pass unmodified |
