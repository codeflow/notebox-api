---
step: constitution.principles
model: opus
effort: high
reads: [definitions/INTAKE.md]
writes: [constitution/00-principles.md]
---

# Business principles (BR)

**Goal.** The inviolable business rules. Each gets a `BR-NN` id that is immutable forever.

## Shape

```markdown
### BR-01 — <short imperative name>
**Rule.** <one sentence, testable>
**Because.** <the business reason; cite INTAKE.md or the human decision>
**Violation.** <what breaks, and who is harmed, if this is broken>
```

## What belongs here

- Rules about money, identity, data ownership, legal exposure, and irreversibility.
- Invariants that must hold regardless of feature: "a refund never exceeds the original charge."
- Rules that a well-meaning engineer might otherwise trade away for convenience.

## What does NOT belong here

- Architecture choices → `01-architecture.md`.
- Regulatory checklists → `02-compliance.md`.
- Style and naming → `03-code-standards.md`.
- Anything a feature spec could legitimately override.

## Done when

Every BR is numbered, grounded in a cited source, and states its violation consequence.
Ungrounded candidates are logged as Open Questions instead.
