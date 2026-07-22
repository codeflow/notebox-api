---
step: constitution
model: opus
effort: high
reads: [definitions/INTAKE.md, constitution/*.md]
writes: [constitution/*.md]
gate: human_approval
---

# Author the guardrails

**Goal.** Write the rules that outrank everything else in this repository, including future
human instructions given in chat. This is the highest-leverage step in the pipeline: get it
wrong and every downstream artifact inherits the error.

The four substeps (`principles`, `architecture`, `compliance`, `standards`) each have their own
instruction file. This file governs all of them.

## The bar

A constitution item is worth writing only if it is:

- **Testable or observable.** "Handlers must not contain SQL" passes. "Code should be clean" fails.
- **Consequential.** Removing it would change a real decision downstream.
- **Project-specific.** If it would read identically in any other repo, it is boilerplate — cut it.

Ten sharp rules beat sixty vague ones. The constitution is read on every feature; every dead line
costs tokens forever.

## Procedure

1. Read `definitions/INTAKE.md` for stated constraints. Read the existing `constitution/` files —
   they ship with generic scaffolding that must be replaced, not appended to.
2. Draft each of the four documents via its substep.
3. For every rule, write the **consequence of violating it** next to it. A rule with no
   consequence is a preference.
4. Anything you cannot ground in a definition or an explicit human decision goes to
   `catalogs/open-questions.md` — never into the constitution on your own authority.

## Gate

`human_approval`. Present a diff-style summary: what you added, what you deleted from the
scaffolding, and which rules you could not ground. Do not proceed to `prd` without an explicit yes.

## Do NOT

- Do not import rules from other projects because they are "best practice".
- Do not soften a constraint the human stated to make it easier to satisfy.
