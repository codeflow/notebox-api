---
step: prd
model: opus
effort: high
reads: [definitions/INTAKE.md, constitution/*.md, prd/PRD_v*.md]
writes: [prd/PRD_v<N>.md, catalogs/open-questions.md]
gate: human_approval
---

# Author the living PRD

**Goal.** One document that states what the product must do and why, where every claim is
traceable to a source. The PRD is *living*: new versions supersede, they never silently rewrite.

## Procedure

1. Read `definitions/INTAKE.md` (not the raw inputs — the intake step already digested them) and
   the constitution.
2. Determine the version. No PRD yet → `prd/PRD_v1.md`. Otherwise `vN+1`, and record what changed
   in `prd/changelog/`.
3. Write the PRD with these sections:
   - **Problem** — whose problem, observed how. If nobody is named, that is an Open Question.
   - **Outcome** — the measurable change that means this worked. Numbers, not adjectives.
   - **Scope** — in, and explicitly out. The out-list prevents the most expensive rework.
   - **Users and jobs** — who acts, what they are trying to finish.
   - **Functional requirements** — `FR-NN`, each one testable.
   - **Non-functional requirements** — `NFR-NN`, each with a number and a measurement method.
   - **Constraints** — from the constitution and the intake.
   - **Open Questions** — `OQ-NN`, each blocking something specific.
4. Every FR/NFR carries an **Origin**: `INTAKE.md § <row>` or `human decision <date>`.

## The no-invention rule

If the sources do not cover something, you write `[TBD — OQ-NN]` and open the question. You do
not write a plausible answer. A PRD that reads complete but contains invented requirements is
the single most expensive failure mode in this pipeline, because everything downstream inherits
it and nobody knows which parts to distrust.

## Gate

`human_approval`. Report: FR/NFR counts, the out-of-scope list, and every OQ opened — the OQs are
the part the human most needs to see.

## Do NOT

- Do not resolve a contradiction flagged by intake. Present both readings and ask.
- Do not renumber or reuse an existing id. Deprecate instead.
