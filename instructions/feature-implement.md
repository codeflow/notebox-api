---
step: feat-*.implement
model: sonnet
effort: high
reads: [features/NNN-slug/tasks.md, features/NNN-slug/contracts/**, constitution/03-code-standards.md]
writes: [source code, tests, features/NNN-slug/tasks.md]
gate: verify_green
---

# Implement — code and tests

**Goal.** Make the Gherkin scenarios pass, one task at a time, with the `verify` signal green
before any task is checked off.

## The loop, per task

1. `./bin/wf start <task-id>`.
2. Read only what the task names — its files, its contract, its scenario. Do not re-read the
   whole plan; it was already decomposed for you.
3. Write the test first when the scenario is testable. It is the acceptance criterion in
   executable form; writing it first is how you find out the spec was ambiguous.
4. Implement. Match the surrounding code's idiom — but naming language and comment policy come
   from `constitution/03-code-standards.md` (materialized from `project.code_naming` and
   `project.comments`), which wins over whatever the neighbouring code happens to do.
5. Run `verify` (`harness.verify` in `workflow.json`). If the harness is disabled, run whatever
   test command exists and say plainly that no gate is enforcing this.
6. Green → `./bin/wf done <task-id>` and tick the box in `tasks.md`. Red → fix. Never check off
   a task on a red signal, and never weaken a test to make it pass.

## Parallelism

Tasks marked `parallel: yes` may run as concurrent sub-agents with `isolation: worktree`.
Sequential tasks stay in the main thread. Merge each worktree only after its own `verify` is
green — a red worktree merged "to fix later" defeats the entire loop.

## When the plan is wrong

You will sometimes discover, mid-task, that the plan cannot work. Stop. Do not improvise a
different design and keep going — that silently detaches the code from the approved artifacts and
the audit will find it much later, at much greater cost.

Mark the task `blocked` with what you found, report it, and let the human decide whether to
amend the plan.

## Routed features — the work happens in the satellite

If the feature step carries a `project` field, the code lives in THAT workspace project, not in
the hub. **If that satellite ships a design reference (e.g. `design/handoff/`), the UI must match
it** — read its `README.md` and open its flow HTML in a browser when unsure; never infer pixels
from the PNG prints. Before the first task:

1. Read the satellite's own `workflow.json` (path in the hub's `workspace.projects[]`). Its
   `harness.verify` is the green/red signal for THIS feature — the hub's verify is for the
   hub's code and proves nothing here.
2. **No `harness.verify` there?** The satellite was never set up for implementation. Stop and
   run its setup — from a session in the satellite, `/wf-setup` (which detects the satellite
   role), or directly: `cd <satellite> && ./bin/wf workspace init --stack "…" --verify "…"
   --repo owner/name`. Do not implement against a satellite with no verify signal and call
   anything "done".
3. Run the tasks with the satellite as working directory; commits, branch and PR target the
   satellite's repo (`wf github` already routes them).

## GitHub (if enabled)

Before the first task, branch off the default branch: `git switch -c feat/<slug>` and record it
with `./bin/wf github link <feature-id> --branch feat/<slug>`. Per task, generate the commit
message with `./bin/wf github commit <task-id>` (add your own body), and tick the matching box on
the issue. When `verify` is green, open the PR with `./bin/wf github pr <feature-id>`. Every push
and PR is confirmed with the human first, and never targets the default branch directly. Details:
`instructions/github-sync.md`.

## Scope

Fix only what the task covers. A bug you notice elsewhere goes into the backlog or an Open
Question — not into this diff. Unrelated fixes riding along in a feature diff are how reviews
stop being reviews.

## Done when

All tasks `[x]`, `verify` green, and every scenario in `spec.md` covered by a test that actually
exercises it.
