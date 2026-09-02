# feat-028 — Form flow: error focus, error clearing, and Enter between fields

## Origin

- Product owner, 2026-09-02, verbatim: *"quando um campo estiver com erro, o foco fique no campo,
  ao começar a digitar o erro e a cor do campo voltem ao normal e queria aquele comportamento de ao
  ir digitando enter ele vai mudando para o próximo campo e o ultimo enter faz o submit"*
- **US-2.1**, and a system rule in the shape of brief item 2.3: it applies to every form, not to
  the screen it was asked on.

**Why it is a feature and not a fix.** It adds behaviour to every form in the product and it
changes what a keypress means. `/wf-fix`'s own rule sends that to a spec.

## In

- **Focus lands on the first field in error** when a submit is rejected.
- **Typing in a field clears that field's error** — the message and the colour — immediately.
- **Enter advances to the next control; Enter on the last one submits.**
- One shared mechanism, and a guard that fails when a form does not use it.

## Out

- The rich-text editor and any `<textarea>`: Enter is a newline there, and always will be.
- The grids' inline row editors. They are one row, not a form, and they already have their own
  confirm/cancel pair; Enter-to-advance across a grid row is a different question.
- Changing which fields are validated, or when. This feature moves focus and clears state; it
  decides nothing about correctness.

## Assumptions

| # | Question | Decision | Rejected |
|---|---|---|---|
| **E-1** | Enter-to-advance breaks the HTML default, where Enter submits. Is that acceptable? | **Yes, chosen deliberately by the product owner (2026-09-02).** It is the Oracle Forms/ADF idiom this product's skin comes from, so it is consistent with what the rest of the interface promises. | Silently keeping the platform default — the product owner asked for this one by name. |
| **E-2** | What does a keyboard-only or screen-reader user do, when Enter no longer submits? | **`Ctrl`/`Cmd`+`Enter` submits from any field**, always, and is the documented escape hatch. This is the accessibility cost of E-1 and it is paid explicitly rather than ignored. | Leaving no way to submit without tabbing to the button — that would make the form slower for exactly the people the keyboard path exists for. |
| **E-3** | Which controls does Enter move between? | **Every enabled, visible form control in DOM order** — inputs, selects, checkboxes — skipping `textarea` and anything `contenteditable`. | A hand-maintained list per form: it goes stale the moment a field is added, and the failure is silent. |
| **E-4** | Where does focus go when a submit is rejected? | **The first control marked `aria-invalid`, in DOM order.** Read from the DOM rather than from each form's error object, because every form models errors differently and the attribute is the one thing they all already agree on. | Each form calling a focus helper — one more thing to forget, and forgetting it is invisible. |
| **E-5** | Typing clears the error for **that** field, or for the whole form? | **That field only.** Clearing every message because one field was touched hides problems the member has not looked at yet. | Clearing all — simpler to implement, and it loses information the member needs. |
| **E-6** | Does clearing the message also clear the colour? | **They are the same fact.** The colour is driven by `aria-invalid`, so clearing the error clears both, and they cannot drift apart. | Styling the field from a separate flag — two sources for one truth is how the announced state and the visible state end up disagreeing. |

## Acceptance criteria (Gherkin)

```gherkin
Feature: a rejected submit puts the member where the problem is

  Scenario: Focus lands on the first field in error              # [E-4]
    Given a form with an empty required field
    When the member submits
    Then the first field marked invalid holds the focus

  Scenario: The first, not just any                              # [E-4]
    Given a form whose second and fourth fields are both rejected
    When the member submits
    Then the second field holds the focus

Feature: correcting a field clears its verdict

  Scenario: Typing clears that field's message and colour        # [E-5] [E-6]
    Given a field showing a validation message
    When the member types in it
    Then the message is gone
    And the field no longer carries the error colours

  Scenario: Other fields keep their messages                     # [E-5]
    Given two fields both showing messages
    When the member types in the first
    Then the second still shows its message

Feature: Enter walks the form

  Scenario: Enter moves to the next control                      # [E-3]
    Given the focus in a field that is not the last
    When the member presses Enter
    Then the next control holds the focus
    And the form is not submitted

  Scenario: Enter on the last control submits
    Given the focus in the last control
    When the member presses Enter
    Then the form is submitted

  Scenario: Ctrl+Enter submits from anywhere                     # [E-2]
    Given the focus in the first of several fields
    When the member presses Ctrl+Enter
    Then the form is submitted

  Scenario: Enter is left alone where it means a newline         # [E-3]
    Given the focus in a textarea or the rich-text editor
    When the member presses Enter
    Then the focus does not move
    And the form is not submitted
```

## Risks

| # | Risk | Mitigation |
|---|---|---|
| **R1** | A form is added later and nobody wires the behaviour | A coverage guard over the class, like the severity-icon and frozen-column guards — it fails when a form renders field errors without the hook |
| **R2** | Enter-to-advance reaches the grids' row editors, where it would mean something else | The hook is opt-in per form and the row editors do not use it; asserted |
| **R3** | Focus moves while the member is still typing | Focus moves only on a **rejected submit**, never on a keystroke |
| **R4** | `aria-invalid` is missing on some controls, so focus has nothing to find | A separate sweep is running for exactly that; its findings land here |

## Compliance pre-flight

| Item | Verdict | Why / evidence |
|---|---|---|
| **C-09 Localization** | not applicable | No new strings. |
| **Accessibility** | **applies — the sharpest item here** | E-1 changes what Enter means, which is a real cost to keyboard users. E-2 pays it with `Ctrl`/`Cmd`+`Enter`, asserted. Focus is moved only to a control that is *announced* invalid, so what the member is sent to is what assistive technology is describing. |
| Everything else | not applicable | No new data path, request, or permission. |
