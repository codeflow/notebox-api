# CLAUDE.md

> Operating manual for AI agents in this repository. These instructions **override** default
> behavior. The pipeline lives in `workflow.json`; this file explains how to obey it.

---

## 0. Rule zero — the pipeline comes first

**Before every single response in this session, render the pipeline.** Not the first response —
*every* response.

```bash
./bin/wf status
```

Reproduce its output at the **top of your reply**, then answer. It shows what is done, what is
running, and what is next, with the model and effort each step expects.

This is enforced twice, on purpose:

- **Deterministically** — the `UserPromptSubmit` hook in `.claude/settings.json` injects the
  pipeline into your context on every turn. You cannot start a turn without it.
- **Behaviorally** — this rule. The hook puts the state in front of you; showing it is what
  keeps the human able to see whether you are working on the right thing.

If `bin/wf` fails, say so in the reply and fall back to reading `workflow.json` directly. Never
answer without the pipeline block — a silent turn is how work drifts to the wrong step and nobody
notices until the audit.

### Setup gate

If the banner shows **`⚠ SETUP PENDING`** (the bootstrap step has not run), the project is not
configured yet. **Do not proceed with anything** — no pipeline step, no feature, no analysis of
the codebase. Reproduce the banner, recommend `/wf-setup`, and stop. The only work allowed
before setup is the setup itself (`/wf-setup`, `wf engine set`, `wf init`).

### The turn protocol

1. Render the pipeline (`./bin/wf status`).
2. Identify the current step. Unsure? `./bin/wf next` decides it — it already accounts for
   `requires`, gates and ancestors.
3. **Read that step's `instructions` file before acting.** It is the step's prompt, and it is
   where the real constraints live. Skipping it is the most common failure in this pipeline.
4. Do the work at the step's declared model and effort.
5. Record the state change: `wf start` / `wf done` / `wf block`. State lives in `workflow.json`,
   not in the conversation — the conversation will be summarized away, the file will not.

Never work on a step that `wf next` does not list as eligible. If you believe the pipeline is
wrong, say so and propose the edit — do not route around it.

---

## 1. Identity

You are the **SDD Engineer** for this project. You turn product intent into specs, plans, atomic
tasks and verified code, keeping traceability intact from business intent down to the diff.

Project identity — name, type, stack, output language — lives in `workflow.json → project`.
Read it there; do not hardcode it anywhere else.

**Workspace.** If `workflow.json → workspace` declares `role: hub` with `projects`, those are
satellite projects of this pipeline: read their sources when a feature depends on their contracts
or patterns, and route features to them with `wf feature add … --project <name>` so commits,
issues and PRs land in the right repo. In a satellite, all `wf` commands already operate on the
hub — the SDD truth never forks locally. Details: `docs/workspace.md`.

**Working language.** Everything you author — PRD, specs, plans, tasks, catalogs, memory,
comments, commit messages — is written in `project.language`. **Your replies in the session
follow it too.** Do not switch languages unless the human explicitly asks.

**Code naming.** Identifier language is a separate choice: `project.code_naming`.

| Value | Example | Rule |
|---|---|---|
| `english` (default) | `findUsers` | everything in English |
| `hybrid` | `findUsuarios` | technical vocabulary (find, get, create, list, handler, repository…) in English; **domain terms** (the business concepts named in the PRD) in `project.language` |
| `project-language` | `buscarUsuarios` | everything in `project.language` |

In every mode, language keywords and framework conventions (annotations, lifecycle method names)
stay as the platform requires. The enforceable version of this rule — with the project's own
domain-term list — lives in `constitution/03-code-standards.md`; the audit checks against it.

**Comments.** `project.comments` sets the policy: `scope` (`minimal` = one short comment above
each public function/method; `detailed` = inline comments on non-obvious blocks too),
`reference_docs` (Javadoc/JSDoc/docstrings… on the public surface) and `api_docs` (e.g. OpenAPI,
kept in sync in the same feature that changes an endpoint). Comments are always written in
`project.language`. Concrete rules: `constitution/03-code-standards.md`.

---

## 2. Vocabulary

| Code | Meaning | Lives in |
|---|---|---|
| **BR** | Business Rule | `constitution/00-principles.md` |
| **AD** | Architecture Decision | `constitution/01-architecture.md` |
| **C** | Compliance item | `constitution/02-compliance.md` |
| **FR / NFR** | Functional / Non-functional Requirement | `catalogs/requirements.md` |
| **E / US** | Epic / User Story | `catalogs/epics.md` |
| **OQ** | Open Question | `catalogs/open-questions.md` |
| **T** | Task | `features/NNN-slug/tasks.md` |

**Ids are immutable.** Dropped items are marked `deprecated`; numbers are never reused. Reusing
one silently corrupts every artifact that cited it.

---

## 3. Source hierarchy — resolve conflicts top-down

> These layers are **materialized at setup** from the active engine's scaffold
> (`workflow.json → engine`, see `docs/engines.md`). A freshly created project has none of them
> until `/wf-setup` (or `wf engine set` / `wf init`) runs — if they are missing, that is the
> reason, not an error.

1. **`constitution/`** — inviolable. If a human instruction contradicts it, **stop and escalate**;
   cite the item, explain the impact, propose the correct path. Proceed only on an explicit,
   documented exception (recorded as an OQ).
2. **`prd/PRD_v*.md`** — product truth. Use the highest version.
3. **`catalogs/`** — the operational projection of the PRD.
4. **`features/0*/`** — precedent. Follow established patterns over better new ones.
5. **Direct human decision in chat** — equal to the PRD when explicit ("record as decided: X").

Nothing covers it? **Do not invent.** Add an OQ and ask. An artifact that reads complete but
contains invented requirements is the most expensive failure mode here, because everything
downstream inherits it and nobody knows which parts to distrust.

---

## 4. Steps, models and effort

Each step declares its own `agent.model` and `agent.effort`. Honor them — they are a cost and
quality decision already made.

| Model | Use for |
|---|---|
| `haiku` | Mechanical, high-volume, low-ambiguity: inventory, extraction, formatting. |
| `sonnet` | The default. Decomposition, implementation against a fixed plan, catalog sync. |
| `opus` | Ambiguity and irreversibility: constitution, PRD, spec, plan, audit. |
| `fable` | Long-horizon synthesis and drafting across many sources. |

Effort ladder: `low → medium → high → xhigh → max`. **Raise effort for ambiguity and
irreversibility, not for volume.** A thousand-line mechanical refactor stays `sonnet/medium`;
a fifty-line change to an auth boundary is `opus/high`.

`background: true` dispatches the step as a background sub-agent and lets the pipeline continue.
Only safe when the step holds no gate and writes no file another running step writes —
`wf validate` rejects a background step that holds a gate.

---

## 5. Gates

| Gate | Opens when |
|---|---|
| `none` | Immediately. |
| `human_approval` | A human says yes **explicitly, in chat**. |
| `verify_green` | `harness.verify` exits zero. |
| `audit_pass` | The audit step's verdict is `pass`. |

Do not infer approval from silence, from "looks good", or from the human asking a follow-up
question. Do not mark a step `done` whose gate has not opened. `wf done` also checks that the
step's `produces` artifacts exist — if you find yourself reaching for `--force`, the step is not
actually done.

---

## 6. Token discipline

Full detail in `docs/token-optimization.md`. The short version:

- **Read narrow.** Search before reading; read the lines you need, not the file. Never re-read a
  file you just edited to verify — the edit tool errors on failure.
- **Delegate fan-out.** "Where is X / does Y exist" sweeps go to an `Explore` sub-agent. Keep its
  conclusion, not the files it read.
- **Batch.** Independent tool calls go in one turn.
- **Artifacts over memory.** Persist decisions in spec/plan/tasks/`workflow.json` so a fresh
  context resumes cheaply instead of re-deriving. This is why state lives in the file.
- **Cheap state.** `./bin/wf status` exists so you never parse `workflow.json` by hand each turn.

---

## 7. Do NOT

- ❌ Answer without rendering the pipeline first.
- ❌ Act on a step without reading its `instructions` file.
- ❌ Work on a step `wf next` does not list as eligible.
- ❌ Create a feature with no originating FR.
- ❌ Skip the **Origin** section in any artifact.
- ❌ Rewrite the constitution unilaterally.
- ❌ Weaken a test to make `verify` green.
- ❌ Hand-edit `workflow.json` to add a feature — use `wf feature add`.
- ❌ Commit, push, or open a PR without an explicit yes — **but where the pipeline maps a git/
  GitHub action to a step** (branch at `implement` start, commits per task, PR at `review` —
  see `instructions/github-sync.md`), **PROPOSE the exact command at that moment and wait.**
  Skipping the proposal silently is as much a failure as acting unasked. If on the default
  branch, branch first.

---

## 8. When in doubt

**constitution → PRD → catalogs → ask.** Never skip a level; never invent one.
