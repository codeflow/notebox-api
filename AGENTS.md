# AGENTS.md — Harness Engineering

> How agents run this pipeline: roles, model routing, background execution and the closed loop.
> Companion to `CLAUDE.md`. The pipeline itself is data — see `workflow.json`.

---

## 1. The closed loop

An agent is only autonomous when it can act and **get a signal back without a human**. That
signal is `harness.verify`.

```
definitions/          raw material
   │ intake                       haiku/low · background
   ▼
constitution/         guardrails  opus/high      ── human approval
   │
   ▼
prd/PRD_vN.md         intent      opus/high      ── human approval
   │
   ▼
catalogs/             FR NFR US OQ  sonnet/medium
   │
   ▼
features/NNN-slug/
   spec.md            opus/high    ── human approval
   plan.md            opus/high    ── human approval
   tasks.md           sonnet/medium ── human approval
   code + tests       sonnet/high  ── verify green   ◄── the loop closes here
   audit.md           opus/xhigh   ── audit pass
   │
   ▼
HANDOFF · ROADMAP · MEMORY
```

Every stage writes a durable artifact, so losing the conversation never restarts the work.

---

## 2. Roles

| Role | Model | Job |
|---|---|---|
| **Explorer** | `haiku`–`sonnet`, low | Read-only fan-out. Returns conclusions, never file dumps. |
| **Architect** | `opus`, high | Constitution, PRD, spec, plan. Decides what is expensive to change. |
| **Decomposer** | `sonnet`, medium | Approved plan → atomic tasks. Mechanical by design. |
| **Implementer** | `sonnet`, high | One task end to end: code, test, `verify`. Often parallel. |
| **Auditor** | `opus`, xhigh | Adversarial. Tries to prove the feature is not done. |

The Auditor outranks the Implementer in reasoning budget on purpose: a reviewer who thinks less
carefully than the author finds nothing and certifies everything.

---

## 3. Routing a step to an agent

`workflow.json` is a declarative binding to the `Agent` tool. A step translates directly:

```jsonc
{ "id": "feat-003-refund.plan",
  "agent": { "model": "opus", "effort": "high",
             "subagent_type": "Plan", "isolation": "worktree" },
  "background": false }
```

→ `Agent(subagent_type: "Plan", model: "opus", run_in_background: false, …)`, prompted with the
step's `instructions` file.

- `isolation: "worktree"` — the step gets its own git worktree. Use it for parallel implementers.
  Never for two tasks that touch the same files.
- `background: true` — dispatch and continue. Only for steps holding no gate.

---

## 4. Parallelism

Parallelise **reads freely, writes carefully.**

- Independent reads → one Explorer covering all of them.
- Tasks marked `parallel: yes` in `tasks.md` → concurrent Implementers in separate worktrees.
- Merge a worktree only when its own `verify` is green.

Two agents writing the same file is not parallelism, it is a merge conflict with extra steps.

---

## 5. Verification is the product

A task is not done until `verify` is green.

- `harness.verify` in `workflow.json` is the single command agents and CI both call — typically
  `lint + typecheck + test + build`.
- Gherkin scenarios in `spec.md` become the executable tests.
- CI runs the same command on every PR, so the agent and the pipeline read the same signal.

**With `harness.enabled: false`** there is no automated signal. The `verify_green` gates become
advisory and every "done" rests on the agent's own claim. That is a legitimate mode for
specification-only work — but say it out loud rather than letting green-looking output imply a
check that never ran.

---

## 6. Background steps

Use `background: true` when a step is slow, read-heavy, and nothing waits on it — the intake step
is the archetype. Rules:

1. A background step must not hold a gate. `wf validate` enforces this.
2. It must not write files a foreground step is writing.
3. Its result must be persisted to an artifact, not just reported in chat.
4. Never predict its result. If it has not reported, say it is still running.

---

## 7. Memory

`MEMORY.md` indexes durable facts, one line each. Record only what is non-obvious and not
derivable from code or git: product constraints, decisions with rationale, external references,
working-style feedback. Convert relative dates to absolute.

Do not record what the repo already says. `workflow.json` is the state; `MEMORY.md` is the
knowledge that has nowhere else to live.

---

## 8. Definition of done, per feature

- [ ] `spec.md`, `plan.md`, `tasks.md` present, each with an **Origin** section.
- [ ] Every task `[x]`; `verify` green locally and in CI.
- [ ] Every Gherkin scenario covered by a test that would fail on regression.
- [ ] Compliance checklist satisfied with evidence.
- [ ] `audit.md` verdict is `pass`.
- [ ] `catalogs/epics.md` updated to `delivered`; Open Questions resolved or carried forward.
- [ ] `wf status` shows the feature step rolled up to done.
