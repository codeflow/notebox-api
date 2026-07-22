# `workflow.json` — field reference

The pipeline as data. Validated by `workflow.schema.json`; checked by `./bin/wf validate`.

---

## Top level

| Field | Required | Meaning |
|---|---|---|
| `version` | ✓ | Schema version, semver. |
| `project` | ✓ | Identity: `name`, `type`, `stack`, `language`. The only place these live. |
| `workspace` | | Multi-project topology: `role: hub` + `projects[]`, or `role: satellite` + `hub` pointer (a satellite file needs no `steps`). See [`workspace.md`](workspace.md). |
| `engine` | | The SDD engine (feature lifecycle + artifact layout) as data: `{"name": "default"}` or an `engines/<name>/engine.json` manifest. See [`engines.md`](engines.md). |
| `harness` | | The verification layer. See [`harness/README.md`](../harness/README.md). |
| `defaults` | | `agent`, `background`, `gate` inherited by steps that omit them. |
| `policy` | | Pipeline-wide enforcement switches. |
| `steps` | ✓ | The pipeline. |

### `project.language`

The output language for **everything agents author** — PRD, specs, plans, catalogs, comments,
commit messages — **and for the agent's replies in the session**. Set it once; agents must not
switch on their own.

### `project.code_naming`

Identifier language in source code — a separate choice from `language`:

| Value | Example | Rule |
|---|---|---|
| `english` (default) | `findUsers` | everything in English |
| `hybrid` | `findUsuarios` | technical vocabulary in English; domain terms (the PRD's nouns) in `project.language` |
| `project-language` | `buscarUsuarios` | everything in `project.language` |

The `constitution.standards` step materializes this into `constitution/03-code-standards.md`
with the project's own domain-term list, which is what the audit enforces.

### `project.comments`

Code-comment policy — always written in `project.language`:

| Field | Values | Meaning |
|---|---|---|
| `scope` | `minimal` (default) · `detailed` | minimal = one short comment above each public function/method; detailed = inline comments on non-obvious blocks too |
| `reference_docs` | `javadoc`, `jsdoc`, `docstrings`, `godoc`, `rustdoc`, `xmldoc`… or empty | structured docs on the public surface, per the stack |
| `api_docs` | `openapi`, `asyncapi`… or empty | API contract doc, updated in the same feature that changes an endpoint |

Set via `/wf-setup` or `wf init --comments detailed --doc-format javadoc --api-docs openapi`.
Materialized as enforceable rules in `constitution/03-code-standards.md`.

### `github`

Optional. When `enabled`, the pipeline is materialized on GitHub — one issue per feature
(assigned to `assignee`), a Projects v2 card tracking status, comments at each gate, semantic
commits per task, one PR per feature, and README/CHANGELOG updates at release.

```jsonc
"github": {
  "enabled": true,
  "repo": "owner/name",            // full https URL is normalized to this
  "assignee": "octocat",           // issues opened for features are assigned here
  "default_branch": "main",        // the protected trunk — receives only promotion PRs
  "integration_branch": "develop", // feature PRs land here; /wf-promote raises develop → main
  "reviewers": ["reviewer1"],      // requested on feature PRs
  "project": { "enabled": true, "number": 3 },   // Projects v2 board
  "sync": { "issue_per": "feature", "changelog_on": "audit_pass" }
}
```

Plug it in with `./bin/wf github on --repo owner/name --assignee user [--project N]`.

**`bin/wf` never calls the network.** Its `github` subcommands generate payloads and record issue/
PR numbers; the agent runs `gh` with them, after the human confirms — creating issues, PRs,
comments and pushes are outward-facing actions. Requires the [`gh`](https://cli.github.com) CLI,
authenticated. See [`instructions/github-sync.md`](../instructions/github-sync.md).

### `policy`

| Key | Default | Effect |
|---|---|---|
| `render_pipeline_before_response` | `true` | Rule zero. Backed by the `UserPromptSubmit` hook. |
| `strict_gates` | `true` | A step cannot start while `requires` or a predecessor's gate is unsatisfied. |
| `single_in_progress` | `true` | One foreground step at a time. Background steps exempt. |
| `escalate_on_constitution_violation` | `true` | Stop and escalate rather than silently adjust. |
| `auto_append_features` | `true` | New features land under the `container_for: "features"` step. |
| `ask_agent_review` | `true` | Adding a feature offers a card to review its per-step agents. `false` keeps the defaults silently (`wf init --agent-review off`). |

---

## The step

| Field | Required | Meaning |
|---|---|---|
| `id` | ✓ | Stable, unique, **immutable**. Never reused, even after deletion. |
| `title` | ✓ | Human-readable, shown in the banner. |
| `instructions` | ✓ | `instructions/<name>-<otherinfo>.md`. The step's prompt. |
| `kind` | | `stage` (default) · `container` · `feature` · `task` · `gate`. |
| `status` | | `pending` · `in_progress` · `done` · `blocked` · `skipped`. |
| `agent` | | Model, effort, optional sub-agent and isolation. |
| `background` | | Dispatch as a background sub-agent and continue. |
| `requires` | | Step ids that must be done first — this is what makes it a DAG, not a line. |
| `produces` | | Artifact paths/globs. `wf done` refuses to close a step that produced none. |
| `gate` | | What must be true before the **next** step may start. |
| `acceptance` | | Checklist that must hold before `done`. |
| `command` | | Optional slash command that drives the step. |
| `container_for` | | `"features"` — marks the insertion point for `wf feature add`. |
| `steps` | | **Recursive substeps.** Unbounded nesting. |

### `agent`

```jsonc
"agent": { "model": "opus", "effort": "high", "subagent_type": "Plan", "isolation": "worktree" }
```

| Model | Use for |
|---|---|
| `haiku` | Mechanical, high-volume, low-ambiguity work. |
| `sonnet` | The default: decomposition, implementation against a fixed plan. |
| `opus` | Ambiguity and irreversibility: constitution, PRD, spec, plan, audit. |
| `fable` | Long-horizon synthesis across many sources. |

Effort: `low → medium → high → xhigh → max`. **Raise it for ambiguity and irreversibility, not
for volume.** A thousand-line mechanical refactor is still `sonnet/medium`; fifty lines touching
an auth boundary is `opus/high`.

`isolation: "worktree"` gives the step its own git worktree — for parallel implementers only, and
never for two tasks touching the same files.

**Resolution is nearest-ancestor-wins:** the step's own `agent`, else the closest ancestor that
declares one, else `defaults.agent`. Set the model once on a feature and every substep inherits
it, unless it says otherwise. Same rule for `background`.

### `gate`

| Value | Opens when |
|---|---|
| `none` | Immediately. |
| `human_approval` | A human says yes **explicitly, in chat**. Not silence, not "looks good". |
| `verify_green` | `harness.verify` exits zero. Advisory if the harness is disabled. |
| `audit_pass` | The audit step's verdict is `pass`. |

A gate constrains the **next** step, not its own. That is why a `background: true` step may not
hold one — it would be a gate that never blocks. `wf validate` rejects the combination.

### Recursive substeps

Nesting has no limit. The shipped pipeline uses four levels:

```
delivery                                container
└── feat-001-refund                      feature
    └── implement                        stage
        └── t3                           task
```

A parent's status **rolls up** from its children: `done` when all children are done or skipped,
`in_progress` when any child has started. Do not set a parent's status by hand — `wf status`
computes it, and a hand-set parent will disagree with its own children.

**Containers are the exception.** A step with `kind: "container"` never rolls up to `done`, no
matter how many children finish — it holds a backlog, and finishing the items created so far says
nothing about whether the backlog is empty. Instead, once its children are all done it becomes
**actionable again**, so `wf next` hands it back: add the next feature, or close it explicitly
with `wf done`. Without this, `delivery` would declare itself finished after the very first
feature and the pipeline would run on to `release` with the epics untouched.

Blockers are inherited downward: a child cannot start while any ancestor is itself blocked. This
is why `feat-001.spec` is not eligible while `catalogs` is still pending.

---

## Features are steps

The core idea: **an SDD feature is not a special entity, it is a step with substeps.**

```bash
./bin/wf feature add refund "Refund a completed checkout" --us US-2.3
```

appends, under the `container_for: "features"` step:

```jsonc
{ "id": "feat-001-refund", "kind": "feature", "status": "pending",
  "instructions": "instructions/feature-lifecycle.md",
  "steps": [
    { "id": "feat-001-refund.spec",      "agent": {"model":"opus",  "effort":"high"},   "gate": "human_approval" },
    { "id": "feat-001-refund.plan",      "agent": {"model":"opus",  "effort":"high"},   "gate": "human_approval" },
    { "id": "feat-001-refund.tasks",     "agent": {"model":"sonnet","effort":"medium"}, "gate": "human_approval" },
    { "id": "feat-001-refund.implement", "agent": {"model":"sonnet","effort":"high"},   "gate": "verify_green" },
    { "id": "feat-001-refund.audit",     "agent": {"model":"opus",  "effort":"xhigh"},  "gate": "audit_pass" }
  ] }
```

and creates `features/001-refund/`. Use the command rather than hand-editing: it keeps ids,
sequence numbers and directories in agreement, and those three drifting apart is what makes a
pipeline stop being trustworthy.

The `tasks` step then mirrors `tasks.md` into substeps of `implement`, giving the fourth level.

---

## `bin/wf`

| Command | Does |
|---|---|
| `wf create [<dest>] [--no-git]` | Scaffold a new project: the engine-agnostic **core only** — no SDD dirs (constitution/prd/catalogs/definitions/features, plus any dir claimed by an engine manifest, e.g. `specs/`, `.specify/`); the engine chosen at setup materializes them. Also cleaned: `.git`/`.github`/`settings.local.json`/`*.tmp`. Prompts for the path if omitted. |
| `wf create <dest> --satellites/--connect/--controller` | Scaffold a multi-project **workspace** — see [`workspace.md`](workspace.md). |
| `wf workspace [status]` | Role, hub, satellites and their link health. |
| `wf workspace add <path> …` | Create-or-connect one satellite from the hub. |
| `wf init …` | Apply the whole setup in one pass (project + harness + github + remove example). The engine behind `/wf-setup`. |
| `wf status [--full] [--plain\|--color]` | Render the pipeline. `--full` keeps completed substeps visible; color auto-detects the terminal. |
| `wf next` | Every eligible step, tab-separated, with resolved model and instructions. |
| `wf show <id>` | One step in detail, including why it is blocked. |
| `wf start <id>` | → `in_progress`, stamps `started_at`. Refuses if blocked. |
| `wf done <id>` | → `done`, stamps `completed_at`. Refuses if `produces` matches nothing. |
| `wf block <id> "why"` | → `blocked` with the reason recorded. |
| `wf skip <id> "why"` | → `skipped` with the reason recorded. |
| `wf reopen <id> [--cascade]` | Send a done step back to `pending`. `--cascade` also reopens every step derived from it (via `requires`); without it, warns which are now stale. Deliberately `skipped` steps are preserved. |
| `wf feature add …` | Append a feature with its lifecycle. |
| `wf harness [status]` | Harness state, and which `verify_green` gates it backs. |
| `wf harness on \| off` | Plug the verification layer in or out. `off` makes those gates advisory. |
| `wf harness verify "CMD"` | Set the single green/red command. |
| `wf harness signal <name> "CMD"` | Set one CI signal's command (e.g. `test`, `build`). |
| `wf harness ci` | Generate `.github/workflows/ci.yml` — one step per signal. |
| `wf github [status]` | GitHub config and per-feature issue/PR/branch mapping. |
| `wf github on \| off` | Plug GitHub integration in or out. |
| `wf github link <feat> …` | Record which issue/PR/branch/card a feature maps to. |
| `wf github issue\|pr\|comment\|commit\|changelog …` | **Offline** payload generators — print text for `gh`, never call it. PR base defaults to the integration branch; `--base/--head` override (promotion). |
| `wf github pending` | Features merged to the integration branch and not yet promoted to the trunk — reported at the end of every implementation. |
| `wf engine [status\|list\|set <name>]` | The active SDD engine (manifest, layout, lifecycle); switch engines. |
| `wf engine scan` | Read-only: installed engine commands vs manifests — drift both ways, plus candidate engines to learn (`/wf-engine learn`). |
| `wf agents [<id-prefix>]` | Every step's resolved model/effort, own vs inherited. |
| `wf agent <id\|defaults> …` | Change a step's (or the defaults') model, effort, subagent, isolation or background. |
| `wf oq [list [--all]]` | Pending Open Questions from `catalogs/open-questions.md`. |
| `wf oq answer <id> "decision" [--by name]` | Resolve one: status flipped, decision + history recorded. The `/wf-answer` cards drive this. |
| `wf oq add "title" […]` | Open a new question with the next immutable id. |
| `wf validate` | Ids, naming pattern, missing files, models, cycles, gate/background conflicts. |

`--force` overrides the `start` and `done` guards. Reaching for it usually means the step is not
actually done — the guard is the cheapest reviewer you have.

Stdlib Python 3, no dependencies. It exists so agents never parse `workflow.json` by hand each
turn: one cheap call returns the whole state.
