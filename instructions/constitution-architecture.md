---
step: constitution.architecture
model: opus
effort: high
reads: [definitions/INTAKE.md, constitution/00-principles.md]
writes: [constitution/01-architecture.md]
---

# Architecture boundaries

**Goal.** The structural decisions that features must not relitigate, and the seams they must
respect.

## Record each decision as

```markdown
### AD-01 — <decision>
**Context.** <the forces>
**Decision.** <what was chosen>
**Consequence.** <what this makes easy; what it makes hard>
**Boundary.** <the rule a feature must not cross, stated so it can be checked>
```

The **Boundary** line is the part agents enforce. "Decision: hexagonal architecture" is
unenforceable; "Boundary: nothing under `domain/` may import from `infra/`" is a grep.

## Cover at minimum

- Layering and dependency direction — which way imports may point.
- Where state lives, and who is allowed to write it.
- Synchronous vs. asynchronous boundaries, and what may cross them.
- The integration seam for every external system named in `INTAKE.md`.
- What is deliberately **not** abstracted yet, so nobody builds a premature framework.

## Done when

Every boundary is mechanically checkable — by a linter, a grep, a test, or an import rule.
Boundaries that cannot be checked are documented as risks in `catalogs/open-questions.md`.
