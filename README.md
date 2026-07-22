# workflow

A Spec-Driven Development template where the pipeline is **data, not prose**.

Most agentic templates describe their process in Markdown and hope the model follows it. This one
puts the process in `workflow.json` — a checklist of steps with state, per-step model and effort,
nested substeps, background flags and gates — and renders it before every single reply.

```
PIPELINE · billing (api)
██████████░░░░░░  9/14  ·  harness ✔  ·  lang en
──────────────────────────────────────────────────────────
✔ bootstrap  Bootstrap context  · haiku·low
✔ definitions  Intake raw material  · haiku·low Explore bg
✔ constitution  Author the guardrails  ‹approval›
✔ prd  Author the living PRD  · opus·high  ‹approval›
✔ catalogs  Derive the catalogs  · sonnet·medium
▸ delivery  Feature delivery
└ ▸ feat-001-refund  Refund a completed checkout (US-2.3)
  ├ ▸ feat-001-refund.plan  Plan — how, with contracts  · opus·high Plan  ‹approval›
  ├ ○ feat-001-refund.tasks  Tasks — atomic and independently verifiable  · sonnet·medium  ‹approval›
  ├ ○ feat-001-refund.implement  Implement — code + tests  · sonnet·high  ‹verify›
  └ ○ feat-001-refund.audit  Audit — adversarial coherence check  · opus·xhigh  ‹audit›
○ release  Handoff, roadmap and memory  · sonnet·medium
──────────────────────────────────────────────────────────
▸ NOW  feat-001-refund.plan  opus·high  instructions/feature-plan.md
✔ done  ▸ active  ○ pending  ✖ blocked  ⊘ skipped
```

Colored in a real terminal (green done, yellow active, dim pending); clean and
color-free when piped, so the per-turn hook injection stays lean.

## Why data instead of prose

A prose pipeline has no state. The agent re-derives where it is from the conversation, and when
the context is summarized, that derivation quietly gets worse. Steps get skipped, gates get
inferred from a "looks good", and nobody notices until the audit — if there is one.

Putting it in a file fixes three things at once:

- **State survives context loss.** `workflow.json` is the memory; the conversation is not.
- **The human can see drift.** The banner makes "you are working on the wrong step" visible in
  one glance instead of three paragraphs.
- **Cost becomes a decision, not an accident.** Each step declares its model and effort, so
  mechanical work runs on `haiku` and irreversible work runs on `opus`, by design.

## Quickstart

From a copy of this template, scaffold a fresh project (clones the template cleaned — no git
history, no machine-local settings, no CI wiring):

```bash
./bin/wf create ~/Workspace/Projects/my-project   # or just `./bin/wf create` and it prompts
cd ~/Workspace/Projects/my-project
```

Then open a Claude Code session in the directory and run the setup wizard:

```
/wf-setup      # asks type · language · harness · GitHub via question cards, configures everything
/wf-next       # from here on, this is the one command you use
```

`/wf-setup` drives the questions through Claude's question-card UI and applies them in one
`./bin/wf init` call (project identity, harness, GitHub, CI, and it removes the example feature).
The `UserPromptSubmit` hook injects the pipeline on every turn, so the agent always knows where
it is. Everything the wizard does is also available as plain CLI:

```bash
./bin/wf init --name my-project --type api --lang en --harness on --github off
./bin/wf status        # see the pipeline
./bin/wf next          # what to do next
```

**Step-by-step walkthrough — install to shipped feature:** [`docs/usage.md`](docs/usage.md).

## Rule zero

**Every reply renders the pipeline first.** Enforced twice — deterministically by the hook in
`.claude/settings.json`, behaviorally by `CLAUDE.md § 0`. The hook is what makes it survive a
model that would otherwise forget by turn forty.

## Structure

```
workflow.json            the pipeline: steps, state, models, gates, substeps
workflow.schema.json     the contract — validates the above, including recursive substeps
bin/wf                   the driver: status · next · start · done · feature add · validate
instructions/            one .md per step — <name>-<otherinfo>.md, referenced from workflow.json
CLAUDE.md                operating manual · rule zero lives here
AGENTS.md                roles, model routing, parallelism, the closed loop
engines/                 SDD engines: manifests + scaffolds (default bundled, speckit draft)
harness/                 the pluggable verification layer — profiles + CI, per project type
docs/                    workflow.json reference · engines · workspace · usage

# materialized at setup by the chosen engine's scaffold (a fresh project has none of these):
constitution/            inviolable guardrails — outrank everything, including chat
prd/                     the living PRD, versioned
catalogs/                requirements · rules · epics · open questions
definitions/             raw input material, archived once digested
features/NNN-slug/       spec · plan · data-model · contracts · tasks · audit
```

## The step

```jsonc
{
  "id": "feat-001-refund.implement",
  "title": "Implement — code + tests",
  "status": "in_progress",
  "instructions": "instructions/feature-implement.md",
  "agent": { "model": "sonnet", "effort": "high", "isolation": "worktree" },
  "background": false,
  "gate": "verify_green",
  "requires": ["feat-001-refund.tasks"],
  "produces": ["src/**/refund*.ts"],
  "steps": [ /* substeps, nested without limit */ ]
}
```

- **`instructions`** is mandatory and is the step's actual prompt. Steps are pointers; the
  thinking lives in `instructions/`.
- **`agent`** — `haiku | sonnet | opus | fable` × `low | medium | high | xhigh | max`. Inherited
  from the nearest ancestor that declares it, else `defaults`.
- **`steps`** — recursive. Features nest under `delivery`, lifecycle stages under features, tasks
  under `implement`. A parent is done only when its children are.
- **`background`** — dispatch as a background sub-agent and continue. Validation rejects a
  background step that holds a gate, because a gate that does not block is not a gate.
- **Any SDD feature is a step.** `wf feature add` appends one with its full lifecycle.

Full field reference: [`docs/workflow-json.md`](docs/workflow-json.md).

## Plugging in the harness

SDD gives you traceable intent. The harness gives you a signal you can trust. They are
independent — flip `harness.enabled` in `workflow.json`:

```jsonc
"harness": { "enabled": true, "profile": "harness/profiles/api.md", "verify": "make verify" }
```

On, `verify_green` gates are enforced against a real command. Off, they are advisory and the
pipeline must **say so** rather than implying a check that never ran. See
[`harness/README.md`](harness/README.md).

## Plugging in GitHub

Optional, and independent of everything above:

```bash
./bin/wf github on --repo owner/name --assignee octocat --project 3
```

The pipeline then materializes on GitHub: one issue per feature (assigned), a Projects v2 card
that follows step status, comments at each gate, semantic commits per task, one PR per feature,
and README/CHANGELOG updates at release.

`bin/wf` stays **offline** — it generates the issue/PR/commit/changelog text and records the
resulting numbers; the agent runs [`gh`](https://cli.github.com) with them **after you confirm**,
because issues, PRs, comments and pushes are outward-facing. It never pushes to the default branch
and never merges before the audit passes. See
[`instructions/github-sync.md`](instructions/github-sync.md).

## Multi-project workspaces

One product, several repos (`users-api`, `users-web`, `users-mobile`)? Keep **one pipeline**:

```bash
./bin/wf create ~/Projects/users-api --satellites "users-web:web,users-mobile:mobile"
# or a dedicated controller that holds only the pipeline:
./bin/wf create ~/Projects/users-workflow --controller --connect "../users-api:api"
```

The **hub** owns the pipeline and all SDD docs; **satellites** keep their code plus a minimal
`workflow.json` pointing home. Open a session in any satellite and every `wf` command — hook
included — transparently operates on the hub. Features route to a satellite's repo with
`wf feature add … --project users-web`. See [`docs/workspace.md`](docs/workspace.md).

## Relationship to `project-template-ai`

The SDD layers — constitution, catalogs, PRD, feature templates, harness profiles — come from
[`project-template-ai`](https://github.com/rafaelsantos/project-template-ai) and stay compatible
with it. What `workflow` adds is the explicit, stateful pipeline on top: `workflow.json`,
`bin/wf`, `instructions/`, and the hook that makes rule zero real.
