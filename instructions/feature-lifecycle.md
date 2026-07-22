---
step: feat-*
model: inherit
effort: inherit
reads: [features/NNN-slug/**]
writes: [features/NNN-slug/**]
---

# Feature lifecycle

**Goal.** Govern one feature end to end. This file is the parent of the seven substeps; each has
its own instruction file and its own model.

```
spec 🚦► plan 🚦► tasks 🚦► implement ─verify─► audit ─pass─► publish ─CI green─► review 🚦(GitHub)
```

## Why the models differ

| Substep | Model | Why |
|---|---|---|
| spec | `opus/high` | Ambiguity is cheapest to kill here and most expensive to inherit. |
| plan | `opus/high` | Structural choices; wrong ones are paid for in every later task. |
| tasks | `sonnet/medium` | Mechanical decomposition of an approved plan. |
| implement | `sonnet/high` | Volume work against a fixed target; raise effort, not model. |
| audit | `opus/xhigh` | Adversarial review must be able to out-think the implementer. |
| publish | `sonnet/medium` | Push feature/<slug>; the gate is the REMOTE CI run green. |
| review | `sonnet/medium` | PR → integration branch; the gate is the human's GitHub approval (👍 + review). |

Raise effort for **ambiguity and irreversibility**, not for volume. A thousand-line mechanical
edit is still `sonnet/medium`; a fifty-line change to an auth boundary is `opus/high`.

## Invariants across the whole lifecycle

- **Origin section in every artifact.** Spec, plan and tasks each open with the US, the FRs, the
  BRs bound, and a source citation. Without it, traceability breaks and `audit` will fail it.
- **Gates are real.** `human_approval` means an explicit human yes in chat. Do not infer approval
  from silence, from "looks good", or from the human asking a follow-up question.
- **Open Questions stop work.** A blocked substep is marked `blocked` with the OQ id. Move to
  another feature rather than guessing.
- **Never skip forward.** No planning inside the spec, no coding inside the plan. Each stage
  exists to be *wrong cheaply*.

## Rollup

The feature step is `done` only when all five substeps are `done` or `skipped`. `wf status`
computes this — do not set a parent's status by hand.
