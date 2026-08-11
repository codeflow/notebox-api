---
step: feat-*.plan
model: opus
effort: high
subagent: Plan
reads: [features/NNN-slug/spec.md, constitution/01-architecture.md, constitution/03-code-standards.md]
writes: [features/NNN-slug/plan.md, features/NNN-slug/data-model.md, features/NNN-slug/contracts/**]
gate: human_approval
---

# Plan — how

**Goal.** Decide the design once, in writing, so that `tasks` and `implement` become mechanical.

## Procedure

1. Read the approved `spec.md` and the architecture boundaries. A plan that crosses a boundary is
   rejected — escalate instead of crossing it.
   If the feature is **routed to a workspace project** (`project` field on the feature step),
   design against THAT satellite's stack — `workspace.projects[].stack` in `workflow.json` —
   and read that project's sources for its patterns. The hub's stack is irrelevant to it.
   **If the satellite ships a design reference (e.g. `design/handoff/`), it is the visual source
   of truth for any UI** — read its `README.md` before designing screens, and open its flow HTML
   in a browser when a detail is unclear rather than inferring from static prints.
2. Survey what already exists before designing anything new. Delegate the sweep to an `Explore`
   sub-agent: *"where does this codebase already handle X, and what is the established pattern?"*
   Reusing the existing pattern beats introducing a better one.
3. Write `plan.md`:
   - **Origin** — spec, US, FRs.
   - **Approach** — the design in prose, with the seams it touches.
   - **Alternatives rejected** — at least one, with why. A plan with no rejected alternative
     usually means only one option was considered.
   - **Blast radius** — every existing file, contract and consumer this changes.
   - **Risk** — what could break, and the signal that would reveal it.
4. Write `data-model.md` — entities, fields, types, invariants, and migrations. Invariants must
   match the BRs cited in the spec.
5. Write `contracts/` — API schemas, event payloads, interfaces. These are the artifacts the
   implementer codes against and the auditor checks against.

## Reversibility

Mark each decision **reversible** or **one-way**. Spend the effort on the one-way ones: schema
shape, public contract, data migration, anything another team will start depending on. Reversible
decisions do not deserve a paragraph of deliberation.

## Gate

`human_approval`. Report the approach in three sentences, the one-way decisions, and the blast
radius. The blast radius is what the human is best positioned to catch you being wrong about.

## Do NOT

- Do not write implementation code here — contracts and signatures only.
- Do not introduce a new dependency without stating what it replaces and why the existing stack
  cannot do it.
