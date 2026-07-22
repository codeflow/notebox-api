---
step: feat-*.tasks
model: sonnet
effort: medium
reads: [features/NNN-slug/plan.md, features/NNN-slug/contracts/**]
writes: [features/NNN-slug/tasks.md, workflow.json]
gate: human_approval
---

# Tasks — atomic and verifiable

**Goal.** Cut the approved plan into units that can each be finished, verified and reviewed on
their own. The design is already decided — this step is decomposition, not thinking.

## What makes a task atomic

- **One outcome.** If the title needs "and", it is two tasks.
- **Independently verifiable.** It ends with a check that passes or fails on its own, without
  waiting for a later task.
- **Bounded.** Roughly one focused sitting. A task that cannot be described in two lines is
  usually hiding an unresolved design question — send it back to `plan`.
- **Ships tests with it.** Tests are not a trailing task. A task without its test is not done.

## Shape

```markdown
- [ ] **T-03 · Reject refunds exceeding the original charge**
      - files: `domain/refund.ts`, `domain/refund.test.ts`
      - covers: FR-07, BR-02 · scenario: "Refund exceeds the original charge"
      - depends: T-01
      - parallel: yes
      - verify: `npm run verify -- refund`
```

Every task cites the Gherkin scenario it makes pass. A task citing no scenario is either
unnecessary or the spec has a hole — resolve which before continuing.

## Mirror into workflow.json

Add each task as a substep of the feature's `implement` step, keeping the same ids:

```json
{ "id": "feat-003-refund.implement.t3",
  "title": "T-03 — Reject refunds exceeding the original charge",
  "kind": "task", "status": "pending",
  "instructions": "instructions/feature-implement.md",
  "agent": { "model": "sonnet", "effort": "medium", "isolation": "worktree" } }
```

Mark `isolation: worktree` only on tasks flagged `parallel: yes`. Tasks that touch the same
files must not run in parallel worktrees — they will conflict on merge.

## Ordering

Order by dependency, then by risk: the task most likely to invalidate the plan goes first, while
changing the plan is still cheap.

## Gate

`human_approval`. Report the task count, the dependency chain, which are parallelisable, and any
scenario left uncovered.
