# feat-028 — audit

**Model:** opus · **Effort:** xhigh · **Date:** 2026-09-02 · **Verdict: PASS with findings (fixed)**

`verify` green at 768 tests. Three findings, all in **my own guards** rather than in the feature's
behaviour — which is the more interesting result.

---

## F-01 · The coverage guard was applied once, by the guard against applying things once

`formConventions.test.ts` holds two rules. The first — *a field message reaches its control* — was
widened during implementation to recognise all three error idioms in this codebase (`<FieldError>`,
a bare `nb-fieldError`, a bare `nb-msg-error` span). **The second was left matching `<FieldError>`
alone.**

So two forms were invisible to it:

| file | writes its error as | consequence |
|---|---|---|
| `GroupFormDialog.tsx` | a bare `nb-msg-error` span | no Enter-to-advance, no focus on a rejected submit |
| `LoginForm.tsx` | a bare `nb-fieldError` div | the same, on the first screen anyone sees |

The sign-in screen is the one every member meets before anything else, and it was the one form
without the keyboard behaviour the feature exists to give.

**Fixed**: both forms use the hook, and the second rule now uses the first's detection.

## F-02 · And then the guard could be satisfied without applying anything

Probing F-01's fix — removing `{...flow.formProps}` from `LoginForm` while leaving the
`useFormFlow()` call — left the test **green**. It checked for the hook's *name*, so a form that
imported the behaviour and never applied it passed.

That is the least useful state to be certified in: the code says the rule is followed and the
screen does not follow it. **Fixed**: the rule now requires the call *and* the spread. Re-probed;
it fails and names the file.

## F-03 · The two elements that outlived their usefulness — both found in a browser, neither by a test

Recorded because it is the same mistake twice in one feature, and because in both cases the tests
passed:

- `useFormFlow` held the form in a **ref**, so for every form that renders a loading placeholder
  first — which is all of them — the mount effect saw `null` and never ran again. Enter worked,
  focus did not.
- `PanelChrome` held the panel in a **ref**, and the panel is portalled into a slot found in an
  effect: the first render puts it where it stands and the second **moves** it, destroying the node
  the effect had captured.

Both are now held in state. A test never catches either, because a test renders its form on the
first pass and has no shell to portal into. **The general lesson, worth carrying:** an element
captured in an effect is only the element on screen if nothing re-parents or delays it, and in this
codebase both happen routinely.

---

## The checks that passed

| Check | Result |
|---|---|
| **Traceability** | All ten scenarios cite a test; the two probes named in the spec were run and each killed only its own assertions. |
| **Scenario honesty** | The `LateHarness` suite exists precisely because the first harness rendered its form immediately, which no real form does. It is the honest fixture. |
| **E-2 (accessibility)** | `Ctrl`/`Cmd`+`Enter` asserted from a non-final field. This is the cost of E-1 being paid, not assumed. |
| **E-3** | Enter is not intercepted in a `textarea`; asserted. The rich-text editor is `contenteditable` and takes the same branch. |
| **E-5** | Typing clears **that** field's message; a second field keeps its own. Asserted per form. |
| **C-09** | No new strings. |
| **Scope** | Two files beyond the plan (`LoginForm`, `GroupFormDialog`) — both are F-01's fix, and the plan could not have named them because the guard that should have found them was blind. |

## Verdict

**pass with findings, all fixed.** The feature's behaviour held up; what failed audit was the
machinery meant to keep it honest. Both guards now fail when the rule is broken, and both were
probed to prove it.
